package com.fsociety.flarebackend.ratelimit;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fsociety.flarebackend.report.QuarantineReason;

/** Placeholder until A07: no limits. A07 replaces this class with the real {@link RateLimitService}. */
@Component
public class NoRateLimits implements RateLimitService {

	@Override
	public Optional<QuarantineReason> check(String deviceHash, String networkHash, String qrCodeId) {
		return Optional.empty();
	}
}
