package com.fsociety.flarebackend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.fsociety.flarebackend.common.MutableClock;

/** QR code Strict mode: entered above the limit, lasts its duration, shown as an alert. */
class QrCodeFloodGuardTest {

	private static final Instant START = Instant.parse("2026-10-31T12:00:00Z");

	private final MutableClock clock = new MutableClock(START);
	private final QrCodeFloodGuard guard = new QrCodeFloodGuard(clock, 20, Duration.ofMinutes(30));

	@Test
	void theTwentyFirstMessageStartsStrictMode() {
		IntStream.range(0, 20).forEach(i -> assertThat(guard.countAndCheck("skp4fq3r5e")).isFalse());

		assertThat(guard.countAndCheck("skp4fq3r5e")).isTrue();
		assertThat(guard.activeAlerts()).singleElement().satisfies(alert -> {
			assertThat(alert.getQrCodeId()).isEqualTo("skp4fq3r5e");
			assertThat(alert.getSince()).isEqualTo(START);
			assertThat(alert.getUntil()).isEqualTo(START.plus(Duration.ofMinutes(30)));
			assertThat(alert.getMessageCount()).isEqualTo(1);
		});
	}

	@Test
	void strictModeLastsItsDurationAndCountsTheMessages() {
		flood("skp4fq3r5e", 21);
		clock.advance(Duration.ofMinutes(15));

		// The window has emptied, but Strict mode still holds.
		assertThat(guard.countAndCheck("skp4fq3r5e")).isTrue();
		assertThat(guard.activeAlerts().get(0).getMessageCount()).isEqualTo(2);

		clock.advance(Duration.ofMinutes(15));
		assertThat(guard.activeAlerts()).isEmpty();
		assertThat(guard.countAndCheck("skp4fq3r5e")).isFalse();
	}

	@Test
	void otherQrCodesAreNotAffected() {
		flood("skp4fq3r5e", 21);

		assertThat(guard.countAndCheck("xw9fwv52ua")).isFalse();
	}

	private void flood(String qrCodeId, int messages) {
		IntStream.range(0, messages).forEach(i -> guard.countAndCheck(qrCodeId));
	}
}
