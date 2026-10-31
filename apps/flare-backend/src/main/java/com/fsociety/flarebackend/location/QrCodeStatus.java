package com.fsociety.flarebackend.location;

/** MUTED sends a QR code's messages to Quarantined; RETIRED is treated as unknown. */
public enum QrCodeStatus {
	ACTIVE,
	MUTED,
	RETIRED
}
