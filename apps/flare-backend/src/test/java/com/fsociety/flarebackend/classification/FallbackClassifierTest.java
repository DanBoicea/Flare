package com.fsociety.flarebackend.classification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.safety.EmergencyTerms;
import org.junit.jupiter.api.Test;

/** What every message becomes while the LLM is off or failing (backend.md "Fallbacks"). */
class FallbackClassifierTest {

	private final FallbackClassifier classifier = new FallbackClassifier(EmergencyTerms.loadDetector());

	@Test
	void everyMessageIsAnUnclassifiedMediumReport() {
		Classification result = classifier.classify(input("Nu mai e hârtie în toaleta de lângă banda 2", Language.RO));

		assertThat(result.getKind()).isEqualTo(Classification.Kind.REPORT);
		assertThat(result.getCategory()).isEqualTo(Category.OTHER);
		assertThat(result.getSeverity()).isEqualTo(Severity.MEDIUM);
		assertThat(result.isEmergency()).isFalse();
		assertThat(result.getSource()).isEqualTo(ClassificationSource.FALLBACK);
		assertThat(result.getLanguage()).isEqualTo(Language.RO);
		assertThat(result.getAnswer()).isNull();
		assertThat(result.getBounceReason()).isNull();
	}

	@Test
	void theSummaryIsTheTextInBothLanguages() {
		Classification result = classifier.classify(input("The lift is stuck", Language.EN));

		assertThat(result.getStaffSummary().getRo()).isEqualTo("The lift is stuck");
		assertThat(result.getStaffSummary().getEn()).isEqualTo("The lift is stuck");
	}

	@Test
	void theSummaryIsCutTo140Characters() {
		Classification result = classifier.classify(input("x".repeat(500), Language.EN));

		assertThat(result.getStaffSummary().getEn()).hasSize(140);
	}

	@Test
	void anEmergencyPhraseMakesItACriticalEmergency() {
		Classification result = classifier.classify(input("There is a fire near gate 2", Language.EN));

		assertThat(result.isEmergency()).isTrue();
		assertThat(result.getSeverity()).isEqualTo(Severity.CRITICAL);
		assertThat(result.getKind()).isEqualTo(Classification.Kind.REPORT);
	}

	private static ClassificationInput input(String text, Language uiLanguage) {
		return new ClassificationInput(text, uiLanguage, Optional.empty(), false);
	}
}
