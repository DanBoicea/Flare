package com.fsociety.flarebackend.issue;

/** Issue state machine (docs/architecture/data-model.md). */
public enum IssueStatus {
	NEW,
	ACKNOWLEDGED,
	IN_PROGRESS,
	RESOLVED
}
