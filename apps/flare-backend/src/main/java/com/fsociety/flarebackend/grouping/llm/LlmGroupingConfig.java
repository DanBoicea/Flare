package com.fsociety.flarebackend.grouping.llm;

import com.fsociety.flarebackend.llm.LlmClient;
import com.fsociety.flarebackend.llm.PromptLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * The LLM matcher, only when {@code flare.llm.enabled=true}. It is {@code @Primary}, so the grouping worker gets it,
 * while {@code NoMatchIssueMatcher} stays registered as the fallback (GroupingConfig).
 */
@Configuration
@ConditionalOnProperty(prefix = "flare.llm", name = "enabled", havingValue = "true")
public class LlmGroupingConfig {

	@Bean
	@Primary
	public LlmIssueMatcher llmIssueMatcher(LlmClient llm, PromptLoader prompts) {
		return new LlmIssueMatcher(llm, prompts);
	}
}
