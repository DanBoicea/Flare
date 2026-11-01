package com.fsociety.flarebackend.classification;

import java.util.Optional;

import com.fsociety.flarebackend.common.Language;
import lombok.Value;

/**
 * What a {@link MessageClassifier} gets (backend.md). Holds Passenger text, so Lombok's @Value toString is
 * replaced: it must never print the text into logs.
 */
@Value
public class ClassificationInput {
	String text;
	Language uiLanguage;
	Optional<LocationInfo> scannedLocation;
	/** True for "Send to staff anyway": the classifier must then return REPORT. */
	boolean passengerInsisted;

	@Override
	public String toString() {
		return "ClassificationInput[uiLanguage=" + uiLanguage + ", passengerInsisted=" + passengerInsisted + "]";
	}
}
