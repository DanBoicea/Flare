package com.fsociety.flarebackend.grouping;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fsociety.flarebackend.classification.Classification;
import com.fsociety.flarebackend.classification.ClassificationInput;
import com.fsociety.flarebackend.classification.FallbackClassifier;
import com.fsociety.flarebackend.classification.LocationInfo;
import com.fsociety.flarebackend.classification.MessageClassifier;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.issue.Issue;
import com.fsociety.flarebackend.issue.IssueRepository;
import com.fsociety.flarebackend.issue.IssueService;
import com.fsociety.flarebackend.issue.IssueStatus;
import com.fsociety.flarebackend.issue.summary.IssueSummarizer;
import com.fsociety.flarebackend.issue.summary.IssueSummaryView;
import com.fsociety.flarebackend.issue.summary.ReportTextView;
import com.fsociety.flarebackend.location.Location;
import com.fsociety.flarebackend.question.QuestionRepository;
import com.fsociety.flarebackend.report.QuarantineReason;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportClassifications;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.report.ReportStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Puts one accepted Report into an Issue, in its own transaction (pipeline.md section 5). A Report released to a
 * chosen Team gets its own Issue; an Emergency joins the open Emergency at its Location; any other Report is matched
 * against nearby candidate Issues and merged into the match, or gets a new Issue. Without the LLM the matcher never
 * matches, so every Report gets its own Issue. Idempotent: a Report that is already grouped, or no longer accepted,
 * is left alone, so being queued twice is harmless.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGrouper {

	/** Recent Report texts shown to the matcher per candidate (pipeline.md 4.2). */
	static final int TEXTS_PER_CANDIDATE = 3;
	/** Newest Report texts given to the summariser (pipeline.md 4.3). */
	static final int TEXTS_FOR_SUMMARY = 20;

	private final ReportRepository reports;
	private final IssueRepository issues;
	private final IssueService issueService;
	private final MessageClassifier classifier;
	private final FallbackClassifier fallbackClassifier;
	private final QuestionRepository questions;
	private final CandidateSelector candidateSelector;
	private final IssueMatcher matcher;
	private final IssueSummarizer summarizer;
	private final Clock clock;

	/** Pipeline.md section 5, steps 1 to 6, for one accepted Report, in one transaction. */
	@Transactional
	public void group(UUID reportId) {
		Report report = groupable(reportId);
		if (report == null) {
			return;
		}
		if (report.getClassificationSource() == null) {
			classify(report);
		}

		Issue issue;
		if (report.getTeamOverride() != null) {
			// Released to a chosen Team (A09): its own Issue for that Team, no matching.
			issue = issueService.createFromReport(report);
		}
		else if (report.isEmergency()) {
			// Speed matters more than precision: join the open Emergency at this Location, without the matcher.
			issue = emergencyIssueFor(report).map(open -> merge(open, report)).orElseGet(() -> issueService.createFromReport(report));
		}
		else {
			issue = match(report).map(target -> merge(target, report)).orElseGet(() -> issueService.createFromReport(report));
		}
		report.setGroupedAt(clock.instant());
		log.info("Report {} grouped into Issue {}", report.getId(), issue.getId());
	}

	/**
	 * The last resort when {@link #group} failed twice (pipeline.md section 5): the Report becomes its own
	 * Unclassified Issue for the duty manager, so nothing is ever stuck. Runs in a fresh transaction.
	 */
	@Transactional
	public void groupAsUnclassified(UUID reportId) {
		Report report = groupable(reportId);
		if (report == null) {
			return;
		}
		ReportClassifications.apply(report, fallbackClassifier.classify(input(report, false)));
		Issue issue = issueService.createFromReport(report);
		report.setGroupedAt(clock.instant());
		log.warn("Report {} grouped as a new Unclassified Issue {} after failures", report.getId(), issue.getId());
	}

	private Report groupable(UUID reportId) {
		Report report = reports.findById(reportId).orElse(null);
		if (report == null || report.getStatus() != ReportStatus.ACCEPTED || report.getIssue() != null) {
			return null;
		}
		return report;
	}

	private Optional<Issue> emergencyIssueFor(Report report) {
		if (report.getLocation() == null) {
			return Optional.empty();
		}
		return issues.findFirstByLocationIdAndEmergencyTrueAndStatusNotOrderByCreatedAtDesc(
				report.getLocation().getId(), IssueStatus.RESOLVED);
	}

	/**
	 * Steps 3 and 4: the candidates, then the matcher. A failure, or an answer that is not one of the candidates,
	 * means no match, so a new Issue is created: over-merging would hide problems.
	 */
	private Optional<Issue> match(Report report) {
		Location location = report.getLocation();
		if (location == null || location.getArea() == null) {
			return Optional.empty();
		}
		Instant now = clock.instant();
		List<Issue> candidates = candidateSelector.select(location,
				issues.findCandidates(location.getArea().getId(), candidateSelector.resolvedSince(now),
						Limit.of(candidateSelector.maxCandidates())),
				now);
		if (candidates.isEmpty()) {
			return Optional.empty();
		}
		NewReportView view = new NewReportView(report.getText(), report.getCategory(), locationName(location));
		List<CandidateIssueView> candidateViews = candidates.stream().map(this::candidateView).toList();
		Optional<UUID> matchId;
		try {
			matchId = matcher.findMatch(view, candidateViews);
		}
		catch (RuntimeException e) {
			log.warn("Matcher failed for report {}, creating a new Issue ({})", report.getId(), e.getClass().getSimpleName());
			return Optional.empty();
		}
		if (matchId == null || matchId.isEmpty()) {
			return Optional.empty();
		}
		Optional<Issue> target = candidates.stream().filter(issue -> issue.getId().equals(matchId.get())).findFirst();
		if (target.isEmpty()) {
			log.warn("Matcher answered an Issue that is not a candidate for report {}; creating a new Issue", report.getId());
		}
		return target;
	}

	/** Steps 4 and 5 for a match: attach, reopen a resolved Issue, then refresh the summary. */
	private Issue merge(Issue issue, Report report) {
		boolean wasResolved = issue.getStatus() == IssueStatus.RESOLVED;
		issueService.attachReport(issue, report);
		if (wasResolved) {
			issueService.reopenByReport(issue, report);
		}
		refreshSummary(issue);
		return issue;
	}

	/** A failed or empty summary keeps the old one (pipeline.md 4.3). */
	private void refreshSummary(Issue issue) {
		LocalizedText current = new LocalizedText(issue.getSummaryRo(), issue.getSummaryEn());
		IssueSummaryView view = new IssueSummaryView(issue.getCategory(), locationName(issue.getLocation()), current);
		List<ReportTextView> newest = reports
				.findByIssueIdAndStatusOrderByCreatedAtDesc(issue.getId(), ReportStatus.ACCEPTED, Limit.of(TEXTS_FOR_SUMMARY))
				.stream()
				.map(report -> new ReportTextView(report.getText(), report.getLanguage()))
				.toList();
		LocalizedText summary;
		try {
			summary = summarizer.summarize(view, newest);
		}
		catch (RuntimeException e) {
			log.warn("Summariser failed for Issue {}, keeping the summary ({})", issue.getId(), e.getClass().getSimpleName());
			return;
		}
		if (summary == null || isBlank(summary.getRo()) || isBlank(summary.getEn()) || summary.equals(current)) {
			return;
		}
		issue.setSummaryRo(summary.getRo());
		issue.setSummaryEn(summary.getEn());
		issue.setSummarySource(ClassificationSource.LLM);
	}

	private CandidateIssueView candidateView(Issue issue) {
		List<String> texts = reports
				.findByIssueIdAndStatusOrderByCreatedAtDesc(issue.getId(), ReportStatus.ACCEPTED, Limit.of(TEXTS_PER_CANDIDATE))
				.stream()
				.map(Report::getText)
				.toList();
		return new CandidateIssueView(issue.getId(), issue.getCategory(), locationName(issue.getLocation()),
				new LocalizedText(issue.getSummaryRo(), issue.getSummaryEn()), issue.getStatus(), texts);
	}

	/**
	 * Classifies a Report stored before classification: the Emergency path, or "Send to staff anyway", where the
	 * classifier is told the Passenger insisted so it must return REPORT. An Emergency stays CRITICAL and an
	 * Emergency whatever the classifier says; any classifier failure uses the fallback.
	 */
	private void classify(Report report) {
		// "Send anyway" (released from Needs review) or a Question sent to staff: the Passenger insisted.
		boolean passengerInsisted = report.getQuarantineReason() == QuarantineReason.SEND_ANYWAY
				|| questions.existsBySentToStaffReportId(report.getId());
		ClassificationInput input = input(report, passengerInsisted);
		Classification classification;
		try {
			classification = classifier.classify(input);
		}
		catch (RuntimeException e) {
			log.warn("Classifier failed for report {}, using the fallback ({})", report.getId(), e.getClass().getSimpleName());
			classification = fallbackClassifier.classify(input);
		}
		ReportClassifications.apply(report, classification);
	}

	private static ClassificationInput input(Report report, boolean passengerInsisted) {
		return new ClassificationInput(report.getText(), report.getLanguage(),
				Optional.ofNullable(report.getLocation()).map(l -> new LocationInfo(l.getNameRo(), l.getNameEn())),
				passengerInsisted);
	}

	private static LocalizedText locationName(Location location) {
		return location == null ? null : new LocalizedText(location.getNameRo(), location.getNameEn());
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
