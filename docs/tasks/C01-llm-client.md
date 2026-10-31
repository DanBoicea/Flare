# C01: `LlmClient` for the self-hosted `llama-server`: config, timeouts, failures, usage cap

Status: DONE
Owner: Florin Cristi
Track: C · Phase 2 · Estimate: 1.5 h · Agent-friendly: no (main session)
Depends on: A01, S04
Unblocks: C02, C03, C04

## Goal
One safe gateway to the self-hosted LLM that every LLM feature uses ([ADR-0022](../adr/0022-self-hosted-llm-llama-cpp.md), [ADR-0016](../adr/0016-llm-model-per-task-in-config.md)).

## Read first
[pipeline section 4](../architecture/pipeline.md#4-llm-calls-track-c-adr-0022-adr-0016) · [backend: config](../architecture/backend.md#configuration-applicationyml-secrets-only-from-env-vars) · [technical: llama.cpp server facts](../technical.md#llamacpp-server-facts-checked-2026-10-31-against-the-llama-server-readme) · **S04's Notes** (the working `curl` request and the `finish_reason` values seen) · the `llama-server` README. **Do not guess request or response field names**; copy them from S04's working request and the README.

## Do
1. **No new dependency.** Use Spring's `RestClient` and the Jackson that Spring Boot already provides. Delete nothing that A-track code uses.
2. **`llm/LlmTask`** enum (`CLASSIFY`, `MATCH`, `SUMMARIZE`). Settings come from `FlareProperties.Llm` (`base-url`, `api-key`, `daily-call-limit`, and per task `model`, `temperature`, `max-tokens`, `timeout`), which A01 already binds.
3. **`llm/LlmClient`** (interface): `<T> T call(LlmTask task, String systemPrompt, String userContent, String jsonSchema, Class<T> type)`. Keep it this small; C02-C04 depend only on it.
4. **`llm/LlamaServerClient`** implements it:
   - one `RestClient` for `flare.llm.base-url`, created once; `Authorization: Bearer <api-key>` only when the key is set
   - `POST /v1/chat/completions` with the task's model alias, `temperature`, `max_tokens`, the system and user messages, and `response_format` with `type: "json_schema"` carrying the schema (exact shape from S04)
   - the system prompt first and byte-for-byte identical across calls of one task, so `llama-server` can reuse the cached prefix
   - a timeout per task (connect and read), and at most one retry on 503 or another 5xx, inside the same timeout
   - anything other than a normal finish (the `finish_reason` S04 recorded for a complete answer), an empty answer, or JSON that doesn't parse into `T` throws `LlmFailure`
   - a connection refused or a 503 "Loading model" throws `LlmFailure` quickly, so the fallback answers the Passenger without waiting for the whole timeout
   - log per call: task, model, latency, prompt and completion token counts if the response has them, and the outcome. **Never prompt or response text**
5. **`llm/LlmUsageCounter`:** a daily call cap (`flare.llm.daily-call-limit`). Over the cap, throw `LlmFailure` without calling. It protects the server from a spam wave; there is no money involved.
6. **`llm/PromptLoader`:** loads `prompts/*.md` from the classpath at startup and fails fast if one is missing.
7. **Wiring:** only when `flare.llm.enabled=true` (`@ConditionalOnProperty`). Otherwise there is no `LlmClient` bean and the fallbacks stay active. Startup must not fail when the server is down; the first call just fails and falls back.
8. **Tests:**
   - unit tests with a fake HTTP server or a mocked `RestClient`: a valid answer, invalid JSON, a cut-off answer, a 503, a timeout, the cap
   - a smoke test tagged `eval` that calls a running `llama-server` with a trivial schema. It runs only on demand.

## Notes
- **Built** in `llm/`: `LlmTask`, `LlmFailure`, `LlmClient`, `LlamaServerClient`, `LlmUsageCounter`, `PromptLoader` and `LlmConfig` (all beans only when `flare.llm.enabled=true`). No new dependency.
- **Deadline:** each task's timeout covers the whole call. The `RestClient` is built per attempt with the time still left, so a retry never runs past the deadline. One retry on any 5xx (including 503 "Loading model"), after 0.5 s, only if at least 2 s remain. 4xx is never retried.
- **Fails fast:** connection refused gives `LlmFailure("llama-server unreachable")` in well under a second (2 s connect timeout), so the fallback answers without waiting 40 s.
- **Only `finish_reason` "stop" counts;** "length" (cut off by `max_tokens`) is a failure, as are an empty answer and JSON that doesn't parse into the requested type. Callers still validate the content (C02-C04).
- **Startup:** with the LLM on, the app logs whether `llama-server` answers `/health`, without blocking or failing startup.
- **For C02-C04:** inject `LlmClient` and `PromptLoader`; call `promptLoader.get("classify.system")` in the constructor so a missing `prompts/*.md` stops startup; mark the bean `@Primary` (the hooks' comments say so). Callers already catch every `RuntimeException`, `LlmFailure` included.
- **Tests:** `LlamaServerClientTest` (10, against the JDK's built-in HTTP server as a fake `llama-server`: valid answer and request shape, API key, `length`, invalid JSON, 503 then 200, two 500s, 401 not retried, timeout, stopped server, daily cap), `anLlmFailureFromTheLlamaServerClientAlsoGivesTheFallback` in `MessageIntakeServiceTest`, and `LlamaServerSmokeTest` (tagged `eval`). Whole suite: 306.

## Done when
- [x] The smoke test passes against S04's `llama-server`. (2026-11-01: Gemma 4 12B classified a Romanian message as REPORT in 1.6 s.)
- [x] The app starts with the LLM disabled, and with it enabled while `llama-server` is stopped. (Also checked with it running: the log says it answered `/health` with 200.)
- [x] A timeout, a stopped server or invalid JSON gives `LlmFailure`, and a unit test with a fake shows the caller falling back.
- [x] Logs show latency and token counts, and no text. (`LLM CLASSIFY ok: model=flare-llm latency=1595ms promptTokens=63 cachedTokens=0 completionTokens=19`)
