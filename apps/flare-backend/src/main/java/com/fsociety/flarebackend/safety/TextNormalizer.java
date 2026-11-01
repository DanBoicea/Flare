package com.fsociety.flarebackend.safety;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Lower case, no diacritics, punctuation as single spaces (A07 item 1). Used to match phrases in Passenger
 * text in a way that doesn't depend on how it was typed (with or without ș ț, or the old ş ţ).
 */
public final class TextNormalizer {

	private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
	private static final Pattern NOT_LETTER_OR_DIGIT = Pattern.compile("[^\\p{L}\\p{N}]+");

	private TextNormalizer() {
	}

	public static String normalize(String text) {
		if (text == null) {
			return "";
		}
		// NFD splits ă into a + breve and ș/ş into s + comma/cedilla; dropping the marks leaves the base letter.
		String withoutMarks = COMBINING_MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
		return NOT_LETTER_OR_DIGIT.matcher(withoutMarks.toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
	}
}
