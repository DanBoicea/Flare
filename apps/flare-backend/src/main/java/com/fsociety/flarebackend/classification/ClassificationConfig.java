package com.fsociety.flarebackend.classification;

import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.safety.EmergencyDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Classification beans. {@link FallbackClassifier} is the only {@link MessageClassifier} until Track C adds
 * the Claude one, which must be marked {@code @Primary} so callers get it while the fallback stays
 * injectable by its own type for the failure path.
 */
@Configuration
public class ClassificationConfig {

	@Bean
	public FallbackClassifier fallbackClassifier(EmergencyDetector emergencyDetector) {
		return new FallbackClassifier(emergencyDetector);
	}

	@Bean
	public CategoryTeamRouting categoryTeamRouting(FlareProperties properties) {
		FlareProperties.Routing routing = properties.getRouting();
		return new CategoryTeamRouting(routing.getCategoryTeam(), routing.getLowConfidenceThreshold());
	}
}
