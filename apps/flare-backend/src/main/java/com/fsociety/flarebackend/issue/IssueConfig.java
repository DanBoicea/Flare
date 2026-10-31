package com.fsociety.flarebackend.issue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fsociety.flarebackend.config.FlareProperties;

/** Issue beans. {@link PriorityCalculator} is pure, so it is created here with the weights from {@code flare.priority}. */
@Configuration
public class IssueConfig {

	@Bean
	public PriorityCalculator priorityCalculator(FlareProperties properties) {
		return new PriorityCalculator(properties.getPriority());
	}
}
