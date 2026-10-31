package com.fsociety.flarebackend.grouping;

import java.util.List;
import java.util.UUID;

import lombok.Value;

import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.issue.IssueStatus;

/**
 * One Issue the new Report might belong to, as the matcher sees it (pipeline.md 4.2): its ID (the only ID the
 * matcher may answer with), category, Location name, current summary, status and up to 3 recent Report texts.
 */
@Value
public class CandidateIssueView {

	UUID id;
	Category category;
	LocalizedText locationName;
	LocalizedText summary;
	IssueStatus status;
	List<String> recentReportTexts;

	@Override
	public String toString() {
		return "CandidateIssueView[id=" + id + ", category=" + category + ", status=" + status + "]";
	}
}
