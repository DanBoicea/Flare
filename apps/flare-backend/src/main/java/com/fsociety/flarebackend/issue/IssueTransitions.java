package com.fsociety.flarebackend.issue;

/**
 * Which staff action is allowed in which Issue state (data-model.md "Issue state machine"). Pure; refused
 * actions become {@code 409 INVALID_TRANSITION}.
 *
 * <pre>
 * acknowledge  NEW
 * take         NEW, ACKNOWLEDGED, or IN_PROGRESS when nobody holds it (hand over clears the holder)
 * resolve      anything but RESOLVED, so a quick fix needs one tap
 * reopen       RESOLVED
 * override     any state
 * hand over    anything but RESOLVED
 * </pre>
 */
public final class IssueTransitions {

	public enum Action {
		ACKNOWLEDGE,
		TAKE,
		RESOLVE,
		REOPEN,
		OVERRIDE,
		HAND_OVER
	}

	private IssueTransitions() {
	}

	public static boolean allowed(Action action, IssueStatus status, boolean taken) {
		return switch (action) {
			case ACKNOWLEDGE -> status == IssueStatus.NEW;
			case TAKE -> status == IssueStatus.NEW || status == IssueStatus.ACKNOWLEDGED
					|| (status == IssueStatus.IN_PROGRESS && !taken);
			case RESOLVE, HAND_OVER -> status != IssueStatus.RESOLVED;
			case REOPEN -> status == IssueStatus.RESOLVED;
			case OVERRIDE -> true;
		};
	}
}
