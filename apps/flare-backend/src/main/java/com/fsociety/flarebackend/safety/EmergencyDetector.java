package com.fsociety.flarebackend.safety;

import java.util.Collection;
import java.util.List;

/**
 * Recognises emergency phrases (medical, fire, missing child, threats) in Passenger text, in Romanian and
 * English (pipeline.md step 4). Pure: the phrases are given to it, see {@link EmergencyTerms}.
 */
public final class EmergencyDetector {

	/** Normalised phrases, each padded with spaces so only whole words match. */
	private final List<String> phrases;

	public EmergencyDetector(Collection<String> phrases) {
		this.phrases = phrases.stream()
				.map(TextNormalizer::normalize)
				.filter(phrase -> !phrase.isEmpty())
				.map(phrase -> " " + phrase + " ")
				.distinct()
				.toList();
		if (this.phrases.isEmpty()) {
			throw new IllegalArgumentException("The emergency phrase list is empty");
		}
	}

	public boolean isEmergency(String text) {
		String normalized = TextNormalizer.normalize(text);
		if (normalized.isEmpty()) {
			return false;
		}
		String padded = " " + normalized + " ";
		return phrases.stream().anyMatch(padded::contains);
	}
}
