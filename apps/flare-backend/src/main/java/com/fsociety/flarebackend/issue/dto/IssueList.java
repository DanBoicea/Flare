package com.fsociety.flarebackend.issue.dto;

import java.time.Instant;
import java.util.List;

import lombok.Value;

/** {@code GET /api/staff/issues} (contract schema {@code IssueList}). The app shows ages using {@code serverTime}. */
@Value
public class IssueList {
	Instant serverTime;
	List<IssueCard> issues;
}
