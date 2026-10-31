package com.fsociety.flarebackend.safety;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The bot check of ADR-0019: honeypot and minimum fill time. */
class HoneypotBotCheckTest {

	private final HoneypotBotCheck check = new HoneypotBotCheck(2000);

	@Test
	void aPersonWithAnEmptyHoneypotWhoTookTimeIsNotABot() {
		assertThat(check.looksAutomated("", 5000L)).isFalse();
		assertThat(check.looksAutomated(null, 5000L)).isFalse();
	}

	@Test
	void exactlyTheMinimumTimeIsAllowed() {
		assertThat(check.looksAutomated("", 2000L)).isFalse();
		assertThat(check.looksAutomated("", 1999L)).isTrue();
	}

	@Test
	void aFilledHoneypotIsABot() {
		assertThat(check.looksAutomated("https://spam.example.invalid", 5000L)).isTrue();
		assertThat(check.looksAutomated(" ", 5000L)).isTrue();
	}

	@Test
	void aMissingFillTimeIsABotBecauseOurPageAlwaysSendsIt() {
		assertThat(check.looksAutomated("", null)).isTrue();
	}
}
