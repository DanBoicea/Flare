package com.fsociety.flarebackend.llm;

/** Helpers for putting untrusted text into a prompt (security-and-abuse.md section 4). */
public final class PromptText {

	private PromptText() {
	}

	/** Stops untrusted text from closing a tag such as {@code <passenger_message>} and writing fake instructions after it. */
	public static String escapeTags(String text) {
		return text == null ? "" : text.replace("<", "‹").replace(">", "›");
	}
}
