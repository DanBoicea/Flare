package com.fsociety.flarebackend.report;

/** Why a Report was Quarantined instead of reaching staff lists. */
public enum QuarantineReason {
	RATE_LIMIT,
	QR_CODE_STRICT_MODE,
	UNKNOWN_QR_CODE,
	MUTED_QR_CODE,
	BOT_CHECK,
	SEND_ANYWAY
}
