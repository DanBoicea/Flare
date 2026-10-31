package com.fsociety.flarebackend.classification;

/** Why a message was Bounced back to the Passenger (contract schema {@code BounceReason}, ADR-0001). */
public enum BounceReason {
	NOT_AIRPORT_RELATED,
	JOKE_OR_NONSENSE,
	SPAM_OR_ADVERTISING,
	ABUSIVE,
	TOO_UNCLEAR
}
