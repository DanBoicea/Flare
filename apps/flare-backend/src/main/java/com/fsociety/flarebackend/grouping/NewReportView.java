package com.fsociety.flarebackend.grouping;

import lombok.Value;

import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.LocalizedText;

/**
 * The new Report as the matcher sees it (backend.md, "View classes"): text, category and Location name only, never
 * contact details, hashes or IDs. Holds Passenger text, so toString is replaced.
 */
@Value
public class NewReportView {

	String text;
	Category category;
	/** Null when the Report has no Location. */
	LocalizedText locationName;

	@Override
	public String toString() {
		return "NewReportView[category=" + category + "]";
	}
}
