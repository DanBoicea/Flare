package com.fsociety.flarebackend.issue.summary;

import java.util.List;

import com.fsociety.flarebackend.common.LocalizedText;

/**
 * Rewrites an Issue's summary when a Report joins it (pipeline.md 4.3, ADR-0007). The fallback is
 * {@link FallbackIssueSummarizer}; Track C adds the LLM implementation, which must be marked {@code @Primary}. The
 * caller catches every exception and keeps the old summary, so an implementation may just throw.
 */
public interface IssueSummarizer {

	/** {@code newestReports}: up to 20, newest first. */
	LocalizedText summarize(IssueSummaryView issue, List<ReportTextView> newestReports);
}
