package com.fsociety.flarebackend.ratelimit;

import org.springframework.stereotype.Component;

/**
 * Placeholder until A07: counts nothing, so "Send anyway" is never offered yet. A07 replaces this class with
 * the real {@link BounceTracker}.
 */
@Component
public class NoBounceTracker implements BounceTracker {

	@Override
	public int recordBounce(String deviceHash) {
		return 0;
	}

	@Override
	public int bounceCount(String deviceHash) {
		return 0;
	}
}
