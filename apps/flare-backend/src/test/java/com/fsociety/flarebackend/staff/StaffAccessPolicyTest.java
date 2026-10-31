package com.fsociety.flarebackend.staff;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.issue.Issue;
import org.junit.jupiter.api.Test;

/** The visibility rules from docs/architecture/auth-and-security.md. */
class StaffAccessPolicyTest {

	@Test
	void staffSeeIssuesOfEveryTeamTheyAreIn() {
		StaffMember mihai = member(Set.of(StaffRole.STAFF), Set.of(TeamId.MAINTENANCE, TeamId.PASSENGER_ASSISTANCE));

		assertThat(StaffAccessPolicy.canSee(mihai, issue(TeamId.MAINTENANCE, Severity.MEDIUM))).isTrue();
		assertThat(StaffAccessPolicy.canSee(mihai, issue(TeamId.PASSENGER_ASSISTANCE, Severity.LOW))).isTrue();
	}

	@Test
	void staffDontSeeAnotherTeamsIssue() {
		StaffMember ana = member(Set.of(StaffRole.STAFF), Set.of(TeamId.CLEANING));

		assertThat(StaffAccessPolicy.canSee(ana, issue(TeamId.SECURITY, Severity.HIGH))).isFalse();
		assertThat(StaffAccessPolicy.canAct(ana, issue(TeamId.SECURITY, Severity.HIGH))).isFalse();
	}

	@Test
	void unroutedIssuesAreForTheDutyManagerOnly() {
		Issue unrouted = issue(null, Severity.MEDIUM);

		assertThat(StaffAccessPolicy.canSee(member(Set.of(StaffRole.STAFF), Set.of(TeamId.values())), unrouted)).isFalse();
		assertThat(StaffAccessPolicy.canSee(member(Set.of(StaffRole.DUTY_MANAGER), Set.of()), unrouted)).isTrue();
	}

	@Test
	void securitySeesEveryCriticalIssue() {
		StaffMember ioana = member(Set.of(StaffRole.STAFF), Set.of(TeamId.SECURITY));

		assertThat(StaffAccessPolicy.canSee(ioana, issue(TeamId.CLEANING, Severity.CRITICAL))).isTrue();
		assertThat(StaffAccessPolicy.canSee(ioana, issue(null, Severity.CRITICAL))).isTrue();
		assertThat(StaffAccessPolicy.canSee(ioana, issue(TeamId.CLEANING, Severity.HIGH))).isFalse();
	}

	@Test
	void criticalIssuesOfAnotherTeamStayHiddenFromNonSecurityStaff() {
		StaffMember ana = member(Set.of(StaffRole.STAFF), Set.of(TeamId.CLEANING));

		assertThat(StaffAccessPolicy.canSee(ana, issue(TeamId.MAINTENANCE, Severity.CRITICAL))).isFalse();
	}

	@Test
	void theDutyManagerSeesEverythingAndReviews() {
		StaffMember radu = member(Set.of(StaffRole.DUTY_MANAGER), Set.of());

		for (TeamId team : TeamId.values()) {
			assertThat(StaffAccessPolicy.canSee(radu, issue(team, Severity.LOW))).isTrue();
			assertThat(StaffAccessPolicy.canAct(radu, issue(team, Severity.LOW))).isTrue();
		}
		assertThat(StaffAccessPolicy.canReview(radu)).isTrue();
	}

	@Test
	void aMemberWithBothRolesHasDutyManagerAccess() {
		StaffMember both = member(Set.of(StaffRole.STAFF, StaffRole.DUTY_MANAGER), Set.of(TeamId.CLEANING));

		assertThat(StaffAccessPolicy.canSee(both, issue(TeamId.SECURITY, Severity.LOW))).isTrue();
		assertThat(StaffAccessPolicy.canReview(both)).isTrue();
	}

	@Test
	void aMemberWithNoRolesGetsOrdinaryStaffAccess() {
		StaffMember noRoles = member(Set.of(), Set.of(TeamId.CLEANING));

		assertThat(StaffAccessPolicy.canSee(noRoles, issue(TeamId.CLEANING, Severity.LOW))).isTrue();
		assertThat(StaffAccessPolicy.canSee(noRoles, issue(TeamId.SECURITY, Severity.LOW))).isFalse();
		assertThat(StaffAccessPolicy.canReview(noRoles)).isFalse();
	}

	@Test
	void ordinaryStaffCantReview() {
		assertThat(StaffAccessPolicy.canReview(member(Set.of(StaffRole.STAFF), Set.of(TeamId.SECURITY)))).isFalse();
	}

	private static StaffMember member(Set<StaffRole> roles, Set<TeamId> teams) {
		StaffMember member = new StaffMember();
		member.setId(UUID.randomUUID());
		member.setActive(true);
		member.getRoles().addAll(roles);
		member.getTeams().addAll(teams);
		return member;
	}

	private static Issue issue(TeamId team, Severity severity) {
		Issue issue = new Issue();
		issue.setTeam(team);
		issue.setSeverity(severity);
		return issue;
	}
}
