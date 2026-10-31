package com.fsociety.flarebackend.safety;

/**
 * Pipeline step 5 (ADR-0019): does the request look automated? A filled honeypot field, or a page submitted
 * faster than a person can type, means yes. A07 provides the real check; until then {@link NoBotCheck}.
 */
public interface BotCheck {

	/** {@code honeypot} is the hidden {@code website} field; {@code elapsedMs} the time since the page opened. */
	boolean looksAutomated(String honeypot, Long elapsedMs);
}
