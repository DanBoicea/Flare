package com.fsociety.flarebackend.ratelimit;

import java.time.Clock;
import java.time.Duration;

/** Bounces per device hash within {@code flare.limits.bounce.window} (pipeline.md section 3). Pure. */
public final class InMemoryBounceTracker implements BounceTracker {

	private final SlidingWindowCounter counter;

	public InMemoryBounceTracker(Clock clock, Duration window) {
		this.counter = new SlidingWindowCounter(clock, window);
	}

	@Override
	public int recordBounce(String deviceHash) {
		return counter.record(deviceHash);
	}

	@Override
	public int bounceCount(String deviceHash) {
		return counter.count(deviceHash);
	}
}
