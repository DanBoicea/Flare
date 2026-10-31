# Technical guide

**Status:** the stack is decided at the top level ([ADR-0002](adr/0002-three-apps-spring-boot-supabase.md), [ADR-0003](adr/0003-llm-only-behind-spring-boot.md)). Details are still being grilled (see "Open technical questions"). Planning (architecture detail and task files) has not started. Code conventions are in [code rules](code-rules.md).

## Stack

| Part | Technology | Notes |
| --- | --- | --- |
| Passenger web app | React (web) | Opened from a QR code. Must be served over HTTPS. |
| Staff app | React Native (mobile) | iOS builds need a Mac. The team has 2 MacBooks and 1 Windows laptop. |
| Back end | Spring Boot (Java) | The only place with business logic and the LLM key. |
| Data | Supabase | What it does versus Spring Boot is still open. |
| LLM | Claude, via Anthropic's official Java SDK | Called only by Spring Boot. |

Tools installed on the Windows laptop: Java 21, Node 22, npm 10, Docker 29, Python 3.13. The MacBooks have not been checked.

## Architecture sketch

```
 Passenger's phone (browser)                Staff member's phone
 ┌──────────────────────────┐              ┌──────────────────────────┐
 │ Passenger web app (React)│              │ Staff app (React Native) │
 └────────────┬─────────────┘              └────────────┬─────────────┘
              │ HTTPS: message + QR code ID             │ HTTPS: sign-in, Issues, actions
              │                                         │ + live updates (mechanism open)
              ▼                                         ▼
 ┌─────────────────────────────────────────────────────────────────────┐
 │ Spring Boot back end                                                │
 │  QR codes & Locations · limits & quarantine · classification ·      │
 │  Knowledge base answers · grouping · Issues & Teams · audit log     │
 └───────────────┬───────────────────────────────────┬─────────────────┘
                 ▼                                   ▼
        Supabase (database, ...)               Claude API
```

## Back-end pipeline (logical order, to confirm at planning)

1. Receive the message and the QR code ID. Resolve the Location. Flag unknown or muted IDs.
2. Rate limits and bot check. Over the limit, the Report is Quarantined.
3. Emergency handling. It is never blocked by later steps.
4. LLM classification: Report, Question or garbage, plus category, Severity, Team, summary, language and Location mismatch.
5. A Question is answered from the Knowledge base. "Send to staff anyway" turns it into a Report.
6. A Report is stored first, then grouped into an Issue, and the Issue's Team is notified.
7. The Issue summary is refreshed when new Reports join.

## Claude API facts (checked 2026-10-30)

- **SDK:** Anthropic publishes an official Java SDK (`com.anthropic:anthropic-java`). It reads the key from the `ANTHROPIC_API_KEY` environment variable.
- **Structured output:** the API supports it (JSON schema), so every LLM answer can be forced into a fixed shape and validated.
- **Billing:** the back end needs an API key from the Claude Console, paid from API credits. A Claude Code or claude.ai subscription does not pay for API calls made by our app.
- **Prices** (Anthropic list prices, per million tokens, as cached on 2026-10-06; check before relying on them):
  - Claude Opus 5.5 (`claude-opus-5-5`): $4 input / $20 output. The most capable current Opus.
  - Claude Haiku 5.5 (`claude-haiku-5-5`): $0.10 input / $0.50 output. The cheapest current model.
- **Rough cost per classification call,** assuming about 1,500 input and 200 output tokens and excluding thinking tokens: about $0.01 on Opus 5.5, about $0.0003 on Haiku 5.5. Which model we use is an open question.

## Requirements carried from the product docs

- A mobile-first passenger web app in Romanian and English that works on slow connections and small screens.
- A Staff app that updates live, with individual accounts and Team routing.
- Rate limiting, a Quarantined queue, a bot check, and unknown QR code flagging.
- LLM calls with fixed-structure output, timeouts and a defined behaviour on failure.
- All Passenger text and LLM output displayed as plain text in both apps.
- Work that three people can do in parallel, with clear interfaces between the parts.

## Open technical questions (next grilling round)

- How Supabase and Spring Boot split the work: database only, or also sign-in, live updates and file storage?
- How the Staff app gets live updates: polling, WebSocket from Spring Boot, Supabase Realtime, push notifications?
- Expo or plain React Native, and how the team runs the Staff app on phones during the demo.
- TypeScript for both apps, and the web app's build tooling.
- Spring Boot setup: Java version, Maven or Gradle, database access, migrations.
- The API contract format between the apps and Spring Boot, and whether the TypeScript types are generated from it.
- Repository layout (folders for each app, the contract, the data and the docs).
- The Claude model, and a cost limit.
- The QR code URL format (opaque ID, proposed).
- Where the Knowledge base lives and how it reaches the LLM.
- Hosting: the team handles it, but the passenger web app needs HTTPS.

## Planning approach (agreed, not started)

- An architecture plan, then tasks written as markdown files in the project so three people can work in parallel.
- Do not start until the team says so.
