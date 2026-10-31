package com.fsociety.flarebackend.safety;

import org.springframework.stereotype.Component;

/** Placeholder until A07: lets every request through. A07 replaces this class with the real {@link BotCheck}. */
@Component
public class NoBotCheck implements BotCheck {

	@Override
	public boolean looksAutomated(String honeypot, Long elapsedMs) {
		return false;
	}
}
