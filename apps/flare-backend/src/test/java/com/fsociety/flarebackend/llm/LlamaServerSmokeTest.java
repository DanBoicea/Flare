package com.fsociety.flarebackend.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

import com.fsociety.flarebackend.config.FlareProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Calls a real running llama-server (task S04: node scripts/llm/start-llama-server.mjs). Tagged "eval", so it
 * runs only with ./mvnw test -Dgroups=eval. FLARE_LLM_BASE_URL and FLARE_LLM_API_KEY are read from the environment.
 */
@Tag("eval")
class LlamaServerSmokeTest {

	record Kind(String kind) {
	}

	@Test
	void classifiesATrivialMessageWithASchema() {
		String baseUrl = System.getenv().getOrDefault("FLARE_LLM_BASE_URL", "http://127.0.0.1:8081");
		String apiKey = System.getenv().getOrDefault("FLARE_LLM_API_KEY", "");
		FlareProperties.LlmTask task = new FlareProperties.LlmTask("flare-llm", 0.0, 64, Duration.ofSeconds(60));
		LlmClient client = new LlamaServerClient(
				new FlareProperties.Llm(true, baseUrl, apiKey, 10, Map.of("classify", task, "match", task, "summarize", task)),
				new LlmUsageCounter(10, Clock.systemUTC()));

		Kind kind = client.call(LlmTask.CLASSIFY,
				"Classify an airport passenger message. REPORT means a problem staff must fix; QUESTION asks for information.",
				"<passenger_message>Nu mai e hârtie în toaleta de lângă banda 2</passenger_message>",
				"""
				{"type":"object","additionalProperties":false,"required":["kind"],"properties":{"kind":{"enum":["REPORT","QUESTION"]}}}""",
				Kind.class);

		assertThat(kind.kind()).isEqualTo("REPORT");
	}
}
