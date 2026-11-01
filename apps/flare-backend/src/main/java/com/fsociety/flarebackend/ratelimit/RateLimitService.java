package com.fsociety.flarebackend.ratelimit;

import java.util.Optional;

import com.fsociety.flarebackend.report.QuarantineReason;

/**
 * Pipeline step 6 (section 3): counts this message against the device, network and QR code limits, and says
 * whether it is over one. Emergencies never reach it, so they are never counted. Implemented by
 * {@link InMemoryRateLimitService}.
 */
public interface RateLimitService {

	/** {@code RATE_LIMIT} or {@code QR_CODE_STRICT_MODE} when over a limit, otherwise empty. */
	Optional<QuarantineReason> check(String deviceHash, String networkHash, String qrCodeId);
}
