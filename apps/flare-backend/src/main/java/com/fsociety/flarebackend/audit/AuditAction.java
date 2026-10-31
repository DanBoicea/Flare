package com.fsociety.flarebackend.audit;

/** Kinds of audit_event. */
public enum AuditAction {
	ISSUE_CREATED,
	REPORT_ADDED,
	ACKNOWLEDGED,
	TAKEN,
	RESOLVED,
	REOPENED,
	CATEGORY_CHANGED,
	SEVERITY_CHANGED,
	HANDED_OVER,
	REPORT_RELEASED,
	REPORT_DISMISSED
}
