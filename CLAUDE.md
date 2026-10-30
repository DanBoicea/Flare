# Flare: passenger issue reporting for airports

> "Flare" is the project name (decided in the grilling session). "Squawk" was an earlier working name and is dropped.

## One-line summary

A passenger scans a QR code in the airport, describes a problem in a very simple web form, and staff see it in real time on a dashboard that sorts by severity and groups duplicates. Routing to specific teams comes in a later phase.

## Hackathon context

- City council and town hall hackathon with airport and aviation themes. Team of three.
- Target setting: Craiova International Airport. The demo uses simulated data. This is not a real deployment, and every piece of demo data must be labelled as simulated.
- Track: not decided. The team will worry about the pitch later.
- Time budget: about one full day plus a few extra hours (1-2 hours on the first evening, a few more on Sunday). The pitch is on Sunday. Keep scope small.
- Nothing about the airport should be invented. If a fact is unknown, mark it as an assumption.

## Where things live

Detail is in `docs/`. Read the relevant file before working on that area, and update it when a decision changes. Do not duplicate content between files; link instead.

| File | Contents |
| --- | --- |
| [docs/product.md](docs/product.md) | Problem, users, must-have vs stretch scope, core flow, form and dashboard requirements, resolution model, ticket details, demo plan |
| [docs/principles.md](docs/principles.md) | Product principles in priority order, privacy, honesty about limits |
| [docs/security-and-abuse.md](docs/security-and-abuse.md) | Threat model: QR impersonation, request limiting and quarantine, prompt injection, uploads, access, privacy |
| [docs/triage-and-dedup.md](docs/triage-and-dedup.md) | Categories, severity scale, request quality gate, triage rules, deduplication, measurement |
| [docs/marketing.md](docs/marketing.md) | Passenger adoption, airport buy-in, pitch positioning, draft signage |
| [docs/technical.md](docs/technical.md) | Technical guide. Placeholder: stack undecided, planning not started |
| [docs/ideas.md](docs/ideas.md) | Stretch goals and parking lot |
| [docs/decisions.md](docs/decisions.md) | Decision log by grilling round, open questions, mentor feedback, pitch description |

## Must-have scope (decided)

Passenger form, triage, duplicate grouping, a user-friendly staff dashboard, and the mentor-driven slice: unknown-code flagging, rate limiting with quarantine, and a request quality gate. Everything else is stretch. Phase 1 has one shared staff app with no role routing. See [docs/product.md](docs/product.md).

## Safety boundary (overrides everything)

- This is not an emergency channel and must never present itself as one.
- The form shows the emergency number prominently. If a report suggests a medical emergency, security threat or missing child, show the passenger emergency instructions immediately and flag the report to staff as top priority. Do not give medical or security advice. Emergency handling is never blocked by the quality gate, rate limits or an LLM outage.
- The system never asks for payments, passwords or sensitive personal data. Optional minimal ticket details are the one open exception and need team confirmation (see [docs/decisions.md](docs/decisions.md)).

## Working agreements for Claude

- The technology stack is undecided. Do not assume, add or install any framework, language, database, service or library unprompted. When a technical decision is needed, present options and wait for the team.
- Technical planning (architecture, task files for three parallel workers) has not started. Do not start it until the team says so.
- Run grilling sessions before building. Ask clarifying questions instead of guessing, and record decisions in [docs/decisions.md](docs/decisions.md).
- Prefer simple, explainable logic over clever logic.
- Be honest about limits: location can be spoofed, LLMs make mistakes, and demo data is simulated.
- Never commit secrets.
- UI text in plain Romanian and English.
- Keep changes small and explain security-relevant choices.
