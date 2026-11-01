package com.fsociety.flarebackend.safety;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/**
 * The emergency word list (A07 item 2), loaded from the real safety/emergency-terms.yml. A false
 * positive costs little (staff can lower the Severity); a missed emergency is worse, but the 112 strip is
 * on every screen and the LLM is a second check once it is enabled.
 */
class EmergencyDetectorTest {

	private final EmergencyDetector detector = EmergencyTerms.loadDetector();

	@ParameterizedTest
	@ValueSource(strings = {
			"E foc la poarta 2!",
			"Foc în toaleta de la sosiri",
			"Miroase a fum lângă banda 2",
			"Un domn a leșinat la check-in",
			"Cineva nu mai respiră",
			"Mi-am pierdut copilul",
			"Copil pierdut lângă poarta 3",
			"Cred că are un atac de cord",
			"Sângerează tare la cap",
			"Un bărbat are o armă",
			"Bagaj suspect, cred că e o bombă",
			"A izbucnit un incendiu în parcare",
			"There is a fire in the toilets",
			"There's a fire near gate 2",
			"I smell smoke near belt 2",
			"Someone collapsed at gate 4",
			"A man is not breathing",
			"I think she is having a heart attack",
			"He is bleeding a lot",
			"My child is missing",
			"Lost child near the cafe",
			"A man has a gun",
			"Someone has a knife",
			"There might be a bomb in that bag",
			"Woman unconscious near security",
	})
	void detectsEmergencies(String text) {
		assertThat(detector.isEmergency(text)).as(text).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"The fire exit sign is broken",
			"Fire extinguisher missing near gate 2",
			"Ieșirea de incendiu e blocată de bagaje",
			"Detectorul de fum piuie de o oră",
			"The smoke detector keeps beeping",
			"No smoking sign fell down",
			"Unde e zona de fumători?",
			"Nu mai e hârtie în toaletă",
			"The lift near belt 2 is broken",
			"I lost my bag",
			"Where can I charge my phone?",
	})
	void ignoresEverydayMessagesThatMentionSimilarWords(String text) {
		assertThat(detector.isEmergency(text)).as(text).isFalse();
	}

	@Test
	void matchesWholePhrasesOnly() {
		// "focus" contains "foc" and "gunther" contains "gun", but neither is the whole word.
		assertThat(detector.isEmergency("Can't focus, the music is too loud")).isFalse();
		assertThat(detector.isEmergency("Gunther left his bag at gate 1")).isFalse();
	}

	@Test
	void ignoresEmptyText() {
		assertThat(detector.isEmergency("")).isFalse();
		assertThat(detector.isEmergency(null)).isFalse();
	}
}
