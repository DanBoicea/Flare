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
| LLM | Claude via the official Java SDK, called only by Spring Boot, model per task in config | [ADR-0003](adr/0003-llm-only-behind-spring-boot.md), [ADR-0014](adr/0014-single-classification-call-with-knowledge-base.md), [ADR-0016](adr/0016-llm-model-per-task-in-config.md) (proposed) |
| Repository | One repo, independent apps | [ADR-0009](adr/0009-monorepo-independent-apps.md) |

Team machines: 2 MacBooks, 1 Windows laptop. The Windows laptop has Java 21, Node 22, npm 10, Docker 29 and Python 3.13. Check the MacBooks in task S05.

## Claude API facts (checked 2026-10-30)

- **SDK:** Anthropic publishes an official Java SDK (`com.anthropic:anthropic-java`). It reads the key from the `ANTHROPIC_API_KEY` environment variable. Read the `claude-api` skill's Java docs before writing any SDK code; never guess class or method names.
- **Structured output:** the API supports it (JSON schema), so every answer can be forced into a fixed shape. We validate it again in our own code.
- **Billing:** the back end needs an API key from the Claude Console, paid from API credits. A Claude Code or claude.ai subscription does not pay for API calls made by our app.
- **Prices** (Anthropic list prices per million tokens, as cached on 2026-10-06; check before relying on them):
  - Claude Opus 5.5 (`claude-opus-5-5`): $4 input / $20 output. The current default model.
  - Claude Haiku 5.5 (`claude-haiku-5-5`): $0.10 input / $0.50 output. The cheapest current model.
- **Rough cost per classification call,** assuming about 1,500 input and 200 output tokens and excluding thinking tokens: about $0.01 on Opus 5.5, about $0.0003 on Haiku 5.5. The team picks the model per task from eval results ([ADR-0016](adr/0016-llm-model-per-task-in-config.md)).
- **Model behaviour:** on Opus 5.5, thinking can't be switched off; use effort `low` to keep calls short. Always check for the `refusal` stop reason and treat it as a failure, which sends the call to the fallback.

## Open technical items

- ADR-0016 (model per task) is decided after the eval run (C06).
- Lombok: the skeleton includes it; keep or remove it (S05).
- Hosting is chosen by the team. The requirements are in [operations](architecture/operations.md).
