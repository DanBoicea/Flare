package com.fsociety.flarebackend.common;

import java.util.regex.Pattern;

/** Shared text limits. Counts Unicode code points, so an emoji or ș never ends up cut in half. */
public final class TextLimits {

	/** Length of an Issue summary made from the Passenger's text when the LLM didn't write one (backend.md). */
	public static final int FALLBACK_SUMMARY = 140;

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private TextLimits() {
	}

	/** The text on one line, cut to {@code max} code points with a trailing "…" when it was longer. */
	public static String summary(String text, int max) {
		String oneLine = WHITESPACE.matcher(text == null ? "" : text).replaceAll(" ").strip();
		if (oneLine.codePointCount(0, oneLine.length()) <= max) {
			return oneLine;
		}
		int end = oneLine.offsetByCodePoints(0, max - 1);
		return oneLine.substring(0, end).stripTrailing() + "…";
	}
}
