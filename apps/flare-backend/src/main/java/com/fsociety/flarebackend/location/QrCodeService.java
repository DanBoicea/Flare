package com.fsociety.flarebackend.location;

import java.util.Optional;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

/**
 * Resolves the QR code ID a Passenger scanned (pipeline.md step 3, ADR-0017). A malformed, retired or
 * missing ID is UNKNOWN, never an error: the message is still stored, just Quarantined.
 */
@Service
@RequiredArgsConstructor
public class QrCodeService {

	/** Real IDs are lowercase letters and digits (ADR-0017); anything else can't exist, so skip the lookup. */
	private static final Pattern WELL_FORMED = Pattern.compile("[a-z0-9]{1,32}");

	private final QrCodeRepository qrCodes;

	public enum Status {
		ACTIVE,
		MUTED,
		UNKNOWN
	}

	@Value
	public static class Resolution {
		Status status;
		/** Present for ACTIVE and MUTED. */
		Optional<Location> location;

		static Resolution unknown() {
			return new Resolution(Status.UNKNOWN, Optional.empty());
		}
	}

	public Resolution resolve(String qrCodeId) {
		if (qrCodeId == null || !WELL_FORMED.matcher(qrCodeId).matches()) {
			return Resolution.unknown();
		}
		return qrCodes.findById(qrCodeId)
				.map(qrCode -> switch (qrCode.getStatus()) {
					case ACTIVE -> new Resolution(Status.ACTIVE, Optional.of(qrCode.getLocation()));
					case MUTED -> new Resolution(Status.MUTED, Optional.of(qrCode.getLocation()));
					case RETIRED -> Resolution.unknown();
				})
				.orElseGet(Resolution::unknown);
	}
}
