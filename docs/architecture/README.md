# Architecture

How Flare is built. Product behaviour is in [product](../product.md), vocabulary in [GLOSSARY.md](../../GLOSSARY.md), conventions in [code rules](../code-rules.md), and decisions in [ADRs](../adr/). ADRs 0009 to 0019 are accepted, except ADR-0016 (model choice), which is decided after the eval run.

| File | Contents |
| --- | --- |
| [repo-structure.md](repo-structure.md) | Folder layout of the whole repo and of each app |
| [data-model.md](data-model.md) | Database schema, enums, state machines, retention |
| [api.md](api.md) | Every endpoint and data shape. The source for `contract/openapi.yaml` |
| [pipeline.md](pipeline.md) | What happens to a message, step by step: limits, Emergency, classification, grouping, summaries, priority. The LLM calls and their schemas |
| [backend.md](backend.md) | Spring Boot packages, seams between tracks, configuration |
| [frontends.md](frontends.md) | Passenger web app and Staff app structure, screens, state, i18n, mock mode, copy |
| [auth-and-security.md](auth-and-security.md) | Staff sign-in, visibility rules, secrets, and the threat-to-mitigation map |
| [operations.md](operations.md) | Environments, environment variables, running locally, seeding, deploying, the demo fallback |

## The system in one picture

```
 Passenger's phone (browser)                       Staff member's phone (Expo Go)
 ┌───────────────────────────────┐                 ┌───────────────────────────────┐
 │ Passenger web app              │                 │ Staff app                     │
 │ React + Vite + TypeScript      │                 │ Expo + React Native + TS      │
 │ /q/{qrCodeId} → one text field │                 │ sign-in · Issue list · detail │
 └───────────────┬───────────────┘                 └───────┬───────────────┬───────┘
                 │ HTTPS  POST /api/public/...             │ HTTPS          │ email + password
                 │ (no account)                            │ Bearer JWT     │ (sign-in only)
                 ▼                                         ▼                ▼
 ┌──────────────────────────────────────────────────────────────┐   ┌──────────────────┐
 │ Spring Boot back end (single instance)                        │   │ Supabase Auth    │
 │                                                               │◀──│ (issues the JWT; │
 │  public API ─▶ intake pipeline ─▶ classification (Claude)     │JWKS│ back end checks │
 │                    │                                          │   │ it via JWKS)     │
 │                    ▼                                          │   └──────────────────┘
 │             grouping worker (1 thread) ─▶ matcher, summariser │
 │                    │                                          │   ┌──────────────────┐
 │  staff API ◀── Issues · Teams · review · audit · retention    │──▶│ Claude API       │
 └───────────────┬───────────────────────────────────────────────┘   │ (only caller:    │
                 │ JDBC                                              │  the back end)   │
                 ▼                                                   └──────────────────┘
 ┌──────────────────────────────┐
 │ Supabase Postgres            │
 │ schema "flare" (not exposed) │
 └──────────────────────────────┘
```

## Responsibilities

| Component | Owns | Never does |
| --- | --- | --- |
| Passenger web app | Reading the QR code ID from the URL, the device ID, one text field, showing the outcome (confirmation, answer, Bounce, Emergency), the language switch | Classifying, deciding the Location, calling the LLM, storing anything except the device ID and language |
| Staff app | Sign-in through Supabase Auth, polling the Issue list, Issue detail and actions, Needs review for the duty manager | Reading the database directly, deciding visibility |
| Spring Boot | Every rule: QR code lookup, rate limits, Strict mode, bot check, Emergency words, LLM calls, grouping, priority, Team visibility, audit, retention | Rendering UI, trusting values sent by a client for Location, Team, role or limits |
| Supabase | Postgres storage, staff accounts and JWTs | Serving data to the apps (the Data API, Realtime and Storage are unused) |
| Claude | Classification, Knowledge base answers, matching, summaries, always in a fixed output shape | Taking actions, seeing secrets, seeing anything except the message, the candidate data and the Knowledge base |

## The key flows

1. **A message comes in** (synchronous, the Passenger waits about 1-5 s):
   - **Emergency word match:** the response is `EMERGENCY` at once.
   - **Over a limit or failing the bot check:** silently Quarantined, response `REPORTED`.
   - **Otherwise,** one LLM call, then the response is `ANSWERED`, `BOUNCED` or `REPORTED`.

   See [pipeline](pipeline.md).
2. **A Report becomes part of an Issue** (asynchronous, single thread): the worker picks candidate Issues, the LLM matcher decides, and the Issue is created or updated, with its summary refreshed. It appears in the Staff app within one poll, about 5 s.
3. **Staff act:** acknowledge, I'm on it, resolve, reopen, override, hand over. Each action is checked for visibility and version, recorded in the audit log, and the list updates on the next poll.

## Design rules that cut across everything

- **Never lose a Report.** A Report is stored before any slow step. Every LLM failure has a defined fallback. The worker re-queues unfinished work on restart.
- **Emergency first.** Nothing (limits, quarantine, the LLM, grouping) delays the Emergency response or hides an Emergency from Security and the duty manager.
- **Untrusted text stays data.** Passenger text never goes into LLM instructions, LLM output is validated against a schema, and both apps show text as plain text only.
- **The server decides.** Location, Team, role and limits are worked out on the server only.
- **Simple and explainable.** Fixed tables (category to Team, priority weights) live in config. The LLM picks categories, not Teams.
- **Simulated data is labelled** wherever it appears: seed data, Knowledge base, staff accounts, the demo.

## Where each track works

| Track | Code | Main docs |
| --- | --- | --- |
| A, back-end platform | `apps/flare-backend/` except the `llm`, `knowledge` and Claude implementations | [backend](backend.md), [data model](data-model.md), [api](api.md), [auth and security](auth-and-security.md) |
| B, Staff app | `apps/flare-mobile/` | [frontends](frontends.md), [api](api.md) |
| C, passenger web app and AI pipeline | `apps/flare-webapp/`, plus `apps/flare-backend/` packages `llm`, `knowledge`, `classification.claude`, `grouping.claude`, `issue.summary.claude` | [frontends](frontends.md), [pipeline](pipeline.md) |
| D, OpenCode agents | `data/`, `scripts/`, fixtures and string files | [tasks](../tasks/README.md) |

The seams between tracks are the contract (`contract/openapi.yaml`) and three Java interfaces (`MessageClassifier`, `IssueMatcher`, `IssueSummarizer`) defined in [backend](backend.md). Track A ships fallback implementations of all three on day one, so the whole system runs before any LLM code exists.
