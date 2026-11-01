package com.fsociety.flarebackend.issue.summary.llm;

import java.util.List;

import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.issue.summary.IssueSummarizer;
import com.fsociety.flarebackend.issue.summary.IssueSummaryView;
import com.fsociety.flarebackend.issue.summary.ReportTextView;
import com.fsociety.flarebackend.llm.LlmClient;
import com.fsociety.flarebackend.llm.LlmTask;
import com.fsociety.flarebackend.llm.PromptLoader;
import com.fsociety.flarebackend.llm.PromptText;

/**
 * Rewrites an Issue's summary from its newest Reports with the self-hosted LLM (ADR-0007, pipeline.md 4.3). An empty
 * or over-long line throws, and the grouping worker then keeps the old summary. Sees only the view classes.
 */
public class LlmIssueSummarizer implements IssueSummarizer {

	static final String PROMPT = "summarize.system";
	static final int SUMMARY_MAX = 160;
	static final int REPORTS_MAX = 20;
	static final String SCHEMA = """
			{"type":"object","additionalProperties":false,"required":["summary"],"properties":{
			  "summary":{"type":"object","additionalProperties":false,"required":["ro","en"],"properties":{
			    "ro":{"type":"string","minLength":1,"maxLength":160},
			    "en":{"type":"string","minLength":1,"maxLength":160}}}}}""";

	private final LlmClient llm;
	private final String systemPrompt;

	public LlmIssueSummarizer(LlmClient llm, PromptLoader prompts) {
		this.llm = llm;
		this.systemPrompt = prompts.get(PROMPT);
	}

	@Override
	public LocalizedText summarize(IssueSummaryView issue, List<ReportTextView> newestReports) {
		SummaryAnswer answer = llm.call(LlmTask.SUMMARIZE, systemPrompt, userTurn(issue, newestReports), SCHEMA, SummaryAnswer.class);
		if (answer.getSummary() == null) {
			throw new IllegalStateException("summary is missing");
		}
		return new LocalizedText(checked(answer.getSummary().getRo(), "ro"), checked(answer.getSummary().getEn(), "en"));
	}

	static String userTurn(IssueSummaryView issue, List<ReportTextView> newestReports) {
		StringBuilder turn = new StringBuilder("<issue>\n")
				.append("category: ").append(issue.getCategory()).append('\n')
				.append("place: ").append(issue.getLocationName() == null ? "unknown"
						: issue.getLocationName().getRo() + " / " + issue.getLocationName().getEn()).append('\n')
				.append("current summary: ").append(issue.getCurrentSummary() == null ? ""
						: PromptText.escapeTags(issue.getCurrentSummary().getRo() + " / " + issue.getCurrentSummary().getEn()))
				.append("\n</issue>\n<reports>\n");
		newestReports.stream().limit(REPORTS_MAX)
				.forEach(report -> turn.append("- ").append(PromptText.escapeTags(report.getText())).append('\n'));
		return turn.append("</reports>").toString();
	}

	private static String checked(String line, String language) {
		if (line == null || line.isBlank()) {
			throw new IllegalStateException("summary." + language + " is empty");
		}
		String stripped = line.strip();
		if (stripped.codePointCount(0, stripped.length()) > SUMMARY_MAX) {
			throw new IllegalStateException("summary." + language + " is longer than " + SUMMARY_MAX + " characters");
		}
		return stripped;
	}
}
