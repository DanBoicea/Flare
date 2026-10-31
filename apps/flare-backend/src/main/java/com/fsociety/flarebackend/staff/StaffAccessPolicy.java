package com.fsociety.flarebackend.staff;

import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.issue.Issue;

/**
 * Who may see and act on which Issue (docs/architecture/auth-and-security.md, ADR-0008).
 * Pure: every staff endpoint calls it on the server, and it never reads roles or Teams from the JWT.
 */
public final class StaffAccessPolicy {

	private StaffAccessPolicy() {
	}

	/** Duty managers see everything; staff see their Teams' Issues; Security also sees every Critical Issue. */
	public static boolean canSee(StaffMember member, Issue issue) {
		return member.hasRole(StaffRole.DUTY_MANAGER)
				|| (issue.getTeam() != null && member.getTeams().contains(issue.getTeam()))
				|| (issue.getSeverity() == Severity.CRITICAL && member.getTeams().contains(TeamId.SECURITY));
	}

	/** Acknowledge, take, resolve, reopen, override and hand over: allowed for anyone who can see the Issue. */
	public static boolean canAct(StaffMember member, Issue issue) {
		return canSee(member, issue);
	}

	/** The Needs review list, release and dismiss. */
	public static boolean canReview(StaffMember member) {
		return member.hasRole(StaffRole.DUTY_MANAGER);
	}
}
