package com.fsociety.flarebackend.llm;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * A daily cap on LLM calls ({@code flare.llm.daily-call-limit}). There is no per-call cost with a self-hosted
 * model; the cap stops a spam wave from keeping the server busy for everyone. Resets at midnight UTC.
 */
public class LlmUsageCounter {

	private final int dailyLimit;
	private final Clock clock;
	private LocalDate day;
	private int calls;

	public LlmUsageCounter(int dailyLimit, Clock clock) {
		this.dailyLimit = dailyLimit;
		this.clock = clock;
	}

	/** Counts one call, or throws {@link LlmFailure} without counting when today's cap is reached. */
	public synchronized void acquire() {
		LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
		if (!today.equals(day)) {
			day = today;
			calls = 0;
		}
		if (calls >= dailyLimit) {
			throw new LlmFailure("daily LLM call limit reached (" + dailyLimit + ")");
		}
		calls++;
	}

	public synchronized int callsToday() {
		return calls;
	}
}
