package com.fsociety.flarebackend.llm;

import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import com.fsociety.flarebackend.config.FlareProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * {@link LlmClient} for the self-hosted llama.cpp server (ADR-0022): {@code POST /v1/chat/completions} with
 * {@code response_format} {@code json_schema}. Field names follow the llama-server README and the request
 * recorded in S04. Each task's timeout covers the whole call, including at most one retry on a 5xx.
 * Logs latency and token counts, never prompt or answer text.
 */
@Slf4j
public class LlamaServerClient implements LlmClient {

	private static final String PATH = "/v1/chat/completions";
	/** llama-server runs on the same machine or LAN; anything slower means it is down. */
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
	private static final Duration RETRY_PAUSE = Duration.ofMillis(500);
	/** Only retry when there is still a realistic chance of an answer before the deadline. */
	private static final Duration MIN_TIME_FOR_RETRY = Duration.ofSeconds(2);
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final FlareProperties.Llm settings;
	private final LlmUsageCounter usage;
	private final HttpClient http;

	public LlamaServerClient(FlareProperties.Llm settings, LlmUsageCounter usage) {
		this.settings = settings;
		this.usage = usage;
		this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
	}

	@Override
	public <T> T call(LlmTask task, String systemPrompt, String userContent, String jsonSchema, Class<T> type) {
		FlareProperties.LlmTask config = settings.getTasks().get(task.getConfigKey());
		if (config == null) {
			throw new IllegalStateException("No settings for flare.llm.tasks." + task.getConfigKey());
		}
		long started = System.nanoTime();
		try {
			usage.acquire();
			String body = requestBody(task, config, systemPrompt, userContent, jsonSchema);
			JsonNode response = postWithOneRetry(body, started + config.getTimeout().toNanos());
			T result = parseAnswer(response, type);
			log.info("LLM {} ok: model={} latency={}ms promptTokens={} cachedTokens={} completionTokens={}",
					task, config.getModel(), millisSince(started),
					response.path("usage").path("prompt_tokens").asInt(-1),
					response.path("timings").path("cache_n").asInt(-1),
					response.path("usage").path("completion_tokens").asInt(-1));
			return result;
		}
		catch (LlmFailure e) {
			log.warn("LLM {} failed after {}ms: {}", task, millisSince(started), e.getMessage());
			throw e;
		}
	}

	private static String requestBody(LlmTask task, FlareProperties.LlmTask config, String systemPrompt,
			String userContent, String jsonSchema) {
		ObjectNode root = JSON.createObjectNode();
		root.put("model", config.getModel());
		root.put("temperature", config.getTemperature());
		root.put("max_tokens", config.getMaxTokens());
		ArrayNode messages = root.putArray("messages");
		messages.addObject().put("role", "system").put("content", systemPrompt);
		messages.addObject().put("role", "user").put("content", userContent);
		ObjectNode schema = root.putObject("response_format").put("type", "json_schema").putObject("json_schema");
		schema.put("name", task.getConfigKey());
		schema.put("strict", true);
		schema.set("schema", JSON.readTree(jsonSchema));
		return JSON.writeValueAsString(root);
	}

	private JsonNode postWithOneRetry(String body, long deadlineNanos) {
		try {
			return post(body, remaining(deadlineNanos));
		}
		catch (HttpServerErrorException e) {
			// 503 while the model loads, or another server error: one more try if there is time.
			if (remaining(deadlineNanos).compareTo(RETRY_PAUSE.plus(MIN_TIME_FOR_RETRY)) < 0) {
				throw new LlmFailure("HTTP " + e.getStatusCode().value() + ", no time left to retry", e);
			}
			sleep(RETRY_PAUSE);
			try {
				return post(body, remaining(deadlineNanos));
			}
			catch (HttpServerErrorException again) {
				throw new LlmFailure("HTTP " + again.getStatusCode().value() + " twice", again);
			}
		}
	}

	private JsonNode post(String body, Duration timeout) {
		if (timeout.isNegative() || timeout.isZero()) {
			throw new LlmFailure("timeout");
		}
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(http);
		requestFactory.setReadTimeout(timeout);
		RestClient client = RestClient.builder().requestFactory(requestFactory).baseUrl(settings.getBaseUrl()).build();
		try {
			String response = client.post()
					.uri(PATH)
					.contentType(MediaType.APPLICATION_JSON)
					.headers(headers -> {
						if (settings.getApiKey() != null && !settings.getApiKey().isBlank()) {
							headers.setBearerAuth(settings.getApiKey());
						}
					})
					.body(body)
					.retrieve()
					.body(String.class);
			return JSON.readTree(response == null ? "" : response);
		}
		catch (HttpServerErrorException e) {
			throw e;
		}
		catch (HttpClientErrorException e) {
			throw new LlmFailure("HTTP " + e.getStatusCode().value(), e);
		}
		catch (ResourceAccessException e) {
			throw new LlmFailure(isTimeout(e) ? "timeout" : connectionProblem(e), e);
		}
		catch (RestClientException | JacksonException e) {
			throw new LlmFailure("unreadable response", e);
		}
	}

	/** Only a complete answer ("stop") counts; "length" means max_tokens cut it off (S04). */
	private static <T> T parseAnswer(JsonNode response, Class<T> type) {
		JsonNode choice = response.path("choices").path(0);
		String finishReason = choice.path("finish_reason").asString("");
		if (!"stop".equals(finishReason)) {
			throw new LlmFailure("finish_reason=" + (finishReason.isEmpty() ? "missing" : finishReason));
		}
		String content = choice.path("message").path("content").asString("");
		if (content.isBlank()) {
			throw new LlmFailure("empty answer");
		}
		try {
			return JSON.readValue(content, type);
		}
		catch (JacksonException e) {
			throw new LlmFailure("answer does not match " + type.getSimpleName(), e);
		}
	}

	private static boolean isTimeout(Throwable e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof HttpTimeoutException || cause instanceof java.net.SocketTimeoutException) {
				return true;
			}
		}
		return false;
	}

	private static String connectionProblem(Throwable e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConnectException) {
				return "llama-server unreachable";
			}
		}
		return "connection problem";
	}

	private static Duration remaining(long deadlineNanos) {
		return Duration.ofNanos(deadlineNanos - System.nanoTime());
	}

	private static long millisSince(long startedNanos) {
		return (System.nanoTime() - startedNanos) / 1_000_000;
	}

	private static void sleep(Duration pause) {
		try {
			Thread.sleep(pause);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LlmFailure("interrupted");
		}
	}
}
