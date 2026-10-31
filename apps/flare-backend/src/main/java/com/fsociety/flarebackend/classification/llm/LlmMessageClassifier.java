package com.fsociety.flarebackend.classification.llm;

import com.fsociety.flarebackend.classification.Classification;
import com.fsociety.flarebackend.classification.ClassificationInput;
import com.fsociety.flarebackend.classification.LocationInfo;
import com.fsociety.flarebackend.classification.MessageClassifier;
import com.fsociety.flarebackend.knowledge.KnowledgeBase;
import com.fsociety.flarebackend.llm.LlmClient;
import com.fsociety.flarebackend.llm.LlmTask;
import com.fsociety.flarebackend.llm.PromptLoader;

/**
 * Classifies a Passenger message with the self-hosted LLM in one call, answering Questions from the Knowledge
 * base (ADR-0014, pipeline.md section 4.1). Any failure throws; intake and the grouping worker then use the
 * fallback. The Passenger's text only ever goes in the user turn, inside tags (security-and-abuse.md section 4).
 */
public class LlmMessageClassifier implements MessageClassifier {

	static final String PROMPT = "classify.system";
	static final String KNOWLEDGE_BASE_PLACEHOLDER = "{{KNOWLEDGE_BASE}}";

	private final LlmClient llm;
	private final ClassificationValidator validator;
	/** Built once and identical for every call, so llama-server can reuse the cached prompt. */
	private final String systemPrompt;

	public LlmMessageClassifier(LlmClient llm, PromptLoader prompts, KnowledgeBase knowledgeBase, ClassificationValidator validator) {
		this.llm = llm;
		this.validator = validator;
		String template = prompts.get(PROMPT);
		if (!template.contains(KNOWLEDGE_BASE_PLACEHOLDER)) {
			throw new IllegalStateException("prompts/" + PROMPT + ".md must contain " + KNOWLEDGE_BASE_PLACEHOLDER);
		}
		this.systemPrompt = template.replace(KNOWLEDGE_BASE_PLACEHOLDER, knowledgeBase.text());
	}

	@Override
	public Classification classify(ClassificationInput input) {
		ClassificationAnswer answer = llm.call(LlmTask.CLASSIFY, systemPrompt, userTurn(input), ClassificationSchema.JSON,
				ClassificationAnswer.class);
		return validator.validate(answer, input);
	}

	static String userTurn(ClassificationInput input) {
		String location = input.getScannedLocation()
				.map(LlmMessageClassifier::describe)
				.orElse("unknown");
		return "<scanned_location>" + location + "</scanned_location>\n"
				+ "<ui_language>" + input.getUiLanguage().getCode() + "</ui_language>\n"
				+ "<passenger_insisted>" + input.isPassengerInsisted() + "</passenger_insisted>\n"
				+ "<passenger_message>" + escapeTags(input.getText()) + "</passenger_message>";
	}

	private static String describe(LocationInfo location) {
		return location.getNameRo() + " / " + location.getNameEn();
	}

	/** Stops a Passenger from closing the tag and writing fake instructions after it. */
	static String escapeTags(String text) {
		return text.replace("<", "‹").replace(">", "›");
	}
}
