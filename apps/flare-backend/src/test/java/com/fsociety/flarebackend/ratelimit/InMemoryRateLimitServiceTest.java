package com.fsociety.flarebackend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.fsociety.flarebackend.common.MutableClock;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.report.QuarantineReason;

/** The demo defaults of pipeline.md section 3: device, day, network, QR code and Bounces. */
class InMemoryRateLimitServiceTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-10-31T12:00:00Z"));
	private final FlareProperties.Limits limits = new FlareProperties.Limits(
			new FlareProperties.Device(3, 10),
			new FlareProperties.Network(30),
			new FlareProperties.QrCode(20, Duration.ofMinutes(30)),
			new FlareProperties.Bounce(Duration.ofMinutes(30), 3, 6),
			new FlareProperties.BotCheck(2000));
	private final InMemoryBounceTracker bounces = new InMemoryBounceTracker(clock, Duration.ofMinutes(30));
	private final InMemoryRateLimitService service = new InMemoryRateLimitService(clock, limits,
			new QrCodeFloodGuard(clock, 20, Duration.ofMinutes(30)), bounces);

	@Test
	void aDeviceMaySendThreeMessagesInTenMinutes() {
		IntStream.range(0, 3).forEach(i -> assertThat(service.check("d1", "n" + i, "q" + i)).isEmpty());

		assertThat(service.check("d1", "n9", "q9")).contains(QuarantineReason.RATE_LIMIT);
	}

	@Test
	void theDeviceLimitFreesUpAfterTenMinutesButTheDailyLimitStays() {
		for (int round = 0; round < 3; round++) {
			IntStream.range(0, 3).forEach(i -> assertThat(service.check("d1", "n", "q")).isEmpty());
			clock.advance(Duration.ofMinutes(10));
		}
		assertThat(service.check("d1", "n", "q")).isEmpty(); // the 10th today

		assertThat(service.check("d1", "n", "q")).contains(QuarantineReason.RATE_LIMIT); // the 11th today
	}

	@Test
	void aNetworkMaySendThirtyMessagesInTenMinutes() {
		IntStream.range(0, 30).forEach(i -> assertThat(service.check("d" + i, "airport-wifi", "q" + i)).isEmpty());

		assertThat(service.check("d99", "airport-wifi", "q99")).contains(QuarantineReason.RATE_LIMIT);
	}

	@Test
	void aFloodedQrCodeIsInStrictModeWhichWinsOverOtherReasons() {
		IntStream.range(0, 20).forEach(i -> assertThat(service.check("d" + i, "n" + i, "skp4fq3r5e")).isEmpty());

		assertThat(service.check("d20", "n20", "skp4fq3r5e")).contains(QuarantineReason.QR_CODE_STRICT_MODE);
		assertThat(service.check("d0", "n0", "skp4fq3r5e")).contains(QuarantineReason.QR_CODE_STRICT_MODE);
	}

	@Test
	void aDeviceWithSixBouncesIsRateLimited() {
		IntStream.range(0, 6).forEach(i -> bounces.recordBounce("d1"));

		assertThat(service.check("d1", "n", "q")).contains(QuarantineReason.RATE_LIMIT);
	}

	@Test
	void fiveBouncesAreNotYetBlocked() {
		IntStream.range(0, 5).forEach(i -> bounces.recordBounce("d1"));

		assertThat(service.check("d1", "n", "q")).isEmpty();
	}

	@Test
	void aMessageWithoutAQrCodeIsStillLimitedPerDevice() {
		IntStream.range(0, 3).forEach(i -> assertThat(service.check("d1", "n", null)).isEmpty());

		assertThat(service.check("d1", "n", null)).contains(QuarantineReason.RATE_LIMIT);
	}
}
