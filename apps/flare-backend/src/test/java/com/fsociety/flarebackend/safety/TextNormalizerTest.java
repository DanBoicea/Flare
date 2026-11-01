package com.fsociety.flarebackend.safety;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Normalisation used before matching emergency phrases (A07 item 1). */
class TextNormalizerTest {

	@Test
	void lowercasesAndStripsRomanianDiacritics() {
		assertThat(TextNormalizer.normalize("Ăla Âna Îți ȘTIE Țara")).isEqualTo("ala ana iti stie tara");
	}

	@Test
	void treatsTheOldCedillaLettersLikeTheCommaBelowOnes() {
		// ş ţ (cedilla) are still typed by many keyboards instead of ș ț (comma below).
		assertThat(TextNormalizer.normalize("şi ţară")).isEqualTo(TextNormalizer.normalize("și țară"));
	}

	@Test
	void turnsPunctuationIntoSingleSpaces() {
		assertThat(TextNormalizer.normalize("  Mi-am   pierdut,copilul!!! ")).isEqualTo("mi am pierdut copilul");
		assertThat(TextNormalizer.normalize("There's a fire\n\nnear gate 2.")).isEqualTo("there s a fire near gate 2");
	}

	@Test
	void keepsDigits() {
		assertThat(TextNormalizer.normalize("Poarta 12")).isEqualTo("poarta 12");
	}

	@Test
	void handlesEmptyAndNull() {
		assertThat(TextNormalizer.normalize("")).isEmpty();
		assertThat(TextNormalizer.normalize("   ")).isEmpty();
		assertThat(TextNormalizer.normalize(null)).isEmpty();
	}
}
