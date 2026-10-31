package com.fsociety.flarebackend.review.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Value;

import com.fsociety.flarebackend.issue.dto.LocationRef;
import com.fsociety.flarebackend.report.QuarantineReason;

/**
 * One Quarantined Report in Needs review (contract schema {@code ReviewItem}). {@code text} is untrusted and shown
 * as plain text only; toString is replaced so it never reaches the logs.
 */
@Value
public class ReviewItem {

	UUID reportId;
	String text;
	Instant createdAt;
	QuarantineReason reason;
	String scannedQrCodeId;
	LocationRef location;

	@Override
	public String toString() {
		return "ReviewItem[reportId=" + reportId + ", reason=" + reason + "]";
	}
}
