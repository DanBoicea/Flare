package com.fsociety.flarebackend.issue.dto;

/** Why an Issue is where it is in the list (contract schema {@code PriorityReasonCode}). */
public enum PriorityReasonCode {
	SEVERITY,
	REPORTERS,
	WAITING,
	CRITICAL_LOCATION,
	EMERGENCY,
	REOPENED
}
