package com.fsociety.flarebackend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.ClassPathResource;

/** Binds the real application.yml without starting Spring, so a typo in a key fails the build. */
class FlarePropertiesTest {

	@Test
	void bindsTheFlareBlockFromApplicationYml() throws IOException {
		FlareProperties flare = bind(Map.of("FLARE_HASH_SECRET", "test-secret"));

		assertThat(flare.getLlm().isEnabled()).isFalse();
		assertThat(flare.getLlm().getTasks()).containsOnlyKeys("classify", "match", "summarize");
		assertThat(flare.getLlm().getBaseUrl()).isEqualTo("http://localhost:8081");
		assertThat(flare.getLlm().getApiKey()).isEmpty();
		assertThat(flare.getLlm().getTasks().get("classify").getModel()).isEqualTo("flare-llm");
		assertThat(flare.getLlm().getTasks().get("classify").getTemperature()).isZero();
		assertThat(flare.getLlm().getTasks().get("classify").getTimeout()).isEqualTo(Duration.ofSeconds(40));
		assertThat(flare.getLimits().getDevice().getPer10Min()).isEqualTo(3);
		assertThat(flare.getLimits().getQrCode().getStrictModeDuration()).isEqualTo(Duration.ofMinutes(30));
		assertThat(flare.getLimits().getBotCheck().getMinElapsedMs()).isEqualTo(2000);
		assertThat(flare.getRouting().getCategoryTeam())
				.containsEntry("CLEANING_OR_SPILL", "CLEANING")
				.containsEntry("SAFETY_OR_SECURITY", "SECURITY")
				.doesNotContainKey("OTHER");
		assertThat(flare.getPriority().getSeverityWeight()).containsEntry("CRITICAL", 10000);
		assertThat(flare.getRetention().getDismissedDays()).isEqualTo(7);
		assertThat(flare.getQuestions().getSendToStaffWindow()).isEqualTo(Duration.ofMinutes(30));
		assertThat(flare.getCors().getAllowedOrigins()).containsExactly("http://localhost:5173");
		assertThat(flare.getSecurity().getHashSecret()).isEqualTo("test-secret");
	}

	@Test
	void splitsAllowedOriginsOnCommas() throws IOException {
		FlareProperties flare = bind(Map.of(
				"FLARE_HASH_SECRET", "test-secret",
				"FLARE_ALLOWED_ORIGINS", "http://localhost:5173,https://flare.example.invalid"));

		assertThat(flare.getCors().getAllowedOrigins())
				.containsExactly("http://localhost:5173", "https://flare.example.invalid");
	}

	private static FlareProperties bind(Map<String, Object> environment) throws IOException {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new MapPropertySource("environment", environment));
		new YamlPropertySourceLoader()
				.load("application.yml", new ClassPathResource("application.yml"))
				.forEach(sources::addLast);
		Binder binder = new Binder(ConfigurationPropertySources.from(sources),
				new PropertySourcesPlaceholdersResolver(sources));
		return binder.bind("flare", FlareProperties.class).get();
	}
}
