package com.fsociety.flarebackend.grouping.llm;

import com.fsociety.flarebackend.issue.summary.llm.LlmIssueSummarizer;
import com.fsociety.flarebackend.llm.LlmClient;
import com.fsociety.flarebackend.llm.PromptLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * The LLM matcher and summariser, only when {@code flare.llm.enabled=true}. Both are {@code @Primary}, so the grouping
 * worker gets them, while {@code NoMatchIssueMatcher} and {@code FallbackIssueSummarizer} stay registered as the
 * fallbacks (GroupingConfig).
 */
@Configuration
@ConditionalOnProperty(prefix = "flare.llm", name = "enabled", havingValue = "true")
public class LlmGroupingConfig {

	@Bean
	@Primary
	public LlmIssueMatcher llmIssueMatcher(LlmClient llm, PromptLoader prompts) {
		return new LlmIssueMatcher(llm, prompts);
	}

	@Bean
	@Primary
	public LlmIssueSummarizer llmIssueSummarizer(LlmClient llm, PromptLoader prompts) {
		return new LlmIssueSummarizer(llm, prompts);
	}
}
