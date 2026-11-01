package com.fsociety.flarebackend.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;

import com.fsociety.flarebackend.audit.AuditAction;
import com.fsociety.flarebackend.audit.AuditEvent;
import com.fsociety.flarebackend.audit.AuditEventRepository;
import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.FlareException;
import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.issue.dto.IssueCard;
import com.fsociety.flarebackend.issue.dto.IssueDetail;
import com.fsociety.flarebackend.issue.dto.IssueList;
import com.fsociety.flarebackend.issue.dto.PriorityReasonCode;
import com.fsociety.flarebackend.location.Location;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.report.ReportStatus;
import com.fsociety.flarebackend.staff.StaffMember;
import com.fsociety.flarebackend.staff.StaffRole;
import com.fsociety.flarebackend.staff.TeamId;

/** The Issue list and detail (A04): each simulated demo account sees exactly what the access policy allows. */
class IssueQueryServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");

	private final IssueRepository issues = mock(IssueRepository.class);
	private final ReportRepository reports = mock(ReportRepository.class);
	private final AuditEventRepository auditEvents = mock(AuditEventRepository.class);
	private final PriorityCalculator priority = new PriorityCalculator(new FlareProperties.Priority(
			Map.of("CRITICAL", 10000, "HIGH", 600, "MEDIUM", 300, "LOW", 100), 25, 10, 120, 50, 50));
	private final IssueQueryService service = new IssueQueryService(issues, reports, auditEvents, priority,
			Clock.fixed(NOW, ZoneOffset.UTC));

	// The four demo accounts from S03/D01.
	private final StaffMember ana = member("Ana (simulat)", Set.of(StaffRole.STAFF), Set.of(TeamId.CLEANING));
	private final StaffMember mihai = member("Mihai (simulat)", Set.of(StaffRole.STAFF), Set.of(TeamId.MAINTENANCE, TeamId.PASSENGER_ASSISTANCE));
	private final StaffMember ioana = member("Ioana (simulat)", Set.of(StaffRole.STAFF), Set.of(TeamId.SECURITY));
	private final StaffMember radu = member("Radu (simulat)", Set.of(StaffRole.DUTY_MANAGER), Set.of());

	private final Issue toilet = issue(TeamId.CLEANING, Severity.MEDIUM, 5);
	private final Issue lift = issue(TeamId.MAINTENANCE, Severity.HIGH, 5);
	private final Issue sign = issue(TeamId.PASSENGER_ASSISTANCE, Severity.LOW, 5);
	private final Issue bag = issue(TeamId.SECURITY, Severity.HIGH, 5);
	private final Issue unrouted = issue(null, Severity.MEDIUM, 5);
	private final Issue smoke = issue(TeamId.CLEANING, Severity.CRITICAL, 1);

	@BeforeEach
	void setUp() {
		smoke.setEmergency(true);
		when(issues.findActive()).thenReturn(List.of(toilet, lift, sign, bag, unrouted, smoke));
	}

	@Test
	void anaFromCleaningSeesOnlyCleaningIssues() {
		assertThat(activeIds(ana)).containsExactly(smoke.getId(), toilet.getId());
	}

	@Test
	void mihaiSeesBothOfHisTeams() {
		assertThat(activeIds(mihai)).containsExactly(lift.getId(), sign.getId());
	}

	@Test
	void ioanaFromSecuritySeesSecurityIssuesAndEveryCriticalOne() {
		assertThat(activeIds(ioana)).containsExactly(smoke.getId(), bag.getId());
	}

	@Test
	void raduTheDutyManagerSeesEverythingIncludingUnroutedIssues() {
		assertThat(activeIds(radu)).containsExactlyInAnyOrder(
				toilet.getId(), lift.getId(), sign.getId(), bag.getId(), unrouted.getId(), smoke.getId());
	}

	@Test
	void theActiveListIsSortedByPriorityWithCriticalFirst() {
		List<IssueCard> cards = service.list(radu, IssueView.ACTIVE).getIssues();

		assertThat(cards.get(0).getId()).isEqualTo(smoke.getId());
		assertThat(cards).extracting(card -> card.getPriority().getScore()).isSortedAccordingTo((a, b) -> b - a);
	}

	@Test
	void theListCarriesTheServerTime() {
		assertThat(service.list(ana, IssueView.ACTIVE).getServerTime()).isEqualTo(NOW);
	}

	@Test
	void aCardShowsLocationTakenByReopenedAndTheReasons() {
		Location location = new Location();
		location.setId(UUID.randomUUID());
		location.setLabel("ARR-WC-02");
		location.setNameRo("Toalete sosiri, lângă banda 2");
		location.setNameEn("Arrivals toilets near belt 2");
		toilet.setLocation(location);
		toilet.setTakenBy(ana);
		toilet.setReopenedCount(1);
		toilet.setReporterCount(3);

		IssueCard card = service.list(ana, IssueView.ACTIVE).getIssues().stream()
				.filter(c -> c.getId().equals(toilet.getId())).findFirst().orElseThrow();

		assertThat(card.getLocation().getLabel()).isEqualTo("ARR-WC-02");
		assertThat(card.getLocation().getName().getEn()).isEqualTo("Arrivals toilets near belt 2");
		assertThat(card.getTakenBy().getDisplayName()).isEqualTo("Ana (simulat)");
		assertThat(card.isReopened()).isTrue();
		assertThat(card.getSummary().getRo()).isEqualTo("Rezumat");
		assertThat(card.getPriority().getReasons()).extracting(r -> r.getCode())
				.contains(PriorityReasonCode.SEVERITY, PriorityReasonCode.REPORTERS, PriorityReasonCode.REOPENED);
	}

	@Test
	void theResolvedListIsFilteredBeforeItIsCappedAt50() {
		List<Issue> resolved = new ArrayList<>();
		IntStream.range(0, 60).forEach(i -> resolved.add(issue(TeamId.MAINTENANCE, Severity.LOW, 30)));
		IntStream.range(0, 55).forEach(i -> resolved.add(issue(TeamId.CLEANING, Severity.LOW, 30)));
		when(issues.findByStatusAndResolvedAtAfterOrderByResolvedAtDesc(eq(IssueStatus.RESOLVED),
				eq(NOW.minus(Duration.ofHours(24))), any(Limit.class))).thenReturn(resolved);

		IssueList list = service.list(ana, IssueView.RESOLVED);

		assertThat(list.getIssues()).hasSize(50).allMatch(card -> card.getTeam() == TeamId.CLEANING);
	}

	@Test
	void theDetailHasReportsAndHistory() {
		Report report = new Report();
		report.setId(UUID.randomUUID());
		report.setText("Nu mai e hârtie");
		report.setLanguage(Language.RO);
		report.setCreatedAt(NOW.minus(Duration.ofMinutes(5)));
		report.setClassificationSource(ClassificationSource.LLM);
		AuditEvent created = event(AuditAction.ISSUE_CREATED, null, "{}");
		AuditEvent changed = event(AuditAction.SEVERITY_CHANGED, ana, "{\"from\":\"MEDIUM\",\"to\":\"HIGH\"}");
		when(issues.findById(toilet.getId())).thenReturn(Optional.of(toilet));
		when(reports.findByIssueIdAndStatusOrderByCreatedAtDesc(toilet.getId(), ReportStatus.ACCEPTED, Limit.of(50)))
				.thenReturn(List.of(report));
		when(auditEvents.findByIssueIdOrderByCreatedAt(toilet.getId())).thenReturn(List.of(created, changed));

		IssueDetail detail = service.detail(ana, toilet.getId());

		assertThat(detail.getCard().getId()).isEqualTo(toilet.getId());
		assertThat(detail.getReports()).singleElement().satisfies(view -> {
			assertThat(view.getText()).isEqualTo("Nu mai e hârtie");
			assertThat(view.getClassificationSource()).isEqualTo(ClassificationSource.LLM);
		});
		assertThat(detail.getHistory()).hasSize(2);
		assertThat(detail.getHistory().get(0).getStaffDisplayName()).isNull();
		assertThat(detail.getHistory().get(1).getStaffDisplayName()).isEqualTo("Ana (simulat)");
		assertThat(detail.getHistory().get(1).getDetails()).containsEntry("from", "MEDIUM").containsEntry("to", "HIGH");
	}

	@Test
	void unreadableHistoryDetailsStillShowTheLine() {
		when(issues.findById(toilet.getId())).thenReturn(Optional.of(toilet));
		when(auditEvents.findByIssueIdOrderByCreatedAt(toilet.getId()))
				.thenReturn(List.of(event(AuditAction.ACKNOWLEDGED, ana, "not json")));

		IssueDetail detail = service.detail(ana, toilet.getId());

		assertThat(detail.getHistory()).singleElement().satisfies(entry -> assertThat(entry.getDetails()).isEmpty());
	}

	@Test
	void anIssueTheMemberCannotSeeIsNotFound() {
		when(issues.findById(lift.getId())).thenReturn(Optional.of(lift));

		assertThatThrownBy(() -> service.detail(ana, lift.getId()))
				.isInstanceOf(FlareException.class)
				.extracting(e -> ((FlareException) e).getCode())
				.isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	void aMissingIssueIsNotFound() {
		UUID missing = UUID.randomUUID();
		when(issues.findById(missing)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.detail(radu, missing))
				.isInstanceOf(FlareException.class)
				.extracting(e -> ((FlareException) e).getCode())
				.isEqualTo(ErrorCode.NOT_FOUND);
	}

	private List<UUID> activeIds(StaffMember member) {
		return service.list(member, IssueView.ACTIVE).getIssues().stream().map(IssueCard::getId).toList();
	}

	private static StaffMember member(String name, Set<StaffRole> roles, Set<TeamId> teams) {
		StaffMember member = new StaffMember();
		member.setId(UUID.randomUUID());
		member.setDisplayName(name);
		member.setActive(true);
		member.setRoles(roles);
		member.setTeams(teams);
		return member;
	}

	private static Issue issue(TeamId team, Severity severity, int minutesAgo) {
		Issue issue = new Issue();
		issue.setId(UUID.randomUUID());
		issue.setVersion(1);
		issue.setTeam(team);
		issue.setSeverity(severity);
		issue.setCategory(Category.OTHER);
		issue.setStatus(IssueStatus.NEW);
		issue.setSummaryRo("Rezumat");
		issue.setSummaryEn("Summary");
		issue.setSummarySource(ClassificationSource.LLM);
		issue.setReportCount(1);
		issue.setReporterCount(1);
		issue.setFirstReportedAt(NOW.minus(Duration.ofMinutes(minutesAgo)));
		issue.setLastReportedAt(NOW.minus(Duration.ofMinutes(minutesAgo)));
		return issue;
	}

	private static AuditEvent event(AuditAction action, StaffMember by, String details) {
		AuditEvent event = new AuditEvent();
		event.setId(UUID.randomUUID());
		event.setAction(action);
		event.setStaffMember(by);
		event.setDetails(details);
		event.setCreatedAt(NOW);
		return event;
	}
}
