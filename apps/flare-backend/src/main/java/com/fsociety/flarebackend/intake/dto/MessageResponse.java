package com.fsociety.flarebackend.intake.dto;

import java.util.UUID;

import com.fsociety.flarebackend.classification.BounceReason;

import lombok.Value;

/**
 * Response of {@code POST /api/public/messages} (contract schema {@code MessageResponse}). All four fields are
 * always sent; the ones that don't apply to the outcome are null. Quarantined and fallback messages are
 * {@code REPORTED} like any Report, so a spammer cannot tell they were caught. Holds the Knowledge base
 * answer, not Passenger text, but toString is still kept short.
 */
@Value
public class MessageResponse {

	Outcome outcome;
	UUID questionId;
	Answer answer;
	Bounce bounce;

	@Value
	public static class Answer {
		String text;
		boolean found;
	}

	@Value
	public static class Bounce {
		BounceReason reason;
		boolean sendAnywayAvailable;
	}

	public static MessageResponse reported() {
		return new MessageResponse(Outcome.REPORTED, null, null, null);
	}

	public static MessageResponse emergency() {
		return new MessageResponse(Outcome.EMERGENCY, null, null, null);
	}

	public static MessageResponse answered(UUID questionId, String text, boolean found) {
		return new MessageResponse(Outcome.ANSWERED, questionId, new Answer(text, found), null);
	}

	public static MessageResponse bounced(BounceReason reason, boolean sendAnywayAvailable) {
		return new MessageResponse(Outcome.BOUNCED, null, null, new Bounce(reason, sendAnywayAvailable));
	}

	@Override
	public String toString() {
		return "MessageResponse[outcome=" + outcome + "]";
	}
}
