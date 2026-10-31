# Tasks

The workplan for three people plus OpenCode agents. Each task is one Markdown file in this folder. Read [architecture](../architecture/README.md) first, then your task file. Every task follows [code rules](../code-rules.md) and [GLOSSARY.md](../../GLOSSARY.md).

## Tracks

| Track | Scope | Owner |
| --- | --- | --- |
| **S, shared** | Kick-off, contract, accounts, integration, demo, pitch | Everyone. Each task names a lead |
| **A, back-end platform** | Spring Boot skeleton, database, staff sign-in, Issues API, intake orchestration, limits, grouping worker, review, retention, deploy | **Cristi** |
| **B, Staff app** | Expo app end to end, plus the shared visual design pass | **Cristi** |
| **C, passenger web app and AI pipeline** | React web app first (it has no back-end dependency), then the Claude integration inside the back end | **Dan** |
| **D, OpenCode agents** | Bulky, well-specified work: simulated data, test set, Knowledge base, string files, fixtures, scenario scripts, posters, research | **Luciano** (every D task names a human reviewer) |

Write names in `Owner:` when you claim a task. The track split is a recommendation; anyone may take any unblocked task.

## How to work a task

1. Pick an unblocked task (all `Depends on` are `DONE`) from your track, or any track if yours is blocked.
2. Set `Status: IN PROGRESS` and `Owner:` in the task file, then commit and push it at once so others see the claim.
3. Branch `feature/<task-id>-<short-name>`, for example `feature/a01-backend-skeleton`.
4. Do the work and meet every item in **Done when**. Update docs, glossary and ADRs in the same PR when you change behaviour.
5. Open a PR. Merge when the tests pass and the checklist is met, without waiting for reviews except where a task says `Review: required`. Set `Status: DONE`.
6. If the plan is wrong, fix the task file (and this board) in your PR. Don't silently do something else.

**Statuses:** `TODO`, `IN PROGRESS`, `DONE`, `CUT`, `BLOCKED (<why>)`. Status lives only in the task files. Check them with `grep -H "^Status:" docs/tasks/*.md`.

**Contract changes** (`contract/openapi.yaml`) are their own PR, announced in the team chat. Both apps regenerate their types afterwards.

**For OpenCode agents:** give the agent the task file and these files: `CLAUDE.md`, `GLOSSARY.md`, `docs/code-rules.md`. Agents never add dependencies, never touch files outside the task's **Files** list, never commit secrets or real data, and always open a PR for human review.

## Phases and gates (today is the main build day; Sunday is demo day)

| Phase | When (approx.) | Goal | Gate to pass before moving on |
| --- | --- | --- | --- |
| 0, Kick-off | First hour | Accounts, keys, repo skeleton folders, contract v1 | **G0:** contract v1 merged; Supabase dev project and API key shared privately; everyone builds their skeleton |
| 1, Thin slice | Hours 1-5 | Message to Issue to staff phone, with fallback classification | **G1:** a message typed on the web app appears, routed by Team, in the right staff account within about 10 s |
| 2, Smart and safe | Hours 5-10 | LLM classification, Knowledge base answers, Bounce, grouping, summaries, limits, review, actions | **G2:** every demo scenario runs on the dev environment |
| 3, Measure and polish | Hours 10-13 | Eval numbers, prompt tuning, design and accessibility pass, hardening | **G3:** eval results recorded; hardening checklist ticked; UI pass done |
| 4, Demo | Sunday morning | Fresh demo environment, rehearsal, recording, pitch | **G4:** two clean rehearsals, fallback recording saved |

**Integration checkpoints:** a 10-minute sync at each gate. Merge to `main` at least every 2 hours.

## Board

