package com.fsociety.flarebackend.issue.dto;

import java.time.Instant;
import java.util.UUID;

import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.issue.IssueStatus;
import com.fsociety.flarebackend.staff.TeamId;
import lombok.Value;

/**
 * One row of the Staff app's list (contract schema {@code IssueCard}). Every field is required; {@code team},
 * {@code location}, {@code mentionedPlace} and {@code takenBy} are sent as null when empty, never left out.
 * The summary holds Passenger-derived text, so toString is replaced to keep it out of logs.
 */
@Value
public class IssueCard {
	UUID id;
	long version;
	IssueStatus status;
	Severity severity;
	Category category;
	TeamId team;
	LocationRef location;
	String mentionedPlace;
	LocalizedText summary;
	ClassificationSource summarySource;
	int reportCount;
	int reporterCount;
	Instant firstReportedAt;
	Instant lastReportedAt;
	StaffRef takenBy;
	boolean emergency;
	boolean unclassified;
	boolean reopened;
	Priority priority;

	@Override
	public String toString() {
		return "IssueCard[id=" + id + ", status=" + status + ", severity=" + severity + "]";
	}
}
