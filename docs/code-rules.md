# Code rules

Rules for everyone who writes code in this repo, the three team members and Claude alike. The goal is that code written by different people in a hurry still looks like one project. If a rule gets in the way, change the rule here, don't quietly break it.

Items marked **(pending)** depend on a decision not made yet (see [technical](technical.md)).

## 1. General

- **Use the glossary.** Names in code, UI copy, docs and commit messages use the terms in [GLOSSARY.md](../GLOSSARY.md): `Issue`, `Report`, `Question`, `Location`, `QrCode`, `Team`, `Severity`, and so on. Never call an Issue a "ticket"; a Ticket is a plane ticket.
- **English for code,** meaning identifiers, comments and commit messages. UI text is Romanian and English.
- **Simple over clever.** Small functions, one job per module, explicit code. No abstractions "for later"; build for the demo.
- **No new dependency without a reason.** Any new library, service or tool is named in the PR with one line on why, and the team agrees. Prefer what the framework already gives us.
- **One place per kind of setting.** Categories, Severities, Teams, the category-to-Team mapping, rate limits, time windows, QR codes and Locations each live in one config or data file, not as constants scattered around the code.
- **Comments explain why,** not what.
- **Delete dead code.** Do not comment it out; git remembers.

## 2. Security (non-negotiable)

- **Passenger text is untrusted.** It is never rendered as HTML or markup in either app. It is never placed inside the LLM's instructions; it is passed only as clearly separated user content.
- **LLM output is untrusted too.** It is parsed against a fixed schema, and enum values (kind, category, Severity, Team) are checked. Invalid output takes the failure path: the Report becomes Unclassified and goes to the duty manager. It never crashes the request and never loses the Report.
- **The server decides.** Location (from the QR code ID), Team routing, permissions and rate limits are decided on the server. Values sent by a client for these are never trusted.
- **Only Spring Boot calls the LLM** and holds the key ([ADR-0022](adr/0022-self-hosted-llm-llama-cpp.md)).
- **Every staff endpoint** requires sign-in and checks the caller's Team on every request.
- **No personal data in logs.** Do not log contact details or message text. Log IDs and outcomes.
- **Secrets only in environment variables.** Document each name, with a fake value, in `.env.example`. See "Never commit" in [CLAUDE.md](../CLAUDE.md).
- **The emergency path comes first** and nothing may block it.

## 3. API contract

- The API between the apps and Spring Boot is written down in one contract file in the repo. **(pending: OpenAPI proposed.)** It is the single source of truth.
- **Contract changes go first,** in their own small PR, and the other two are told. Code then follows the contract, never the other way round.
- Field names are the same across all apps and match the glossary: `issueId`, `qrCodeId`, `locationId`, `severity`, `team`.
- One error format for every endpoint.
- Times are sent in UTC as ISO 8601 and shown in local time.

## 4. Back end (Spring Boot)

- **Package by feature,** not by layer. The proposed packages are `qrcode`, `intake`, `classification`, `knowledge`, `grouping`, `issue`, `staff`, `audit` (pending planning).
- **Thin controllers.** A controller validates input, calls a service and returns a DTO. Business logic lives in services.
- **Pure logic in plain classes** with no Spring annotations, so it can be unit tested directly. Examples are the priority score, rate-limit counting and choosing grouping candidates.
- **DTOs for the API.** Database entities are never returned directly.
- **LLM access through one interface.** Prompts live in resource files, never inline strings, and change through PRs. Every LLM call has a timeout and a defined failure path.
- **Store the Report before slow work.** Once a message is accepted as a Report, it is stored before grouping or summaries run, so a failure in a later step cannot lose it.
- **Schema changes only through versioned migrations.** **(pending: tool, e.g. Flyway.)**
- Java version and build tool: **(pending)**.

## 5. Passenger web app (React) and Staff app (React Native)

- Language: **(pending: TypeScript proposed for both).**
- Function components and hooks. Keep state local. Add a state library only with the team's agreement.
- **No hard-coded UI text.** Every string is a translation key with both Romanian and English. A missing translation is a bug. Use plain words and short sentences.
- **Accessibility:**
  - every control has an accessible label
  - touch targets are at least 48 px, and the passenger web app's main buttons are at least 56 px tall
  - text contrast meets WCAG 2.2 AA
  - colour is never the only signal
  - the system text size is respected
- **Plain text only.** No `dangerouslySetInnerHTML` on the web. In React Native, data is shown with `Text`, never in a web view.
- **The passenger web app stays light.** No heavy UI kits, system fonts, and it must work on a slow connection.
- **Design:** follow [docs/design](design/README.md): tokens from [foundations](design/foundations.md), components and screens as specified, compared against the [reference screens](design/reference/README.md). No new colours, sizes or components without updating the design docs in the same PR. Avoid the generic "AI app" look: gratuitous gradients, glow, glassmorphism, emoji used as icons.

## 6. Tests

- **Pure logic is written test-first:** emergency word detection, priority score, rate limiter, grouping candidate selection, and LLM output parsing and validation.
- **Evaluation script.** It runs over the labelled Romanian and English test set and prints the metrics in [the AI pipeline doc](triage-and-dedup.md). Run it before and after any prompt change and record the result in the PR.
- **Passenger web app tests** (decided 2026-10-31, replacing "no browser end-to-end tests"): Vitest and Testing Library for logic and screens (`npm test`), and Playwright for the whole flow in mock mode at 390 px, 320 px and desktop width, in RO and EN (`npm run test:e2e`). The e2e suite fails on mixed languages, horizontal scrolling, a missing 112 strip or lost focus. Run both before a web PR. No browser tests for the Staff app.
- `main` must build and its tests must pass before anything is merged.

## 7. Git (GitHub Flow)

- Branches: `main` plus `feature/<short-name>`, for example `feature/passenger-form` or `feature/rate-limiter`. No other branch types.
- Branches are short-lived. Merge to `main` through a PR, several times a day if possible, and delete the branch after merging. Pull `main` often.
- **One task per PR.** The description says what and why, and links the task file or ADR.
- **Commit messages** are short and in the imperative: "Add rate limiter for QR codes".
- Never force-push `main`. Never commit secrets or real Passenger data.

## 8. Docs and decisions

- **Something decided or changed means an ADR.** Write a new ADR in `docs/adr/`, numbered after the highest existing one, or a one-line entry in [decisions](decisions.md) for small items.
- **Amending an ADR:** an ADR may be corrected in place only while no code is built on it, and the correction is recorded in its `amended` field. Once code depends on it, write a new ADR that supersedes it.
- **Proposed ADRs:** an ADR with status `proposed` can be built on, but changing it is cheap until the team accepts it. Flag any work that would make it expensive to change.
- **Work from task files.** Every piece of work has a task file in [tasks/](tasks/README.md). Update its status as you go, and fix the task file if the plan turns out wrong.
- **A new domain term** goes into [GLOSSARY.md](../GLOSSARY.md) in the same PR.
- **When behaviour changes,** the docs describing it change in the same PR.

## 9. Demo data

- All seed data, Knowledge base content, staff accounts and the test set are fictional and labelled "Simulated".
- No real facts about Craiova Airport are invented. Anything assumed is marked as an assumption.
