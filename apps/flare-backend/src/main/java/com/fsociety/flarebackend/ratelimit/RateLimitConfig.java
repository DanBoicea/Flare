package com.fsociety.flarebackend.ratelimit;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fsociety.flarebackend.config.FlareProperties;

/** The in-memory limits (ADR-0015), configured from {@code flare.limits}. The classes themselves are pure. */
@Configuration
public class RateLimitConfig {

	@Bean
	public QrCodeFloodGuard qrCodeFloodGuard(Clock clock, FlareProperties properties) {
		FlareProperties.QrCode qrCode = properties.getLimits().getQrCode();
		return new QrCodeFloodGuard(clock, qrCode.getPer10Min(), qrCode.getStrictModeDuration());
	}

	@Bean
	public BounceTracker bounceTracker(Clock clock, FlareProperties properties) {
		return new InMemoryBounceTracker(clock, properties.getLimits().getBounce().getWindow());
	}

	@Bean
	public RateLimitService rateLimitService(Clock clock, FlareProperties properties, QrCodeFloodGuard floodGuard,
			BounceTracker bounceTracker) {
		return new InMemoryRateLimitService(clock, properties.getLimits(), floodGuard, bounceTracker);
	}
}
