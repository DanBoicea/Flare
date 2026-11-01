package com.fsociety.flarebackend.classification.llm;

import com.fsociety.flarebackend.knowledge.KnowledgeBase;
import com.fsociety.flarebackend.llm.LlmClient;
import com.fsociety.flarebackend.llm.PromptLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * The LLM classifier, only when {@code flare.llm.enabled=true}. It is {@code @Primary}, so intake and the grouping
 * worker get it as their {@code MessageClassifier}, while {@code FallbackClassifier} stays injectable by its own
 * type for the failure path (ClassificationConfig).
 */
@Configuration
@ConditionalOnProperty(prefix = "flare.llm", name = "enabled", havingValue = "true")
public class LlmClassificationConfig {

	@Bean
	public KnowledgeBase knowledgeBase() {
		return new KnowledgeBase();
	}

	@Bean
	public ClassificationValidator classificationValidator() {
		return new ClassificationValidator();
	}

	@Bean
	@Primary
	public LlmMessageClassifier llmMessageClassifier(LlmClient llm, PromptLoader prompts, KnowledgeBase knowledgeBase,
			ClassificationValidator validator) {
		return new LlmMessageClassifier(llm, prompts, knowledgeBase, validator);
	}
}
