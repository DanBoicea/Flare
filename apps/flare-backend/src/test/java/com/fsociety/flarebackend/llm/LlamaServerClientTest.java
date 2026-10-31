package com.fsociety.flarebackend.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fsociety.flarebackend.config.FlareProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** {@link LlamaServerClient} against a fake llama-server (the JDK's built-in HTTP server). */
class LlamaServerClientTest {

	private static final String SCHEMA = """
			{"type":"object","additionalProperties":false,"required":["kind"],"properties":{"kind":{"enum":["REPORT","QUESTION"]}}}""";
	private static final JsonMapper JSON = JsonMapper.builder().build();

	record Kind(String kind) {
	}

	/** One scripted answer of the fake server. */
	record Reply(int status, String body, Duration delay) {
		static Reply ok(String content) {
			return ok(content, "stop");
		}

		static Reply ok(String content, String finishReason) {
			String body = """
					{"choices":[{"finish_reason":"%s","message":{"role":"assistant","content":%s}}],
					 "usage":{"prompt_tokens":3777,"completion_tokens":12},"timings":{"cache_n":3700}}"""
					.formatted(finishReason, JSON.writeValueAsString(content));
			return new Reply(200, body, Duration.ZERO);
		}

		static Reply status(int status) {
			return new Reply(status, "{\"error\":{\"message\":\"Loading model\"}}", Duration.ZERO);
		}

		static Reply slow(Duration delay) {
			return new Reply(200, ok("{\"kind\":\"REPORT\"}").body(), delay);
		}
	}

	private final Deque<Reply> replies = new ArrayDeque<>();
	private final List<String> requestBodies = new CopyOnWriteArrayList<>();
	private final List<String> authHeaders = new CopyOnWriteArrayList<>();
	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void returnsTheParsedAnswerAndSendsTheSchema() {
		replies.add(Reply.ok("{\"kind\":\"REPORT\"}"));

		Kind kind = client(startServer(), "", Duration.ofSeconds(5), 10).call(LlmTask.CLASSIFY, "system", "user", SCHEMA, Kind.class);

		assertThat(kind.kind()).isEqualTo("REPORT");
		JsonNode sent = JSON.readTree(requestBodies.get(0));
		assertThat(sent.path("model").asString()).isEqualTo("flare-llm");
		assertThat(sent.path("temperature").asDouble()).isZero();
		assertThat(sent.path("max_tokens").asInt()).isEqualTo(256);
		assertThat(sent.path("messages").path(0).path("role").asString()).isEqualTo("system");
		assertThat(sent.path("messages").path(1).path("content").asString()).isEqualTo("user");
		assertThat(sent.path("response_format").path("type").asString()).isEqualTo("json_schema");
		assertThat(sent.path("response_format").path("json_schema").path("schema").path("required").path(0).asString()).isEqualTo("kind");
		assertThat(authHeaders.get(0)).isEmpty();
	}

	@Test
	void sendsTheApiKeyWhenSet() {
		replies.add(Reply.ok("{\"kind\":\"QUESTION\"}"));

		client(startServer(), "secret-key", Duration.ofSeconds(5), 10).call(LlmTask.CLASSIFY, "s", "u", SCHEMA, Kind.class);

		assertThat(authHeaders.get(0)).isEqualTo("Bearer secret-key");
	}

	@Test
	void anAnswerCutOffByTheTokenLimitFails() {
		replies.add(Reply.ok("{\"kind\":\"REP", "length"));

		assertThatThrownBy(() -> client(startServer(), "", Duration.ofSeconds(5), 10).call(LlmTask.CLASSIFY, "s", "u", SCHEMA, Kind.class))
				.isInstanceOf(LlmFailure.class).hasMessage("finish_reason=length");
	}

	@Test
	void anAnswerThatIsNotTheExpectedJsonFails() {
		replies.add(Reply.ok("not json at all"));

		assertThatThrownBy(() -> client(startServer(), "", Duration.ofSeconds(5), 10).call(LlmTask.CLASSIFY, "s", "u", SCHEMA, Kind.class))
				.isInstanceOf(LlmFailure.class).hasMessage("answer does not match Kind");
	}

	@Test
	void retriesOnceWhenTheServerIsStillLoading() {
		replies.add(Reply.status(503));
		replies.add(Reply.ok("{\"kind\":\"REPORT\"}"));

		Kind kind = client(startServer(), "", Duration.ofSeconds(5), 10).call(LlmTask.CLASSIFY, "s", "u", SCHEMA, Kind.class);

		assertThat(kind.kind()).isEqualTo("REPORT");
		assertThat(requestBodies).hasSize(2);
	}

	@Test
	void twoServerErrorsFail() {
		replies.add(Reply.status(500));
		replies.add(Reply.status(500));

		assertThatThrownBy(() -> client(startServer(), "", Duration.ofSeconds(5), 10).call(LlmTask.CLASSIFY, "s", "u", SCHEMA, Kind.class))
				.isInstanceOf(LlmFailure.class).hasMessage("HTTP 500 twice");
		assertThat(requestBodies).hasSize(2);
	}

	@Test
	void aClientErrorIsNotRetried() {
		replies.add(Reply.status(401));

		assertThatThrownBy(() -> client(startServer(), "", Duration.ofSeconds(5), 10).call(LlmTask.CLASSIFY, "s", "u", SCHEMA, Kind.class))
				.isInstanceOf(LlmFailure.class).hasMessage("HTTP 401");
		assertThat(requestBodies).hasSize(1);
	}

	@Test
	void aSlowAnswerTimesOutWithinTheTasksTimeout() {
		replies.add(Reply.slow(Duration.ofSeconds(3)));

		long started = System.nanoTime();
		assertThatThrownBy(() -> client(startServer(), "", Duration.ofMillis(800), 10).call(LlmTask.CLASSIFY, "s", "u", SCHEMA, Kind.class))
				.isInstanceOf(LlmFailure.class).hasMessage("timeout");
		assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(2));
	}

	@Test
	void aStoppedServerFailsFast() throws IOException {
		int closedPort;
		try (ServerSocket socket = new ServerSocket(0)) {
			closedPort = socket.getLocalPort();
		}

		long started = System.nanoTime();
		assertThatThrownBy(() -> client("http://127.0.0.1:" + closedPort, "", Duration.ofSeconds(40), 10)
				.call(LlmTask.CLASSIFY, "s", "u", SCHEMA, Kind.class))
				.isInstanceOf(LlmFailure.class).hasMessage("llama-server unreachable");
		assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(3));
	}

	@Test
	void overTheDailyCapNothingIsSent() {
		replies.add(Reply.ok("{\"kind\":\"REPORT\"}"));
		LlmClient client = client(startServer(), "", Duration.ofSeconds(5), 1);
		client.call(LlmTask.CLASSIFY, "s", "u", SCHEMA, Kind.class);

		assertThatThrownBy(() -> client.call(LlmTask.CLASSIFY, "s", "u", SCHEMA, Kind.class))
				.isInstanceOf(LlmFailure.class).hasMessageContaining("daily LLM call limit");
		assertThat(requestBodies).hasSize(1);
	}

	private String startServer() {
		try {
			server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
		server.createContext("/v1/chat/completions", exchange -> {
			requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			String auth = exchange.getRequestHeaders().getFirst("Authorization");
			authHeaders.add(auth == null ? "" : auth);
			Reply reply = replies.isEmpty() ? Reply.status(500) : replies.poll();
			try {
				Thread.sleep(reply.delay());
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			try {
				exchange.sendResponseHeaders(reply.status(), bytes.length);
				try (OutputStream out = exchange.getResponseBody()) {
					out.write(bytes);
				}
			}
			catch (IOException e) {
				// The client gave up (timeout test); nothing to do.
			}
		});
		server.start();
		return "http://127.0.0.1:" + server.getAddress().getPort();
	}

	private static LlmClient client(String baseUrl, String apiKey, Duration timeout, int dailyLimit) {
		FlareProperties.LlmTask task = new FlareProperties.LlmTask("flare-llm", 0.0, 256, timeout);
		FlareProperties.Llm settings = new FlareProperties.Llm(true, baseUrl, apiKey, dailyLimit,
				Map.of("classify", task, "match", task, "summarize", task));
		return new LlamaServerClient(settings, new LlmUsageCounter(dailyLimit, Clock.systemUTC()));
	}
}
