package com.fsociety.flarebackend.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Counts events per key over a sliding time window (pipeline.md section 3, ADR-0015): one deque of timestamps
 * per key, in memory, single instance. An event exactly {@code window} old no longer counts. Keys with no event
 * left in the window are swept out every few minutes. Pure; thread-safe by synchronising every method, which is
 * plenty at demo scale.
 */
public final class SlidingWindowCounter {

	private static final Duration SWEEP_EVERY = Duration.ofMinutes(5);

	private final Clock clock;
	private final Duration window;
	private final Map<String, Deque<Instant>> events = new HashMap<>();
	private Instant lastSweep;

	public SlidingWindowCounter(Clock clock, Duration window) {
		this.clock = clock;
		this.window = window;
		this.lastSweep = clock.instant();
	}

	/**
	 * Records one event for {@code key} and says whether the key is still within {@code limit}. Every attempt is
	 * recorded, also the ones over the limit, so a flood keeps the key blocked for as long as it lasts.
	 */
	public synchronized boolean tryAcquire(String key, int limit) {
		return record(key) <= limit;
	}

	/** Records one event and returns the key's count in the window, including this one. */
	public synchronized int record(String key) {
		Instant now = clock.instant();
		sweepIfDue(now);
		Deque<Instant> deque = events.computeIfAbsent(key, k -> new ArrayDeque<>());
		prune(deque, now);
		deque.addLast(now);
		return deque.size();
	}

	/** The key's count in the window, without recording anything. */
	public synchronized int count(String key) {
		Deque<Instant> deque = events.get(key);
		if (deque == null) {
			return 0;
		}
		prune(deque, clock.instant());
		return deque.size();
	}

	/** How many keys are held in memory (for tests of the sweep). */
	synchronized int trackedKeys() {
		return events.size();
	}

	private void prune(Deque<Instant> deque, Instant now) {
		Instant cutoff = now.minus(window);
		while (!deque.isEmpty() && !deque.peekFirst().isAfter(cutoff)) {
			deque.pollFirst();
		}
	}

	private void sweepIfDue(Instant now) {
		if (now.isBefore(lastSweep.plus(SWEEP_EVERY))) {
			return;
		}
		events.values().forEach(deque -> prune(deque, now));
		events.values().removeIf(Deque::isEmpty);
		lastSweep = now;
	}
}
