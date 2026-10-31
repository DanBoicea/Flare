package com.fsociety.flarebackend.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fsociety.flarebackend.audit.AuditAction;
import com.fsociety.flarebackend.audit.AuditService;
import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.FlareException;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.issue.dto.IssueDetail;
import com.fsociety.flarebackend.staff.StaffMember;
import com.fsociety.flarebackend.staff.StaffRole;
import com.fsociety.flarebackend.staff.TeamId;

/** Staff actions (A05): visibility, version, transition, the change itself and the audit log. */
class IssueActionServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");
	private static final Instant EARLIER = Instant.parse("2026-10-31T11:40:00Z");

	private final IssueRepository issues = mock(IssueRepository.class);
	private final AuditService audit = mock(AuditService.class);
	private final IssueQueryService queries = mock(IssueQueryService.class);
	private final IssueActionService service = new IssueActionService(issues, audit, queries, Clock.fixed(NOW, ZoneOffset.UTC));

	private final StaffMember ana = member("Ana (simulat)", Set.of(StaffRole.STAFF), Set.of(TeamId.CLEANING));
	private final StaffMember mihai = member("Mihai (simulat)", Set.of(StaffRole.STAFF), Set.of(TeamId.MAINTENANCE));
	private final StaffMember radu = member("Radu (simulat)", Set.of(StaffRole.DUTY_MANAGER), Set.of());
	private final IssueDetail detail = mock(IssueDetail.class);
	private Issue issue;

	@BeforeEach
	void setUp() {
		issue = new Issue();
		issue.setId(UUID.randomUUID());
		issue.setVersion(3);
		issue.setStatus(IssueStatus.NEW);
		issue.setTeam(TeamId.CLEANING);
		issue.setCategory(Category.TOILET);
		issue.setSeverity(Severity.MEDIUM);
		when(issues.findById(issue.getId())).thenReturn(Optional.of(issue));
		when(queries.detailOf(issue)).thenReturn(detail);
	}

	@Test
	void acknowledgeMarksItAcknowledgedAndRecordsWhoDidIt() {
		IssueDetail result = service.acknowledge(ana, issue.getId(), 3);

		assertThat(issue.getStatus()).isEqualTo(IssueStatus.ACKNOWLEDGED);
		assertThat(issue.getAcknowledgedAt()).isEqualTo(NOW);
		verify(issues).saveAndFlush(issue);
		verify(audit).record(issue, null, ana, AuditAction.ACKNOWLEDGED, Map.of());
		assertThat(result).isSameAs(detail);
	}

	@Test
	void takeFromNewAcknowledgesTooAndSetsTheHolder() {
		service.take(ana, issue.getId(), 3);

		assertThat(issue.getStatus()).isEqualTo(IssueStatus.IN_PROGRESS);
		assertThat(issue.getTakenBy()).isSameAs(ana);
		assertThat(issue.getAcknowledgedAt()).isEqualTo(NOW);
		verify(audit).record(issue, null, ana, AuditAction.TAKEN, Map.of());
	}

	@Test
	void takeKeepsAnEarlierAcknowledgement() {
		issue.setStatus(IssueStatus.ACKNOWLEDGED);
		issue.setAcknowledgedAt(EARLIER);

		service.take(ana, issue.getId(), 3);

		assertThat(issue.getAcknowledgedAt()).isEqualTo(EARLIER);
	}

	@Test
	void anIssueSomeoneHoldsCannotBeTaken() {
		issue.setStatus(IssueStatus.IN_PROGRESS);
		issue.setTakenBy(radu);

		assertCode(() -> service.take(ana, issue.getId(), 3), ErrorCode.INVALID_TRANSITION);
		verify(issues, never()).saveAndFlush(any());
	}

	@Test
	void resolveFromNewTakesOneTap() {
		service.resolve(ana, issue.getId(), 3);

		assertThat(issue.getStatus()).isEqualTo(IssueStatus.RESOLVED);
		assertThat(issue.getResolvedAt()).isEqualTo(NOW);
		verify(audit).record(issue, null, ana, AuditAction.RESOLVED, Map.of());
	}

	@Test
	void reopenStartsAgainAndCountsIt() {
		issue.setStatus(IssueStatus.RESOLVED);
		issue.setResolvedAt(EARLIER);
		issue.setTakenBy(ana);

		service.reopen(ana, issue.getId(), 3);

		assertThat(issue.getStatus()).isEqualTo(IssueStatus.NEW);
		assertThat(issue.getReopenedCount()).isEqualTo(1);
		assertThat(issue.getTakenBy()).isNull();
		assertThat(issue.getResolvedAt()).isNull();
		verify(audit).record(issue, null, ana, AuditAction.REOPENED, Map.of());
	}

	@Test
	void reopeningAnOpenIssueIsAnInvalidTransition() {
		assertCode(() -> service.reopen(ana, issue.getId(), 3), ErrorCode.INVALID_TRANSITION);
	}

	@Test
	void overrideSetsTheValuesAndTheirFlagsAndRecordsFromAndTo() {
		service.override(ana, issue.getId(), 3, Category.CLEANING_OR_SPILL, Severity.HIGH);

		assertThat(issue.getCategory()).isEqualTo(Category.CLEANING_OR_SPILL);
		assertThat(issue.isCategoryOverridden()).isTrue();
		assertThat(issue.getSeverity()).isEqualTo(Severity.HIGH);
		assertThat(issue.isSeverityOverridden()).isTrue();
		assertThat(issue.getTeam()).isEqualTo(TeamId.CLEANING);
		verify(audit).record(issue, null, ana, AuditAction.CATEGORY_CHANGED, Map.of("from", "TOILET", "to", "CLEANING_OR_SPILL"));
		verify(audit).record(issue, null, ana, AuditAction.SEVERITY_CHANGED, Map.of("from", "MEDIUM", "to", "HIGH"));
	}

	@Test
	void overrideWithTheSameValueSetsTheFlagWithoutAHistoryLine() {
		service.override(ana, issue.getId(), 3, null, Severity.MEDIUM);

		assertThat(issue.isSeverityOverridden()).isTrue();
		assertThat(issue.isCategoryOverridden()).isFalse();
		verify(issues).saveAndFlush(issue);
		verifyNoInteractions(audit);
	}

	@Test
	void overrideNeedsAtLeastOneValue() {
		assertCode(() -> service.override(ana, issue.getId(), 3, null, null), ErrorCode.INVALID_REQUEST);
		verifyNoInteractions(issues);
	}

	@Test
	void handOverMovesTheTeamClearsTheHolderAndKeepsTheStatus() {
		issue.setStatus(IssueStatus.IN_PROGRESS);
		issue.setTakenBy(ana);

		service.handOver(ana, issue.getId(), 3, TeamId.SECURITY);

		assertThat(issue.getTeam()).isEqualTo(TeamId.SECURITY);
		assertThat(issue.getTakenBy()).isNull();
		assertThat(issue.getStatus()).isEqualTo(IssueStatus.IN_PROGRESS);
		verify(audit).record(issue, null, ana, AuditAction.HANDED_OVER, Map.of("from", "CLEANING", "to", "SECURITY"));
	}

	@Test
	void theDutyManagerCanRouteAnUnroutedIssue() {
		issue.setTeam(null);

		service.handOver(radu, issue.getId(), 3, TeamId.MAINTENANCE);

		assertThat(issue.getTeam()).isEqualTo(TeamId.MAINTENANCE);
		verify(audit).record(eq(issue), isNull(), eq(radu), eq(AuditAction.HANDED_OVER), anyMap());
	}

	@Test
	void handingOverToTheSameTeamIsRefused() {
		assertCode(() -> service.handOver(ana, issue.getId(), 3, TeamId.CLEANING), ErrorCode.INVALID_TRANSITION);
	}

	@Test
	void aResolvedIssueCannotBeHandedOver() {
		issue.setStatus(IssueStatus.RESOLVED);

		assertCode(() -> service.handOver(ana, issue.getId(), 3, TeamId.SECURITY), ErrorCode.INVALID_TRANSITION);
	}

	@Test
	void anOldVersionIsAConflictAndChangesNothing() {
		assertCode(() -> service.take(ana, issue.getId(), 2), ErrorCode.VERSION_CONFLICT);

		assertThat(issue.getStatus()).isEqualTo(IssueStatus.NEW);
		verify(issues, never()).saveAndFlush(any());
		verifyNoInteractions(audit);
	}

	@Test
	void anIssueTheCallerCannotSeeIsNotFound() {
		assertCode(() -> service.take(mihai, issue.getId(), 3), ErrorCode.NOT_FOUND);
	}

	@Test
	void aMissingIssueIsNotFound() {
		UUID missing = UUID.randomUUID();
		when(issues.findById(missing)).thenReturn(Optional.empty());

		assertCode(() -> service.resolve(radu, missing, 1), ErrorCode.NOT_FOUND);
	}

	@Test
	void securityCanActOnACriticalIssueOfAnotherTeam() {
		StaffMember ioana = member("Ioana (simulat)", Set.of(StaffRole.STAFF), Set.of(TeamId.SECURITY));
		issue.setSeverity(Severity.CRITICAL);

		service.take(ioana, issue.getId(), 3);

		assertThat(issue.getTakenBy()).isSameAs(ioana);
	}

	private static void assertCode(Runnable call, ErrorCode code) {
		assertThatThrownBy(call::run)
				.isInstanceOf(FlareException.class)
				.extracting(e -> ((FlareException) e).getCode())
				.isEqualTo(code);
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
}