| ID | Task | Track | Phase | Depends on |
| --- | --- | --- | --- | --- |
| [S01](S01-repo-scaffold-and-gitignore.md) | Repo folders, `.gitignore` for all stacks, `.env.example`s | S (A lead) | 0 | none |
| [S02](S02-api-contract-v1.md) | API contract v1 (`contract/openapi.yaml`) | S (A lead) | 0 | none |
| [S03](S03-supabase-project-and-accounts.md) | Supabase dev project, schema settings, demo staff accounts | S (A lead) | 0 | none |
| [S04](S04-anthropic-api-key.md) | Anthropic Console API key with spend limit | S (C lead) | 0 | none |
| [S05](S05-team-kickoff-and-machines.md) | Assign tracks, check tools on every machine, accept proposed ADRs | S (all) | 0 | none |
| [S06](S06-integration-g1-g2.md) | Integration runs at G1 and G2 | S (all) | 1-2 | see file |
| [S07](S07-demo-environment-and-recording.md) | Demo environment, rehearsal, fallback recording | S (all) | 4 | G3 |
| [S08](S08-pitch.md) | Pitch script, slides, honest numbers | S (all) | 3-4 | C05 |
| [S09](S09-ci-optional.md) | CI on GitHub Actions (optional, needs team OK) | S (A lead) | 2 | A01, W01, B01 |
| [A01](A01-backend-skeleton.md) | Spring Boot skeleton, config, errors, health, CORS | A | 1 | S01 |
| [A02](A02-database-schema.md) | Flyway schema, entities, repositories | A | 1 | A01, S03 |
| [A03](A03-staff-auth.md) | Supabase JWT verification, `CurrentStaff`, `/me`, `/teams`, access policy | A | 1 | A02 |
| [A04](A04-issues-read-api-and-priority.md) | Issue list and detail, priority calculator | A | 1 | A03 |
| [A05](A05-issue-actions-and-audit.md) | Issue actions, optimistic locking, audit log | A | 2 | A04 |
| [A06](A06-intake-orchestration.md) | Public endpoints, intake pipeline, fallbacks, Questions | A | 1 | A02, S02 |
| [A07](A07-safety-and-limits.md) | Emergency words, bot check, rate limits, Strict mode, Bounce tracker, hashing | A | 2 | A06 |
| [A08](A08-grouping-worker.md) | Grouping worker, candidates, Issue create, merge, reopen | A | 1-2 | A06 |
| [A09](A09-review-queue-api.md) | Needs review API: list, release, dismiss, alerts | A | 2 | A07, A08 |
| [A10](A10-retention-and-log-hygiene.md) | Retention job, log hygiene check | A | 3 | A08 |
| [A11](A11-deploy-readiness.md) | Dockerfile, demo profile, hardening checklist | A | 3 | A09 |
| [B01](B01-staff-app-skeleton.md) | Expo skeleton, theme, i18n, API client, mock mode | B | 1 | S01, S02 |
| [B02](B02-sign-in-and-session.md) | Sign-in, session, `/me`, role-aware tabs | B | 1 | B01, S03 |
| [B03](B03-issue-list-and-polling.md) | Active and Resolved lists, polling, Issue cards | B | 1 | B02 |
| [B04](B04-issue-detail-and-actions.md) | Issue detail, actions, hand over, overrides, conflicts | B | 2 | B03 |
| [B05](B05-needs-review-tab.md) | Needs review tab for the duty manager | B | 2 | B04 |
| [B06](B06-staff-design-and-accessibility.md) | Staff app design and accessibility pass, real phones | B | 3 | B05 |
| [W01](W01-passenger-web-skeleton.md) | Web skeleton, tokens, i18n, API client, mock mode, device and QR code IDs | C | 0-1 | S01, S02 |
| [W02](W02-compose-and-sending.md) | Compose and Sending screens, emergency strip, language button | C | 1 | W01 |
| [W03](W03-outcome-screens.md) | Answer, Bounced, Confirmation, Emergency screens | C | 1-2 | W02 |
| [W04](W04-passenger-design-and-accessibility.md) | Web design and accessibility pass, slow-network test | B | 3 | W03 |
| [C01](C01-llm-client.md) | `LlmClient` on the Claude Java SDK: config, timeouts, failures, usage cap | C | 2 | A01, S04 |
| [C02](C02-classifier-and-knowledge-base.md) | Knowledge base loader, classify prompt and schema, validator | C | 2 | C01, A06, D03 |
| [C03](C03-issue-matcher.md) | LLM issue matcher | C | 2 | C01, A08 |
| [C04](C04-issue-summarizer.md) | LLM Issue summariser | C | 2 | C01, A08 |
| [C05](C05-evaluation-runner.md) | Evaluation runner and results doc | C | 3 | C02, C03, D02 |
| [C06](C06-prompt-tuning.md) | Prompt tuning from eval results, model choice | C | 3 | C05 |
| [D01](D01-simulated-airport-data.md) | Simulated Areas, Locations, QR codes, staff seed, reset script | D | 1 | A02 |
| [D02](D02-labelled-test-set.md) | Labelled RO/EN test set and grouping pairs | D | 1-2 | none |
| [D03](D03-knowledge-base-content.md) | Simulated Knowledge base, RO + EN | D | 1 | none |
| [D04](D04-string-files.md) | RO/EN string files for both apps, i18n check script | D | 1 | W01, B01 |
| [D05](D05-mock-fixtures.md) | Mock fixtures for both apps | D | 1 | S02 |
| [D06](D06-demo-scenario-runner.md) | Demo scenario files and runner script | D | 2 | S02, D01 |
| [D07](D07-qr-posters.md) | Printable QR posters | D | 3 | D01 |
| [D08](D08-competitor-research.md) | Competitor feature check from primary sources | D | 2-3 | none |
| [D09](D09-docs-and-pitch-assets.md) | Pitch description update, README setup, docs consistency | D | 3 | C05 |

## Critical path

`S02 → A06 → A08 → (C02, C03) → C05`, and `S03 → A02 → A03 → A04 → B03`. Protect these. If a critical task is late, the next free person pairs on it.

## Cut list (cut from the top when behind; never cut the "keep" list)

1. D07 posters: print one or two by hand instead.
2. S09 CI.
3. The Resolved tab (B03 part): show only Active.
4. Manual overrides of category and Severity (A05 and B04 parts).
5. Summary refresh (C04): keep the first Report's summary.
6. Hand over (A05 and B04 parts): the duty manager handles routing in the database.
7. Needs review release with a Team (A09): release without a Team.
8. W04 and B06 polish beyond accessibility basics.

**Keep, whatever happens:**
- Emergency handling
- Bounce with feedback
- rate limits with silent quarantine, and Needs review
- unknown QR code handling
- Team routing and visibility
- LLM grouping with "N people reported"
- Knowledge base answers
- fallbacks when the LLM fails
- simulated-data labels

## Definition of done (every task)

- [ ] **Done when** items met, tests pass, `main` still builds.
- [ ] Glossary terms used. New terms added to `GLOSSARY.md`.
- [ ] Both RO and EN strings exist for any UI text (`node scripts/check-i18n.mjs`).
- [ ] No secrets, no real Passenger data, no new dependency outside [ADR-0013](../adr/0013-tooling-and-dependencies.md).
- [ ] Docs, ADRs and this board updated if the plan or behaviour changed.
