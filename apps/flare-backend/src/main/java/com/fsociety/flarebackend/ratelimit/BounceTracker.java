package com.fsociety.flarebackend.ratelimit;

/**
 * Counts Bounced messages per device within {@code flare.limits.bounce.window} (pipeline.md steps 7 and 9).
 * From {@code send-anyway-after} Bounces the Passenger may "Send anyway". A07 provides the real counter;
 * until then {@link NoBounceTracker}.
 */
public interface BounceTracker {

	/** Records one Bounce and returns the device's count in the window, including this one. */
	int recordBounce(String deviceHash);

	/** The device's current count in the window. */
	int bounceCount(String deviceHash);
}
