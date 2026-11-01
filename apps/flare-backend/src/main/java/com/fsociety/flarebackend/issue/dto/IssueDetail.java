package com.fsociety.flarebackend.issue.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Value;

/**
 * {@code GET /api/staff/issues/{id}} (contract schema {@code IssueDetail}: every {@link IssueCard} field at the
 * top level, plus the Reports and the history).
 */
@Value
public class IssueDetail {
	@JsonUnwrapped
	IssueCard card;
	/** Newest first, at most 50. */
	List<ReportView> reports;
	/** Oldest first. */
	List<HistoryEntry> history;

	@Override
	public String toString() {
		return "IssueDetail[" + card + ", reports=" + reports.size() + "]";
	}
}
