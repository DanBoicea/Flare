package com.fsociety.flarebackend.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Value;

/**
 * QR code Strict mode (pipeline.md section 3): a QR code over its limit in 10 minutes enters Strict mode for
 * {@code strict-mode-duration}. While it lasts, its messages are Quarantined (Emergencies never reach it) and an
 * alert is shown in the duty manager's Needs review (A09). Pure.
 */
public final class QrCodeFloodGuard {

	private static final Duration WINDOW = Duration.ofMinutes(10);

	private final Clock clock;
	private final int limitPer10Min;
	private final Duration strictModeDuration;
	private final SlidingWindowCounter counter;
	private final Map<String, StrictMode> strict = new HashMap<>();

	public QrCodeFloodGuard(Clock clock, int limitPer10Min, Duration strictModeDuration) {
		this.clock = clock;
		this.limitPer10Min = limitPer10Min;
		this.strictModeDuration = strictModeDuration;
		this.counter = new SlidingWindowCounter(clock, WINDOW);
	}

	/** An active Strict mode, as A09 shows it. {@code messageCount} counts the messages since it began. */
	@Value
	public static class StrictModeAlert {
		String qrCodeId;
		Instant since;
		Instant until;
		int messageCount;
	}

	/** Counts one message for the QR code; true when the QR code is in Strict mode, including when this message starts it. */
	public synchronized boolean countAndCheck(String qrCodeId) {
		Instant now = clock.instant();
		expire(now);
		boolean withinLimit = counter.tryAcquire(qrCodeId, limitPer10Min);
		StrictMode active = strict.get(qrCodeId);
		if (active != null) {
			active.messageCount++;
			return true;
		}
		if (!withinLimit) {
			strict.put(qrCodeId, new StrictMode(now, now.plus(strictModeDuration)));
			return true;
		}
		return false;
	}

	/** The QR codes in Strict mode now, the oldest first. */
	public synchronized List<StrictModeAlert> activeAlerts() {
		expire(clock.instant());
		return strict.entrySet().stream()
				.map(e -> new StrictModeAlert(e.getKey(), e.getValue().since, e.getValue().until, e.getValue().messageCount))
				.sorted(Comparator.comparing(StrictModeAlert::getSince))
				.toList();
	}

	private void expire(Instant now) {
		strict.values().removeIf(mode -> !now.isBefore(mode.until));
	}

	private static final class StrictMode {

		final Instant since;
		final Instant until;
		int messageCount = 1;

		StrictMode(Instant since, Instant until) {
			this.since = since;
			this.until = until;
		}
	}
}
