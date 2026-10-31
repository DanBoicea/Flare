package com.fsociety.flarebackend.issue.dto;

import java.time.Instant;
import java.util.UUID;

import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.Language;
import lombok.Value;

/**
 * One Passenger Report inside an Issue's detail (contract schema {@code ReportView}). {@code text} is untrusted
 * and shown as plain text only. Holds Passenger text and contact details, so toString is replaced.
 */
@Value
public class ReportView {
	UUID id;
	String text;
	Language language;
	Instant createdAt;
	String contact;
	String flightNumber;
	String mentionedPlace;
	ClassificationSource classificationSource;

	@Override
	public String toString() {
		return "ReportView[id=" + id + "]";
	}
}
