# Flare: passenger issue reporting for airports

> "Flare" is the project name (decided in the grilling session). "Squawk" was an earlier working name and is dropped.

## One-line summary

A Passenger scans a QR code in the airport and types a problem or a question into one simple field. Questions get an instant answer from an airport Knowledge base. Problems go to the right Team's Staff app, grouped with other Reports of the same problem and sorted by Severity.

## Hackathon context

- City council and town hall hackathon with airport and aviation themes. Team of three: 2 MacBooks, 1 Windows laptop.
- Target setting: Craiova International Airport. The demo uses simulated data. This is not a real deployment, and every piece of demo data must be labelled as simulated.
- Track: not decided. The team will worry about the pitch later.
- Time budget: about one full day plus a few extra hours (1-2 hours on the first evening, a few more on Sunday). The pitch is on Sunday. Keep scope small.
- Nothing about the airport should be invented. If a fact is unknown, mark it as an assumption.

## Stack (decided at the top level)

React passenger web app (`apps/flare-webapp`), React Native Staff app (`apps/flare-mobile`), Spring Boot back end (`apps/flare-backend`), Supabase, and Claude called only from Spring Boot. See [ADR-0002](docs/adr/0002-three-apps-spring-boot-supabase.md) and [ADR-0003](docs/adr/0003-llm-only-behind-spring-boot.md). The detailed technical choices are ADRs 0009 to 0019, all accepted except ADR-0016 (model choice), which waits for the eval run:
- Expo
- Vite
- TypeScript
- Maven
- Supabase for Postgres and Auth only
- polling
- OpenAPI
- the model per task in config

The full design is in [docs/architecture/](docs/architecture/README.md).

## Where things live

Read the relevant file before working on that area, and update it when a decision changes. Do not duplicate content between files; link instead.

| File | Contents |
| --- | --- |
| [GLOSSARY.md](GLOSSARY.md) | Domain vocabulary. Use these terms in code, UI copy and docs. |
| [docs/code-rules.md](docs/code-rules.md) | Coding, security, contract, testing, git and docs rules for everyone, Claude included |
| [docs/adr/](docs/adr/) | Architecture decision records, numbered |
| [docs/architecture/](docs/architecture/README.md) | System design: repo structure, data model, API, pipeline and LLM calls, back end, frontends, auth and security, operations |
| [docs/tasks/](docs/tasks/README.md) | Workplan: tracks, phases and gates, the task board, cut list, one file per task |
| [docs/product.md](docs/product.md) | Problem, users, scope, core flow, passenger web app and Staff app requirements, resolution model, lost luggage, demo plan |
| [docs/principles.md](docs/principles.md) | Product principles in priority order, privacy, honesty about limits |
| [docs/security-and-abuse.md](docs/security-and-abuse.md) | Threat model: QR code impersonation, request limiting and quarantine, prompt injection, staff access |
| [docs/triage-and-dedup.md](docs/triage-and-dedup.md) | AI pipeline: classification, quality check, Knowledge base answers, grouping, summaries, measurement |
| [docs/technical.md](docs/technical.md) | Stack, architecture sketch, Claude API facts, open technical questions |
| [docs/marketing.md](docs/marketing.md) | Passenger adoption, poster contents, airport buy-in, pitch positioning |
| [docs/ideas.md](docs/ideas.md) | Stretch goals and parking lot |
| [docs/decisions.md](docs/decisions.md) | Decision log by round, ADR index, open questions, pitch description |

## Must-have scope (decided)

- A passenger web app with one text field.
- LLM classification of each message: Report, Question or Bounced.
- Knowledge base answers to Questions.
- LLM grouping of Reports into Issues, with counts and summaries.
- A Staff app with individual accounts and Team routing.
- The mentor-driven slice: unknown QR code flagging, rate limiting with quarantine, and the quality check.

Everything else is stretch. See [docs/product.md](docs/product.md).

## Safety boundary (overrides everything)

- This is not an emergency channel and must never present itself as one.
- The passenger web app shows the emergency number (112) on every screen. If a message suggests a medical emergency, a security threat or a missing child, show the Passenger emergency instructions immediately and flag it to Security and the duty manager as Critical. Do not give medical or security advice. Nothing blocks emergency handling: not the quality check, not rate limits, not an LLM outage.
- The system never asks for payments, passwords or sensitive personal data. Ticket data never passes through Flare (decided; see [docs/product.md](docs/product.md)).

## Never commit

- Secrets: API keys (including the Claude API key), `.env` files, credentials. Keys live in environment variables or an ignored `.env`. Commit only `.env.example`, with fake placeholders.
- Real Passenger data: Reports, photos, Tickets, contact details. Only simulated data labelled as such goes in the repo. Simulated seed data, the Knowledge base and the labelled test set are committed.
- The demo fallback recording. It is kept in the ignored `recordings/` folder and shared outside git.
- Personal `.claude/settings.local.json`. The shared `.claude/settings.json` is committed.
- Keep `.gitignore` free of broad patterns (`*.json`, `*.csv`, `data/`) that would hide seed data or the test set. Add each app's ignore rules (for example `node_modules/`, build output) before anyone installs anything.
- `.gitignore` does not protect tracked files or history. If a secret is committed, rotate it.

## Working agreements for Claude

- Follow [docs/code-rules.md](docs/code-rules.md) and use the terms in [GLOSSARY.md](GLOSSARY.md).
- The top-level stack is decided. Do not add any other framework, library, service or tool unprompted. When a technical decision is needed, present options with a recommendation and wait for the team.
- Write or update ADRs throughout development whenever something is decided or changes. Log small decisions in [docs/decisions.md](docs/decisions.md). Add new terms to [GLOSSARY.md](GLOSSARY.md) as they come up.
- Planning is done: build from the task files in [docs/tasks/](docs/tasks/README.md). Before starting a task, read it and the architecture docs it links. Claim it, follow its **Done when** list, and keep its status current. If the plan turns out wrong, fix the task file and the architecture doc in the same PR.
- When writing Claude API code (Track C), use the `claude-api` skill and its Java docs. Never guess SDK names.
- Run grilling sessions before building. Ask clarifying questions instead of guessing.
- Prefer simple, explainable logic over clever logic.
- Be honest about limits: location can be spoofed, LLMs make mistakes, and demo data is simulated.
- UI text in plain Romanian and English.
- Keep changes small and explain security-relevant choices.
