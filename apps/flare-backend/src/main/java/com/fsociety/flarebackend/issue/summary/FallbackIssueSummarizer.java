package com.fsociety.flarebackend.issue.summary;

import java.util.List;

import com.fsociety.flarebackend.common.LocalizedText;

/** Used while the LLM is off or failing: keeps the current summary (backend.md "Fallbacks"). */
public class FallbackIssueSummarizer implements IssueSummarizer {

	@Override
	public LocalizedText summarize(IssueSummaryView issue, List<ReportTextView> newestReports) {
		return issue.getCurrentSummary();
	}
}
