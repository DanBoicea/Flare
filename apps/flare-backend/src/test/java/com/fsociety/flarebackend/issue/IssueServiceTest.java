package com.fsociety.flarebackend.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fsociety.flarebackend.audit.AuditService;
import com.fsociety.flarebackend.classification.CategoryTeamRouting;
import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.staff.StaffMember;
import com.fsociety.flarebackend.staff.TeamId;

/** Creating an Issue from a Report: routing by category, or the Team the duty manager chose on release. */
class IssueServiceTest {

	private final IssueService service = new IssueService(mock(IssueRepository.class), mock(ReportRepository.class),
			mock(AuditService.class), new CategoryTeamRouting(Map.of("TOILET", "CLEANING"), 0.5));

	@Test
	void aClassifiedReportIsRoutedByItsCategory() {
		Issue issue = service.createFromReport(report(Category.TOILET, ClassificationSource.LLM, null));

		assertThat(issue.getTeam()).isEqualTo(TeamId.CLEANING);
	}

	@Test
	void aChosenTeamBeatsRouting() {
		Issue issue = service.createFromReport(report(Category.TOILET, ClassificationSource.LLM, TeamId.SECURITY));

		assertThat(issue.getTeam()).isEqualTo(TeamId.SECURITY);
	}

	@Test
	void aChosenTeamAlsoRoutesAnUnclassifiedReport() {
		Issue issue = service.createFromReport(report(Category.OTHER, ClassificationSource.FALLBACK, TeamId.SECURITY));

		assertThat(issue.getTeam()).isEqualTo(TeamId.SECURITY);
		assertThat(issue.isUnclassified()).isTrue();
	}

	@Test
	void aMatchingReportReopensAResolvedIssueAsTheSystem() {
		Issue issue = new Issue();
		issue.setStatus(IssueStatus.RESOLVED);
		issue.setResolvedAt(Instant.parse("2026-10-31T11:50:00Z"));
		issue.setTakenBy(new StaffMember());
		issue.setReopenedCount(1);

		service.reopenByReport(issue, report(Category.TOILET, ClassificationSource.LLM, null));

		assertThat(issue.getStatus()).isEqualTo(IssueStatus.NEW);
		assertThat(issue.getReopenedCount()).isEqualTo(2);
		assertThat(issue.getTakenBy()).isNull();
		assertThat(issue.getResolvedAt()).isNull();
	}

	private static Report report(Category category, ClassificationSource source, TeamId teamOverride) {
		Report report = new Report();
		report.setText("Toaleta e murdară");
		report.setCategory(category);
		report.setSeverity(Severity.MEDIUM);
		report.setClassificationSource(source);
		report.setConfidence(new BigDecimal("0.90"));
		report.setTeamOverride(teamOverride);
		report.setCreatedAt(Instant.parse("2026-10-31T12:00:00Z"));
		return report;
	}
}
