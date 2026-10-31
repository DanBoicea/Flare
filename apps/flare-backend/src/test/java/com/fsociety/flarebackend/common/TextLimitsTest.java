package com.fsociety.flarebackend.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The fallback summary: the Passenger's text, tidied and cut to fit an Issue card. */
class TextLimitsTest {

	@Test
	void keepsShortTextAsIsButTidiesWhitespace() {
		assertThat(TextLimits.summary("  Nu mai e\n\nhârtie   în toaletă ", 140)).isEqualTo("Nu mai e hârtie în toaletă");
	}

	@Test
	void cutsLongTextWithAnEllipsisWithinTheLimit() {
		String summary = TextLimits.summary("a".repeat(200), 140);

		assertThat(summary.codePointCount(0, summary.length())).isEqualTo(140);
		assertThat(summary).endsWith("…").startsWith("aaa");
	}

	@Test
	void textExactlyAtTheLimitIsNotCut() {
		assertThat(TextLimits.summary("b".repeat(140), 140)).isEqualTo("b".repeat(140));
	}

	@Test
	void neverSplitsACharacterMadeOfTwoCodeUnits() {
		// Each emoji is one code point but two Java chars; a naive substring would leave half of one.
		String summary = TextLimits.summary("🚽".repeat(150), 140);

		assertThat(summary.codePointCount(0, summary.length())).isEqualTo(140);
		assertThat(summary).isEqualTo("🚽".repeat(139) + "…");
	}
}
