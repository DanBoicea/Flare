package com.fsociety.flarebackend.review.dto;

import java.time.Instant;

import lombok.Value;

import com.fsociety.flarebackend.issue.dto.LocationRef;

/** A QR code in Strict mode (contract schema {@code ReviewAlert}). {@code location} is null for an unknown QR code ID. */
@Value
public class ReviewAlert {

	String qrCodeId;
	LocationRef location;
	String kind = "STRICT_MODE";
	Instant since;
	int messageCount;

	public ReviewAlert(String qrCodeId, LocationRef location, Instant since, int messageCount) {
		this.qrCodeId = qrCodeId;
		this.location = location;
		this.since = since;
		this.messageCount = messageCount;
	}
}
