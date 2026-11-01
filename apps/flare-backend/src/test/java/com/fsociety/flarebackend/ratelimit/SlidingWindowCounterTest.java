package com.fsociety.flarebackend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fsociety.flarebackend.common.MutableClock;

/** The sliding window behind every limit (pipeline.md section 3), including its edges. */
class SlidingWindowCounterTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-10-31T12:00:00Z"));
	private final SlidingWindowCounter counter = new SlidingWindowCounter(clock, Duration.ofMinutes(10));

	@Test
	void allowsUpToTheLimitThenRefuses() {
		assertThat(counter.tryAcquire("d1", 3)).isTrue();
		assertThat(counter.tryAcquire("d1", 3)).isTrue();
		assertThat(counter.tryAcquire("d1", 3)).isTrue();
		assertThat(counter.tryAcquire("d1", 3)).isFalse();
	}

	@Test
	void keysAreIndependent() {
		counter.tryAcquire("d1", 1);

		assertThat(counter.tryAcquire("d2", 1)).isTrue();
		assertThat(counter.tryAcquire("d1", 1)).isFalse();
	}

	@Test
	void anEventExactlyAWindowOldNoLongerCounts() {
		counter.record("d1");
		clock.advance(Duration.ofMinutes(10).minusMillis(1));
		assertThat(counter.count("d1")).isEqualTo(1);

		clock.advance(Duration.ofMillis(1));
		assertThat(counter.count("d1")).isZero();
	}

	@Test
	void refusedAttemptsAreCountedSoAFloodStaysBlocked() {
		counter.tryAcquire("d1", 1);
		clock.advance(Duration.ofMinutes(6));
		assertThat(counter.tryAcquire("d1", 1)).isFalse();

		// The first event has left the window, but the refused one 6 minutes later still counts.
		clock.advance(Duration.ofMinutes(5));
		assertThat(counter.tryAcquire("d1", 1)).isFalse();
	}

	@Test
	void recordReturnsTheCountIncludingThisEvent() {
		assertThat(counter.record("d1")).isEqualTo(1);
		assertThat(counter.record("d1")).isEqualTo(2);
		assertThat(counter.count("d1")).isEqualTo(2);
		assertThat(counter.count("unknown")).isZero();
	}

	@Test
	void idleKeysAreSweptOut() {
		counter.record("d1");
		counter.record("d2");
		clock.advance(Duration.ofMinutes(11));

		counter.record("d3");

		assertThat(counter.trackedKeys()).isEqualTo(1);
	}
}
