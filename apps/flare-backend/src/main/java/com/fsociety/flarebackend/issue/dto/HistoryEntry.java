package com.fsociety.flarebackend.issue.dto;

import java.time.Instant;
import java.util.Map;

import com.fsociety.flarebackend.audit.AuditAction;
import lombok.Value;

/** One line of an Issue's history (contract schema {@code HistoryEntry}). Never contains Passenger text. */
@Value
public class HistoryEntry {
	AuditAction action;
	/** Null means the system did it. */
	String staffDisplayName;
	Instant at;
	Map<String, Object> details;
}
