package com.fsociety.flarebackend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fsociety.flarebackend.common.MutableClock;

/** Bounces per device within the window. */
class InMemoryBounceTrackerTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-10-31T12:00:00Z"));
	private final InMemoryBounceTracker tracker = new InMemoryBounceTracker(clock, Duration.ofMinutes(30));

	@Test
	void countsBouncesPerDevice() {
		assertThat(tracker.recordBounce("d1")).isEqualTo(1);
		assertThat(tracker.recordBounce("d1")).isEqualTo(2);
		assertThat(tracker.bounceCount("d1")).isEqualTo(2);
		assertThat(tracker.bounceCount("d2")).isZero();
	}

	@Test
	void bouncesOlderThanTheWindowAreForgotten() {
		tracker.recordBounce("d1");
		clock.advance(Duration.ofMinutes(30));

		assertThat(tracker.bounceCount("d1")).isZero();
	}
}
