package com.fsociety.flarebackend.grouping.llm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.grouping.CandidateIssueView;
import com.fsociety.flarebackend.grouping.IssueMatcher;
import com.fsociety.flarebackend.grouping.NewReportView;
import com.fsociety.flarebackend.llm.LlmClient;
import com.fsociety.flarebackend.llm.LlmTask;
import com.fsociety.flarebackend.llm.PromptLoader;
import com.fsociety.flarebackend.llm.PromptText;
import lombok.extern.slf4j.Slf4j;

/**
 * Asks the self-hosted LLM whether a new Report is the same real-world problem as one of the candidate Issues
 * (ADR-0007, pipeline.md 4.2). Candidates are shown as C1, C2, ... and the schema of each call only allows those
 * labels or null, so the model cannot invent an ID; an unknown label still throws, and the worker then creates a
 * new Issue. Sees only the view classes: texts, categories, places, summaries, never contact details or hashes.
 */
@Slf4j
public class LlmIssueMatcher implements IssueMatcher {

	static final String PROMPT = "match.system";
	static final int REASON_MAX = 120;

	private final LlmClient llm;
	private final String systemPrompt;

	public LlmIssueMatcher(LlmClient llm, PromptLoader prompts) {
		this.llm = llm;
		this.systemPrompt = prompts.get(PROMPT);
	}

	@Override
	public Optional<UUID> findMatch(NewReportView report, List<CandidateIssueView> candidates) {
		if (candidates.isEmpty()) {
			return Optional.empty();
		}
		Map<String, UUID> idsByLabel = new LinkedHashMap<>();
		for (int i = 0; i < candidates.size(); i++) {
			idsByLabel.put("C" + (i + 1), candidates.get(i).getId());
		}
		MatchAnswer answer = llm.call(LlmTask.MATCH, systemPrompt, userTurn(report, candidates), schema(idsByLabel.keySet()),
				MatchAnswer.class);
		String reason = answer.getReason() == null ? "" : answer.getReason();
		log.info("Matcher answered {} of {} candidates (reason {} characters)",
				answer.getMatchIssueId() == null ? "no match" : answer.getMatchIssueId(), candidates.size(), reason.length());
		if (answer.getMatchIssueId() == null) {
			return Optional.empty();
		}
		UUID id = idsByLabel.get(answer.getMatchIssueId());
		if (id == null) {
			throw new IllegalStateException("Matcher answered " + answer.getMatchIssueId() + ", which is not a candidate");
		}
		return Optional.of(id);
	}

	static String userTurn(NewReportView report, List<CandidateIssueView> candidates) {
		StringBuilder turn = new StringBuilder("<new_report>\n")
				.append("category: ").append(report.getCategory()).append('\n')
				.append("place: ").append(place(report.getLocationName())).append('\n')
				.append("text: ").append(PromptText.escapeTags(report.getText())).append('\n')
				.append("</new_report>\n<candidate_issues>\n");
		for (int i = 0; i < candidates.size(); i++) {
			CandidateIssueView c = candidates.get(i);
			turn.append("<issue id=\"C").append(i + 1).append("\">\n")
					.append("category: ").append(c.getCategory()).append('\n')
					.append("place: ").append(place(c.getLocationName())).append('\n')
					.append("status: ").append(c.getStatus()).append('\n')
					.append("summary: ").append(summary(c.getSummary())).append('\n')
					.append("recent reports:\n");
			for (String text : c.getRecentReportTexts()) {
				turn.append("- ").append(PromptText.escapeTags(text)).append('\n');
			}
			turn.append("</issue>\n");
		}
		return turn.append("</candidate_issues>").toString();
	}

	/** The answer may only be one of this call's labels, or null. */
	static String schema(Iterable<String> labels) {
		String allowed = StreamSupport.stream(labels.spliterator(), false)
				.map(label -> "\"" + label + "\"")
				.collect(Collectors.joining(","));
		return """
				{"type":"object","additionalProperties":false,"required":["matchIssueId","reason"],"properties":{
				  "matchIssueId":{"anyOf":[{"enum":[%s]},{"type":"null"}]},
				  "reason":{"type":"string","maxLength":%d}}}""".formatted(allowed, REASON_MAX);
	}

	private static String place(LocalizedText name) {
		return name == null ? "unknown" : name.getRo() + " / " + name.getEn();
	}

	private static String summary(LocalizedText summary) {
		return summary == null ? "" : PromptText.escapeTags(summary.getEn() + " / " + summary.getRo());
	}
}
