package com.fsociety.flarebackend.location;

import java.util.Optional;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

import com.fsociety.flarebackend.report.QuarantineReason;

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

		/** Why a Report from this QR code is Quarantined (pipeline.md step 3), or empty for an active one. */
		public Optional<QuarantineReason> quarantineReason() {
			return switch (status) {
				case ACTIVE -> Optional.empty();
				case MUTED -> Optional.of(QuarantineReason.MUTED_QR_CODE);
				case UNKNOWN -> Optional.of(QuarantineReason.UNKNOWN_QR_CODE);
			};
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
