package com.fsociety.flarebackend.issue.summary;

import lombok.Value;

import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.LocalizedText;

/**
 * The Issue as the summariser sees it (pipeline.md 4.3): category, Location name and the current summary. Named
 * IssueSummaryView because {@code issue.IssueView} is the ACTIVE/RESOLVED list choice.
 */
@Value
public class IssueSummaryView {

	Category category;
	/** Null when the Issue has no Location. */
	LocalizedText locationName;
	LocalizedText currentSummary;

	@Override
	public String toString() {
		return "IssueSummaryView[category=" + category + "]";
	}
}
