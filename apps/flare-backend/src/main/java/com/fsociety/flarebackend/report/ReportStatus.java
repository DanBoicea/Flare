package com.fsociety.flarebackend.report;

/** Lifecycle of a Report: accepted for grouping, held for review, or dismissed by the duty manager. */
public enum ReportStatus {
	ACCEPTED,
	QUARANTINED,
	DISMISSED
}
