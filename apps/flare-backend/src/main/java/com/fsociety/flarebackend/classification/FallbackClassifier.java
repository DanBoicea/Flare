package com.fsociety.flarebackend.classification;

import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.common.TextLimits;
import com.fsociety.flarebackend.safety.EmergencyDetector;

/**
 * Used whenever the LLM is off or fails (backend.md "Fallbacks", triage-and-dedup.md "LLM outage"): every
 * message becomes an Unclassified Report for the duty manager, so an outage never blocks a genuine Report.
 * The language is taken from the UI, since nothing else detects it without the LLM (an assumption).
 */
public class FallbackClassifier implements MessageClassifier {

	private final EmergencyDetector emergencyDetector;

	public FallbackClassifier(EmergencyDetector emergencyDetector) {
		this.emergencyDetector = emergencyDetector;
	}

	@Override
	public Classification classify(ClassificationInput input) {
		boolean emergency = emergencyDetector.isEmergency(input.getText());
		String summary = TextLimits.summary(input.getText(), TextLimits.FALLBACK_SUMMARY);
		return Classification.builder()
				.kind(Classification.Kind.REPORT)
				.emergency(emergency)
				.category(Category.OTHER)
				.severity(emergency ? Severity.CRITICAL : Severity.MEDIUM)
				.language(input.getUiLanguage())
				.confidence(0)
				.staffSummary(new LocalizedText(summary, summary))
				.source(ClassificationSource.FALLBACK)
				.build();
	}
}
