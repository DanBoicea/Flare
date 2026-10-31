---
status: accepted
date: 2026-10-31
supersedes: 0003
---

# The LLM is a self-hosted open-weight model served by llama.cpp, called only by Spring Boot

Flare no longer uses the Claude API. Every LLM call (classification with the Knowledge base answer, matching, summaries) goes to an open-weight model that we run ourselves with [llama.cpp](https://github.com/ggml-org/llama.cpp)'s `llama-server`. The rule from ADR-0003 stays: **only the Spring Boot back end talks to the LLM.** The passenger web app and the Staff app never call it and never learn its address or key.

- **Interface:** `llama-server` exposes an OpenAI-compatible `POST /v1/chat/completions`. The back end calls it with Spring's built-in HTTP client, so no new library is needed and `com.anthropic:anthropic-java` is dropped (ADR-0013).
- **Fixed output:** `response_format` with `type: "json_schema"` makes the server constrain the output to our JSON schema. We still validate every answer again in our own code, and anything invalid goes to the fallback, exactly as before.
- **Where it runs:** on a machine we control: a team laptop on the same network during development (S04), and a decided demo machine for the pitch (S10). The back end finds it through `FLARE_LLM_BASE_URL`. If `llama-server` listens on anything other than `127.0.0.1`, it is started with `--api-key`, and the back end sends that key from `FLARE_LLM_API_KEY`.
- **Model:** chosen in S04 by a quick check, then confirmed by the evaluation run (C05, C06; ADR-0016). It must be an instruction-tuned model in GGUF format whose licence allows this use, that handles Romanian and English, and that answers within the classify timeout on our hardware. Model files are never committed (`*.gguf` is git-ignored).

## Why

- **The team chose it.** Running our own model means no API account, no per-call cost and no spend limit to manage.
- **Privacy:** Passenger text never leaves machines we control, which is a stronger story for an airport and for the pitch than "sent to a US provider".
- **Nothing else changes:** one gateway (`llm.LlmClient`), structured output, timeouts, validation and fallbacks stay as designed, so Track A's work is unaffected.

## Consequences

- **Quality is lower and unknown until measured.** An open model that fits on a laptop will make more mistakes than a frontier model, especially in Romanian. The fallbacks, the staff override, the original Reports always being visible, and the evaluation numbers (C05) matter even more. The pitch says this honestly.
- **Latency is higher.** Classification is synchronous, so the Passenger waits for it. S04 measures it; the classify timeout in config (ADR-0016) is set from that measurement, and on timeout the fallback answers.
- **Someone must run the server.** If `llama-server` is down, every call fails fast and the fallbacks run: Reports become Unclassified and go to the duty manager. Emergency handling never depends on it (CLAUDE.md safety boundary; the word list in A07).
- **Prompt caching** is done by `llama-server` itself, which reuses a prompt prefix it has already processed. Keep the system prompt (instructions plus the Knowledge base) identical and first in every call so the prefix can be reused.
- **No more `ANTHROPIC_API_KEY`**: the S04 Anthropic key task is replaced by setting up `llama-server`, and S07 no longer revokes a key.
- The `claude` packages in the back end are renamed `llm` (`classification.llm`, `grouping.llm`, `issue.summary.llm`).

## Considered options

- **Keep the Claude API (ADR-0003):** better quality and less setup, but per-call cost, an external account and Passenger text sent to a third party. The team chose self-hosting.
- **Ollama instead of llama.cpp:** a friendlier wrapper around the same engine, but llama.cpp's server gives direct control of JSON-schema output and flags, and is what the team picked.
- **Another hosted open-model API:** still sends Passenger text to a third party, which is the main thing self-hosting avoids.
