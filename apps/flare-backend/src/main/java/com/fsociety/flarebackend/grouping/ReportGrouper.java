package com.fsociety.flarebackend.grouping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import com.fsociety.flarebackend.classification.Classification;
import com.fsociety.flarebackend.classification.ClassificationInput;
import com.fsociety.flarebackend.classification.FallbackClassifier;
import com.fsociety.flarebackend.classification.LocationInfo;
import com.fsociety.flarebackend.classification.MessageClassifier;
import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.issue.Issue;
import com.fsociety.flarebackend.issue.IssueRepository;
import com.fsociety.flarebackend.issue.IssueService;
import com.fsociety.flarebackend.issue.IssueStatus;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.report.ReportStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Puts one accepted Report into an Issue, in its own transaction (pipeline.md section 5). Without the LLM
 * matcher (C03) every Report gets its own Issue, except Emergencies: those join the open Emergency Issue at
 * the same Location, because speed matters more than precision there. Idempotent: a Report that is already
 * grouped, or no longer accepted, is left alone, so being queued twice is harmless.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGrouper {

	private final ReportRepository reports;
	private final IssueRepository issues;
	private final IssueService issueService;
	private final MessageClassifier classifier;
	private final FallbackClassifier fallbackClassifier;
	private final Clock clock;

	@Transactional
	public void group(UUID reportId) {
		Report report = reports.findById(reportId).orElse(null);
		if (report == null || report.getStatus() != ReportStatus.ACCEPTED || report.getIssue() != null) {
			return;
		}
		if (report.getClassificationSource() == null) {
			classify(report);
		}

		Issue issue = emergencyIssueFor(report)
				.map(open -> {
					issueService.attachReport(open, report);
					return open;
				})
				.orElseGet(() -> issueService.createFromReport(report));
		report.setGroupedAt(clock.instant());
		log.info("Report {} grouped into Issue {}", report.getId(), issue.getId());
	}

	private Optional<Issue> emergencyIssueFor(Report report) {
		if (!report.isEmergency() || report.getLocation() == null) {
			return Optional.empty();
		}
		return issues.findFirstByLocationIdAndEmergencyTrueAndStatusNotOrderByCreatedAtDesc(
				report.getLocation().getId(), IssueStatus.RESOLVED);
	}

	/**
	 * Classifies a Report stored before classification (the Emergency path). An Emergency stays CRITICAL and
	 * an Emergency whatever the classifier says; any classifier failure uses the fallback.
	 */
	private void classify(Report report) {
		ClassificationInput input = new ClassificationInput(report.getText(), report.getLanguage(),
				Optional.ofNullable(report.getLocation()).map(l -> new LocationInfo(l.getNameRo(), l.getNameEn())), false);
		Classification classification;
		try {
			classification = classifier.classify(input);
		}
		catch (RuntimeException e) {
			log.warn("Classifier failed for report {}, using the fallback ({})", report.getId(), e.getClass().getSimpleName());
			classification = fallbackClassifier.classify(input);
		}

		boolean emergency = report.isEmergency() || classification.isEmergency();
		report.setEmergency(emergency);
		report.setCategory(classification.getCategory() != null ? classification.getCategory() : Category.OTHER);
		report.setSeverity(emergency ? Severity.CRITICAL
				: classification.getSeverity() != null ? classification.getSeverity() : Severity.MEDIUM);
		report.setConfidence(BigDecimal.valueOf(classification.getConfidence()).setScale(2, RoundingMode.HALF_UP));
		if (classification.getStaffSummary() != null) {
			report.setStaffSummaryRo(classification.getStaffSummary().getRo());
			report.setStaffSummaryEn(classification.getStaffSummary().getEn());
		}
		report.setMentionedPlace(classification.getMentionedPlace());
		report.setFlightNumber(classification.getFlightNumber());
		report.setClassificationSource(classification.getSource());
	}
}
