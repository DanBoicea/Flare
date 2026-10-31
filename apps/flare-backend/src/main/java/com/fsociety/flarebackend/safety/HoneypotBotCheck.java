package com.fsociety.flarebackend.safety;

/**
 * The demo's bot check (ADR-0019): a filled honeypot field, or a page sent faster than a person can type. The
 * passenger web app always sends {@code elapsedMs}, so a request without it didn't come from our page. A bot is
 * Quarantined silently, never rejected. Pure.
 */
public final class HoneypotBotCheck implements BotCheck {

	private final long minElapsedMs;

	public HoneypotBotCheck(long minElapsedMs) {
		this.minElapsedMs = minElapsedMs;
	}

	@Override
	public boolean looksAutomated(String honeypot, Long elapsedMs) {
		return (honeypot != null && !honeypot.isEmpty()) || elapsedMs == null || elapsedMs < minElapsedMs;
	}
}
