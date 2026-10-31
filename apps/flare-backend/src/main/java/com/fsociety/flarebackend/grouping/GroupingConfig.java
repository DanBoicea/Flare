package com.fsociety.flarebackend.grouping;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.issue.summary.FallbackIssueSummarizer;
import com.fsociety.flarebackend.issue.summary.IssueSummarizer;

/**
 * Grouping beans. The fallbacks are the only matcher and summariser until Track C adds the LLM ones, which must be
 * marked {@code @Primary} so the worker gets them.
 */
@Configuration
public class GroupingConfig {

	@Bean
	public CandidateSelector candidateSelector(FlareProperties properties) {
		FlareProperties.Grouping grouping = properties.getGrouping();
		return new CandidateSelector(grouping.getWindow(), grouping.getMaxCandidates());
	}

	@Bean
	public IssueMatcher noMatchIssueMatcher() {
		return new NoMatchIssueMatcher();
	}

	@Bean
	public IssueSummarizer fallbackIssueSummarizer() {
		return new FallbackIssueSummarizer();
	}
}
