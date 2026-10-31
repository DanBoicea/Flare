package com.fsociety.flarebackend.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;

import com.fsociety.flarebackend.config.FlareProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * LLM beans, only when {@code flare.llm.enabled=true}. Otherwise there is no {@link LlmClient} and the
 * fallbacks stay in charge. The LLM-backed classifier, matcher and summariser (C02-C04) depend on these beans,
 * so they disappear with them.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "flare.llm", name = "enabled", havingValue = "true")
public class LlmConfig {

	@Bean
	public LlmUsageCounter llmUsageCounter(FlareProperties properties, Clock clock) {
		return new LlmUsageCounter(properties.getLlm().getDailyCallLimit(), clock);
	}

	@Bean
	public LlmClient llmClient(FlareProperties properties, LlmUsageCounter usage) {
		return new LlamaServerClient(properties.getLlm(), usage);
	}

	@Bean
	public PromptLoader promptLoader() {
		return new PromptLoader();
	}

	/**
	 * Says in the log whether llama-server answers, without blocking startup: the app must start even when it is
	 * down (every call then fails fast and the fallbacks answer).
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void checkLlamaServer(ApplicationReadyEvent event) {
		String baseUrl = event.getApplicationContext().getBean(FlareProperties.class).getLlm().getBaseUrl();
		try {
			HttpResponse<String> health = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
					.send(HttpRequest.newBuilder(URI.create(baseUrl + "/health")).timeout(Duration.ofSeconds(2)).build(),
							HttpResponse.BodyHandlers.ofString());
			log.info("LLM enabled; llama-server at {} answered /health with {}", baseUrl, health.statusCode());
		}
		catch (Exception e) {
			log.warn("LLM enabled, but llama-server at {} is not reachable ({}); the fallbacks answer until it is",
					baseUrl, e.getClass().getSimpleName());
		}
	}
}
