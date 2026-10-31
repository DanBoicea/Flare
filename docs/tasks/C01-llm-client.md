# C01: `LlmClient` for the self-hosted `llama-server`: config, timeouts, failures, usage cap

Status: TODO
Owner:
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

## Done when
- [ ] The smoke test passes against S04's `llama-server`.
- [ ] The app starts with the LLM disabled, and with it enabled while `llama-server` is stopped.
- [ ] A timeout, a stopped server or invalid JSON gives `LlmFailure`, and a unit test with a fake shows the caller falling back.
- [ ] Logs show latency and token counts, and no text.
