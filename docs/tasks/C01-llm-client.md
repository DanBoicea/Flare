# C01: `LlmClient` on the Claude Java SDK: config, timeouts, failures, usage cap

Status: TODO
Owner:
Track: C · Phase 2 · Estimate: 1 h · Agent-friendly: no (main Claude session, using the `claude-api` skill)
Depends on: A01, S04
Unblocks: C02, C03, C04

## Goal
One safe gateway to Claude that every LLM feature uses ([ADR-0003](../adr/0003-llm-only-behind-spring-boot.md), [ADR-0016](../adr/0016-llm-model-per-task-in-config.md)).

## Read first
[pipeline section 4](../architecture/pipeline.md#4-llm-calls-track-c-adr-0003-adr-0016) · [backend: config](../architecture/backend.md#configuration-applicationyml-secrets-only-from-env-vars) · **the `claude-api` skill and its `java/` documentation. Do not guess SDK class or method names. If something is not documented there, check the official SDK repository it links.**

## Do
1. Add `com.anthropic:anthropic-java` to `pom.xml`, at the version given in the skill's Java README or the latest release.
2. **`llm/LlmTask`** enum (`CLASSIFY`, `MATCH`, `SUMMARIZE`) and **`LlmTaskProperties`** (model, effort, `max-tokens`, timeout) from `flare.llm.tasks.*`.
3. **`llm/LlmClient`:** `<T> T call(LlmTask task, String systemPrompt, String userContent, JsonSchema schema, Class<T> type)`. The exact shape may follow the SDK's structured-output API; keep the interface small. Implementation `ClaudeLlmClient`:
   - client from env (`ANTHROPIC_API_KEY`), created once
   - model, effort (`output_config.effort`) and `max_tokens` per task; structured output with the JSON schema
   - the system prompt marked for **prompt caching**
   - a timeout per task, and at most one retry on 429 or 5xx inside the timeout
   - check `stop_reason`: a `refusal`, `max_tokens`, or anything other than a normal end throws `LlmFailure`
   - parse into `T`; parse errors throw `LlmFailure`
   - log per call: task, model, latency, input, output and cache-read tokens. **Never prompt or response text**
   - `LlmUsageCounter`: a daily call cap (`flare.llm.daily-call-limit`); over the cap, throw `LlmFailure` without calling
   - the skill recommends server-side refusal `fallbacks` for this model family. Decide with the team whether to enable them; if enabled, note it in ADR-0016
4. **`llm/PromptLoader`:** loads `prompts/*.md` from the classpath at startup and fails fast if one is missing.
5. **Wiring:** only when `flare.llm.enabled=true` and the key is set (`@ConditionalOnProperty`). Otherwise there is no `LlmClient` bean, and the fallbacks stay active.
6. **Smoke test:** a test tagged `eval` that calls `CLASSIFY` with a trivial schema. It runs only on demand, because it costs money.

## Done when
- [ ] The smoke test passes with a real key.
- [ ] The app starts with the LLM disabled and no key.
- [ ] A forced timeout or an invalid key gives `LlmFailure`, which a unit test with a fake shows being handled by the caller.
- [ ] Logs show tokens and no text.
