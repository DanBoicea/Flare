package com.fsociety.flarebackend.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.report.QuarantineReason;

/**
 * The rate limits of pipeline.md section 3: per device (10 minutes and a day), per network, per QR code (Strict
 * mode), and a device with too many Bounces. Every counter records the message first, so a flood is counted even
 * when another limit already applies. Strict mode wins over the other reasons, so Needs review shows the attack
 * on the QR code. Emergencies never reach this class. Pure.
 */
public final class InMemoryRateLimitService implements RateLimitService {

	private final FlareProperties.Limits limits;
	private final QrCodeFloodGuard floodGuard;
	private final BounceTracker bounces;
	private final SlidingWindowCounter devicePer10Min;
	private final SlidingWindowCounter devicePerDay;
	private final SlidingWindowCounter networkPer10Min;

	public InMemoryRateLimitService(Clock clock, FlareProperties.Limits limits, QrCodeFloodGuard floodGuard,
			BounceTracker bounces) {
		this.limits = limits;
		this.floodGuard = floodGuard;
		this.bounces = bounces;
		this.devicePer10Min = new SlidingWindowCounter(clock, Duration.ofMinutes(10));
		this.devicePerDay = new SlidingWindowCounter(clock, Duration.ofDays(1));
		this.networkPer10Min = new SlidingWindowCounter(clock, Duration.ofMinutes(10));
	}

	@Override
	public Optional<QuarantineReason> check(String deviceHash, String networkHash, String qrCodeId) {
		boolean device10Ok = devicePer10Min.tryAcquire(deviceHash, limits.getDevice().getPer10Min());
		boolean deviceDayOk = devicePerDay.tryAcquire(deviceHash, limits.getDevice().getPerDay());
		boolean networkOk = networkPer10Min.tryAcquire(networkHash, limits.getNetwork().getPer10Min());
		boolean strictMode = qrCodeId != null && floodGuard.countAndCheck(qrCodeId);
		boolean tooManyBounces = bounces.bounceCount(deviceHash) >= limits.getBounce().getBlockAfter();

		if (strictMode) {
			return Optional.of(QuarantineReason.QR_CODE_STRICT_MODE);
		}
		if (!device10Ok || !deviceDayOk || !networkOk || tooManyBounces) {
			return Optional.of(QuarantineReason.RATE_LIMIT);
		}
		return Optional.empty();
	}
}
