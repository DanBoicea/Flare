package com.fsociety.flarebackend.safety;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

/** Loads safety/emergency-terms.yml once at startup and provides the {@link EmergencyDetector} bean. */
@Configuration
public class EmergencyTerms {

	static final String RESOURCE = "safety/emergency-terms.yml";

	@Bean
	public EmergencyDetector emergencyDetector() {
		return loadDetector();
	}

	/** The detector with the phrases from the classpath file. A missing or empty file stops the app from starting. */
	public static EmergencyDetector loadDetector() {
		try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
			Map<String, List<String>> byLanguage = new Yaml().load(in);
			List<String> phrases = new ArrayList<>();
			for (String language : List.of("ro", "en")) {
				List<String> list = byLanguage == null ? null : byLanguage.get(language);
				if (list == null || list.isEmpty()) {
					throw new IllegalStateException(RESOURCE + " has no '" + language + "' phrases");
				}
				phrases.addAll(list);
			}
			return new EmergencyDetector(phrases);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Cannot read " + RESOURCE, e);
		}
	}
}
