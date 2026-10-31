package com.fsociety.flarebackend.issue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import com.fsociety.flarebackend.audit.AuditAction;
import com.fsociety.flarebackend.audit.AuditService;
import com.fsociety.flarebackend.classification.CategoryTeamRouting;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.common.TextLimits;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.report.ReportStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates Issues from Reports and adds Reports to Issues (pipeline.md section 5, A08 item 4). Runs inside the
 * grouping worker's transaction for one Report; writes the matching audit events (never Passenger text).
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class IssueService {

	private final IssueRepository issues;
	private final ReportRepository reports;
	private final AuditService auditService;
	private final CategoryTeamRouting routing;

	/** A new Issue made from one classified, accepted Report. The Report is linked to it. */
	public Issue createFromReport(Report report) {
		Issue issue = new Issue();
		issue.setLocation(report.getLocation());
		issue.setCategory(report.getCategory());
		issue.setSeverity(report.getSeverity());
		// A Team chosen by the duty manager on release beats routing by category (A09).
		issue.setTeam(report.getTeamOverride() != null ? report.getTeamOverride()
				: routing.teamFor(report.getCategory(), report.getClassificationSource(), confidence(report)).orElse(null));
		issue.setStatus(IssueStatus.NEW);
		String fallbackSummary = TextLimits.summary(report.getText(), TextLimits.FALLBACK_SUMMARY);
		issue.setSummaryRo(report.getStaffSummaryRo() != null ? report.getStaffSummaryRo() : fallbackSummary);
		issue.setSummaryEn(report.getStaffSummaryEn() != null ? report.getStaffSummaryEn() : fallbackSummary);
		issue.setSummarySource(report.getClassificationSource());
		issue.setMentionedPlace(report.getMentionedPlace());
		issue.setEmergency(report.isEmergency());
		issue.setUnclassified(report.getClassificationSource() == ClassificationSource.FALLBACK);
		issue.setReportCount(1);
		issue.setReporterCount(1);
		issue.setFirstReportedAt(report.getCreatedAt());
		issue.setLastReportedAt(report.getCreatedAt());
		issues.save(issue);

		report.setIssue(issue);
		audit(issue, report, AuditAction.ISSUE_CREATED);
		return issue;
	}

	/**
	 * Adds a Report to an open Issue: recounts Reports and distinct Passengers, raises the Severity to the
	 * highest Report's unless staff overrode it, and keeps {@code emergency} and {@code unclassified} true to
	 * every Report.
	 */
	public void attachReport(Issue issue, Report report) {
		report.setIssue(issue);
		// The count queries flush the link above first (Hibernate's AUTO flush mode), so they include this Report.
		issue.setReportCount((int) reports.countByIssueIdAndStatus(issue.getId(), ReportStatus.ACCEPTED));
		issue.setReporterCount((int) reports.countReporters(issue.getId()));
		if (!issue.isSeverityOverridden() && isMoreSevere(report.getSeverity(), issue.getSeverity())) {
			issue.setSeverity(report.getSeverity());
		}
		issue.setEmergency(issue.isEmergency() || report.isEmergency());
		issue.setUnclassified(issue.isUnclassified() && report.getClassificationSource() == ClassificationSource.FALLBACK);
		if (report.getMentionedPlace() != null) {
			issue.setMentionedPlace(report.getMentionedPlace());
		}
		Instant reportedAt = report.getCreatedAt();
		if (reportedAt.isAfter(issue.getLastReportedAt())) {
			issue.setLastReportedAt(reportedAt);
		}
		audit(issue, report, AuditAction.REPORT_ADDED);
	}

	/** CRITICAL is declared first, so a lower ordinal is more severe. */
	private static boolean isMoreSevere(Severity candidate, Severity current) {
		return candidate != null && candidate.ordinal() < current.ordinal();
	}

	private static double confidence(Report report) {
		BigDecimal confidence = report.getConfidence();
		return confidence == null ? 0 : confidence.doubleValue();
	}

	/** Done by the system (no staff member), with no details. */
	private void audit(Issue issue, Report report, AuditAction action) {
		auditService.record(issue, report, null, action, Map.of());
	}
}
