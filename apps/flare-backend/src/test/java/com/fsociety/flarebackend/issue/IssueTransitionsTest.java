package com.fsociety.flarebackend.issue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fsociety.flarebackend.issue.IssueTransitions.Action;

/** Every allowed and refused staff action, per state (data-model.md "Issue state machine"). */
class IssueTransitionsTest {

	@ParameterizedTest(name = "{0} from {1} (taken={2}) allowed={3}")
	@CsvSource({
			// acknowledge: only a NEW Issue
			"ACKNOWLEDGE, NEW,          false, true",
			"ACKNOWLEDGE, ACKNOWLEDGED, false, false",
			"ACKNOWLEDGE, IN_PROGRESS,  true,  false",
			"ACKNOWLEDGE, RESOLVED,     false, false",
			// take: NEW, ACKNOWLEDGED, or IN_PROGRESS that nobody holds (after a hand over)
			"TAKE,        NEW,          false, true",
			"TAKE,        ACKNOWLEDGED, false, true",
			"TAKE,        IN_PROGRESS,  false, true",
			"TAKE,        IN_PROGRESS,  true,  false",
			"TAKE,        RESOLVED,     false, false",
			// resolve: anything but RESOLVED
			"RESOLVE,     NEW,          false, true",
			"RESOLVE,     ACKNOWLEDGED, false, true",
			"RESOLVE,     IN_PROGRESS,  true,  true",
			"RESOLVE,     RESOLVED,     false, false",
			// reopen: only RESOLVED
			"REOPEN,      NEW,          false, false",
			"REOPEN,      ACKNOWLEDGED, false, false",
			"REOPEN,      IN_PROGRESS,  true,  false",
			"REOPEN,      RESOLVED,     false, true",
			// override: any state
			"OVERRIDE,    NEW,          false, true",
			"OVERRIDE,    ACKNOWLEDGED, false, true",
			"OVERRIDE,    IN_PROGRESS,  true,  true",
			"OVERRIDE,    RESOLVED,     false, true",
			// hand over: anything but RESOLVED
			"HAND_OVER,   NEW,          false, true",
			"HAND_OVER,   ACKNOWLEDGED, false, true",
			"HAND_OVER,   IN_PROGRESS,  true,  true",
			"HAND_OVER,   RESOLVED,     false, false",
	})
	void transitions(Action action, IssueStatus status, boolean taken, boolean allowed) {
		assertThat(IssueTransitions.allowed(action, status, taken)).isEqualTo(allowed);
	}
}
