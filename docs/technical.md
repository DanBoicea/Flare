# Technical guide

The entry point for technical work. The full design is in [architecture/](architecture/README.md), and the work is split into [tasks](tasks/README.md). Conventions are in [code rules](code-rules.md).

## Stack

| Part | Technology | Decision |
| --- | --- | --- |
| Passenger web app | React + Vite + TypeScript | [ADR-0002](adr/0002-three-apps-spring-boot-supabase.md), [ADR-0013](adr/0013-tooling-and-dependencies.md) |
| Staff app | Expo + React Native + TypeScript, run in Expo Go | ADR-0002, ADR-0013 |
| Back end | Spring Boot, Java 21, Maven wrapper, single instance | ADR-0002, ADR-0013, [ADR-0015](adr/0015-single-instance-in-memory-limits.md) |
| Data and staff sign-in | Supabase Postgres + Supabase Auth; apps never touch the database | [ADR-0010](adr/0010-supabase-postgres-and-auth-only.md) |
| API | Contract-first OpenAPI in `contract/openapi.yaml` | [ADR-0012](adr/0012-contract-first-openapi.md) |
| Live updates | Staff app polls every 5 s | [ADR-0011](adr/0011-staff-app-polling.md) |
| LLM | Self-hosted open-weight model served by llama.cpp's `llama-server`, called only by Spring Boot over its OpenAI-compatible HTTP API, settings per task in config | [ADR-0022](adr/0022-self-hosted-llm-llama-cpp.md), [ADR-0014](adr/0014-single-classification-call-with-knowledge-base.md), [ADR-0016](adr/0016-llm-model-per-task-in-config.md) (proposed) |
| Repository | One repo, independent apps | [ADR-0009](adr/0009-monorepo-independent-apps.md) |

Team machines: 2 MacBooks, 1 Windows laptop. The Windows laptop has Java 21, Node 22, npm 10, Docker 29 and Python 3.13. Check the MacBooks in task S05.

## llama.cpp server facts (checked 2026-10-31 against the `llama-server` README)

- **What it is:** `llama-server` from [llama.cpp](https://github.com/ggml-org/llama.cpp) serves one GGUF model over HTTP. Install it from the project's releases or a package manager (for example Homebrew on macOS); S04 records the exact steps that worked on our machines.
- **API:** an OpenAI-compatible `POST /v1/chat/completions`. `response_format` accepts `{"type": "json_object"}` and schema-constrained `{"type": "json_schema", ...}`, so every answer can be forced into a fixed shape. We validate it again in our own code. C01 confirms the exact request and response field names against the server README before relying on them.
- **Health:** `GET /health` returns `503` while the model loads and `200 {"status": "ok"}` once it is ready.
- **Our settings** are in `scripts/llm/start-llama-server.mjs` and were measured in S04: one slot (`-np 1`), 8,192 tokens, 8-bit KV cache, capped checkpoints and prompt cache, thinking off, CORS limited to localhost.
- **Useful flags:** `-m <file.gguf>` or `-hf <user>/<repo>[:quant]` (download from Hugging Face), `--host` (default `127.0.0.1`), `--port` (we use `8081`; the back end is on `8080`), `-c` context size (large enough for the Knowledge base plus a message), `-ngl` GPU layers (default auto), `-a/--alias` (we use `flare-llm`), `--api-key` (required whenever it listens beyond `127.0.0.1`).
- **Prompt caching:** the server reuses a prompt prefix it has already processed, so the system prompt (instructions plus the Knowledge base) must be identical and first in every call.
- **Cost:** no per-call price. The limits are speed and memory on the machine that runs it, measured in S04.
- **Model choice:** an instruction-tuned GGUF model whose licence allows this use, good enough in Romanian and English, and fast enough for the classify timeout. Candidates and measurements go in S04; the final choice comes from the eval (C05, C06, ADR-0016).

## Open technical items

- ADR-0016 (model and settings per task) is decided after the eval run (C06).
- Which machine runs `llama-server` for the demo (S10).
- Hosting is chosen by the team. The requirements are in [operations](architecture/operations.md).
