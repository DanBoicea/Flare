# Decisions and open questions

The log of what the team decided in grilling sessions, plus what is still open. Significant decisions get an ADR in [adr/](adr/). Small ones are a line here. When a decision changes, add the change here and supersede the ADR rather than editing it.

## ADRs

| ADR | Decision |
| --- | --- |
| [0001](adr/0001-bounce-garbage-messages.md) | Garbage, joke and spam messages are Bounced back to the Passenger |
| [0002](adr/0002-three-apps-spring-boot-supabase.md) | Three apps: React passenger web app, React Native Staff app, Spring Boot back end with Supabase |
| [0003](adr/0003-llm-only-behind-spring-boot.md) | Only Spring Boot talks to the LLM, and the LLM is Claude |
| [0004](adr/0004-single-free-text-field.md) | The passenger web app is one free-text field, not category buttons |
| [0005](adr/0005-knowledge-base-answers-questions.md) | Questions are answered from an airport Knowledge base instead of going to staff |
| [0006](adr/0006-location-specific-qr-codes.md) | Every QR code belongs to one Location, which only staff see |
| [0007](adr/0007-llm-grouping-and-summaries.md) | Grouping and staff summaries are done by the LLM |
| [0008](adr/0008-individual-staff-accounts-team-routing.md) | Individual staff accounts with Team routing from phase 1 |
| [0009](adr/0009-monorepo-independent-apps.md) | One repository, three independent apps, one contract folder |
| [0010](adr/0010-supabase-postgres-and-auth-only.md) | Supabase for the database and staff sign-in only; all data goes through Spring Boot |
| [0011](adr/0011-staff-app-polling.md) | The Staff app polls every 5 seconds |
| [0012](adr/0012-contract-first-openapi.md) | Contract-first OpenAPI, generated TypeScript types, hand-written Java DTOs |
| [0013](adr/0013-tooling-and-dependencies.md) | Tooling and the full dependency list |
| [0014](adr/0014-single-classification-call-with-knowledge-base.md) | One LLM call classifies and answers Questions; whole Knowledge base in the prompt |
| [0015](adr/0015-single-instance-in-memory-limits.md) | One back-end instance; in-memory limits and a single-threaded grouping worker |
| [0016](adr/0016-llm-model-per-task-in-config.md) | Model and effort per LLM task in config (proposed until the eval run) |
| [0017](adr/0017-qr-code-url-format.md) | QR codes open `/q/{qrCodeId}` with a random opaque ID |
| [0018](adr/0018-hashed-device-and-network-keys.md) | Device and network identifiers stored only as keyed hashes |
| [0019](adr/0019-bot-check-without-third-party.md) | Bot check is a honeypot and a minimum fill time, no third-party service |
| [0020](adr/0020-design-language-airfield-signage.md) | One design language: airfield signage, Atkinson Hyperlegible Next (superseded by 0021) |
| [0021](adr/0021-design-v1-navy-cobalt-instrument-sans.md) | Design V1 (navy and cobalt, self-hosted Instrument Sans) replaces airfield signage |

## Decided

### Round 1: product (2026-10-30)

- **Name:** Flare. "Squawk" is dropped.
- **Time:** about one full day plus a few hours (1-2 hours on the first evening, a few on Sunday). Pitch on Sunday.
- **Track:** not decided. Pitch is handled later.
- **Severity scale and categories:** tentative, in [AI pipeline](triage-and-dedup.md). They will probably change.
- **Anonymity:** anonymous by default, optional contact details.
- **Resolution:** hotline model. Staff or management resolve. Passengers do not confirm, and there is no Passenger "reopen" button.
- **Languages:** Romanian and English at minimum.
- *Superseded:* "phase 1 sends everything to one shared employee app" was replaced by ADR-0008. "Big category buttons" was replaced by ADR-0004.

### Round 2: mentor feedback (2026-10-30)

Mentor feedback, verbatim: "De adresat partea de impersonare qr code; Marketingul; Cum limitează Nr de req. Calitatea requesturilor".

- **Scope:** a minimal demonstrable slice of QR code impersonation detection, request limiting and request quality is must-have, tackled in the implementation and in the pitch.
- **QR code impersonation:** we say plainly that software cannot stop a sticker over a real QR code. Proper signage plus security measures. See [security and abuse](security-and-abuse.md).
- **Request limiting:** three layers (per device and per QR code, a global per-QR-code ceiling, a bot check). Over-limit Reports are Quarantined, not rejected.
- **Marketing:** passenger adoption, airport buy-in and pitch positioning, with the emphasis on passenger adoption and convenience and on pitch positioning. See [marketing](marketing.md).
- **Request quality:** the LLM Bounces garbage, joke and spam messages with clear feedback, and the Passenger rewrites them (ADR-0001).

### Round 3: technical grilling, part 1 (2026-10-30)

- **Stack:** a React passenger web app, a React Native Staff app, a Spring Boot back end and Supabase (ADR-0002). Team machines: 2 MacBooks, 1 Windows laptop.
- **Demo hosting and running:** the team handles it.
- **LLM:** Claude, called only through Spring Boot (ADR-0003). The team pays API usage from its own credits for the proof of concept.
- **Git:** GitHub Flow, with `main` and `feature/<name>` branches only.
- **ADRs:** created and updated throughout development whenever something is decided or changes.
- **Vocabulary:** in [GLOSSARY.md](../GLOSSARY.md). "Ticket" means only a plane ticket.
- **Passenger web app:** one free-text field (ADR-0004), an emergency strip, a language button at the top right, and the default language taken from the device.
- **Questions** are answered from the airport Knowledge base, and the Passenger can still send them to staff (ADR-0005).
- **QR codes** are specific to one Location, with a Location label on each poster. Only staff see the Location (ADR-0006).
- **Grouping and summaries** are done by the LLM. The Staff app shows "N people reported" (ADR-0007).
- **Staff accounts** are individual, with Team routing from phase 1 (ADR-0008).
- **Status page:** none. The Passenger gets a confirmation only.
- **Retention:** contact details are deleted 30 days after the Issue is resolved. This is an assumption for the demo.
- **UI:** clean, clear, easy for elderly and non-technical people, and must not look AI-generated.
- **Testing:** pure logic test-first, an evaluation script on the labelled Romanian and English set, and no browser end-to-end tests.
- **Code rules:** [code-rules.md](code-rules.md).

### Round 4: domain follow-ups (2026-10-31)

- **Report vs Issue confirmed:** a Report is one Passenger's message; an Issue is the real-world problem that one or more Reports describe.
- **Deterministic rules:** only three things stay outside the LLM:
  - a short Romanian and English emergency word list that shows 112 instantly
  - the LLM-failure default (the Report becomes Unclassified and goes to the duty manager)
  - cheap checks before any LLM call (empty or repeated text, rate limits, bot check)
  
  Everything else is decided by the LLM.
- **Lost luggage:** the anonymised Ticket code idea is dropped. A missing bag is handled at the airline's baggage desk using the bag tag. Flare treats "my bag didn't arrive" as a Question and answers it from the Knowledge base, so Ticket data never passes through Flare. Luggage problems that staff can act on, such as an unattended bag, a broken belt or missing trolleys, stay normal Reports. A heads-up to a handling-agent Team is parked in [ideas](ideas.md).
- **Teams:** Cleaning, Maintenance, Security and Passenger assistance. A staff member can belong to several Teams. Duty manager is a role, not a Team (ADR-0008, amended).
- **Routing safeguards:**
  - Critical Issues are always visible to Security and the duty manager.
  - Unclassified, "Other" and low-confidence Issues go to the duty manager.
  - Staff can hand an Issue over to another Team.
  - Only the duty manager sees Needs review.
- **ADR amendments:** an ADR may be corrected in place only while nothing is built on it. After that, write a new ADR that supersedes it.
- **Planning started:** architecture in [architecture/](architecture/), tasks in [tasks/](tasks/). The technical choices made there are ADRs 0009 to 0019 with status `proposed`, waiting for the team's confirmation.

### Round 5: technical ADRs confirmed (2026-10-31)

- **Accepted:** ADRs 0009-0015 and 0017-0019. **0016** (model per task) stays proposed until the eval run (C06).
- **Project skeleton:** a colleague created the three apps. The docs now use their real names: `apps/flare-backend` (Spring Boot 4.1.2, Java 21, package `com.fsociety.flarebackend`), `apps/flare-webapp` (Vite + React + TypeScript) and `apps/flare-mobile` (Expo 58, Expo Router, routes in `src/app/`).
- **Bot check:** no third-party bot detection services (ADR-0019 confirmed).
- **`qrcode` npm package approved** for the poster generator (D07).
- **Romanian first:** both apps default to Romanian, and switch to English only when the device language is English. The team's Romanian speaker reviews all Romanian text.

### Round 6: build-time changes (2026-10-31)

- **Staff roles many-to-many:** roles move from the `staff_member.role` column to a `role` table and a `staff_member_role` link table, like Teams, so a staff member can hold several roles. `/api/staff/me` returns `roles` (an array) instead of `role`. Passwords stay in Supabase Auth only, never in our tables (ADR-0010). See [data model](architecture/data-model.md) and ADR-0008 (amended in place; nothing was built on it yet).
- **Lombok kept, used instead of records** (A01) for DTOs, config and value classes. No `@Data` or `@ToString` on classes holding Passenger text. ADR-0013 amended in place, since no code used records yet.
- **Main class keeps the skeleton name** `FlareBackendApplication` (backend.md said `FlareApplication`).
- **Request bodies over 16 KB, or chunked bodies without a Content-Length, get `413` with code `INVALID_REQUEST`** (A01). Browsers always send a Content-Length for `fetch` bodies.
- **`POST /api/public/messages` is a validating stub that returns `501`** until A06.
- **Contract `Me` uses `roles`** (an array of `StaffRole`) instead of `role`, matching the many-to-many roles (fixed before B01 generates types).
- **Staff-member check is central** (A03): an interceptor on `/api/staff/**` rejects valid tokens without an active `staff_member` row with `403 NOT_A_STAFF_MEMBER`, so individual endpoints can't forget it.
- **Staff app icons use `react-native-svg`** (B01), so both apps share the design's icon shapes. The Atkinson font is deferred to B06, as W01 deferred it to W04; both apps use the system font until ADR-0020 is accepted.
- **Staff app is light theme only** (design foundations, colour rule 6), so the template's colour-scheme hooks were removed.
- **Staff session storage is plain AsyncStorage** (B02), as in Supabase's current Expo example. The encrypted variant from Supabase's tutorial needs three unapproved packages; it can come later if a real deployment needs it.

### Round 7: passenger contact and design (2026-10-31)

- **Contact details are a phone number only.** Staff call back; nobody replies by email, so the form never asks for an email address. The field is `type="tel"`, optional, and limited to 32 characters in the web app.
- **Design direction is being chosen by testing three directions with persona reviews** (see [design](design/README.md)). The result is recorded in ADR-0020.

### Round 8: Design V1 (2026-10-31)

- **Design V1 replaces airfield signage** (ADR-0021). The mock-ups in [design/flare-design-v1](design/flare-design-v1/README.md) are the visual source of truth.
- **Instrument Sans is self-hosted**, never loaded from Google Fonts (privacy).
- **Compose uses a call-back checkbox** that reveals the phone field; unticked sends no contact. The title is the field's label. Sending echoes the Passenger's text.
- **Enter sends the message** on Compose (the keyboard key says "Send"); Shift+Enter adds a new line.
- **Open for B01:** distinct colours for High, Medium and Low severity in V1.
- **Emergency strip reworded** for clarity: "Urgență?" on the left, a red pill button "Sună la 112" with a phone icon on the right (a bare "112" box read like a badge).
- **Outcome screens (W03) follow V1 with three changes:** Confirmation keeps "Send another message" as a secondary button and keeps the copy-deck wording (V1's "the right team got it on their phone" is untrue for Quarantined "Send anyway" Reports); the Emergency title shows in both languages and keeps "tell the nearest staff member"; each Bounce reason gets a short title plus a body.
- **W04 design pass:** shorter header, plain question above the answer card, field borders at 3:1 or more, a page title per screen, and no sentence-initial "Ia" in Passenger copy (Instrument Sans draws I and l alike).
- **Web tests:** Vitest + Testing Library and Playwright (mock mode, RO and EN, phone, 320 px and desktop) in the passenger web app. Replaces "no browser end-to-end tests" in code-rules; ADR-0013 amended.
- **Language button:** flag plus code ("RO" / "EN") instead of the language name. Replaces "never flags" in foundations.
- **Wide screens:** the passenger app is one centred, framed column.
- **Mock answers follow `uiLanguage`**, like the real back end. An answer keeps its own `lang` if the Passenger switches language afterwards.
- **Answer not found:** the second button is "Send another message" ("That helped" makes no sense when nothing was found).

### Round 8: simulated data and demo scenarios (2026-10-31)

- **D01 split in two:** `data/airport/airport.json` was written first so D02 and D03 could use the real Location labels; the SQL seed followed with A02. `airport.json` adds `qrCodeStatus` per Location and a `_comment` header. The muted demo QR code is at `LND-ENT-03`.
- **Knowledge base places:** places come from `airport.json`. Details not in it (the baggage services desk near belt 2, the Wi-Fi name, the smoking area, water fountains, trolley spots) are invented and marked "(simulat)" / "(simulated)" in the text. Some topics are left out on purpose so the demo can show an honest "I don't know" (listed in `data/knowledge-base/README.md`).
- **Test set: category of an Emergency (assumption, to be confirmed by the Track C owner):** medical → `ASSISTANCE_NEEDED`, missing child → `LOST_ITEM_OR_PERSON`, fire, smoke or threat → `SAFETY_OR_SECURITY`. Routing does not depend on it. The test set also adds `expected.language`.
- **Demo scenarios run the spam wave last,** and a full run needs a back-end restart first, because every simulated Passenger shares one laptop's network limit. Raising the network limit in the `local` profile is suggested to the A07 owner (see A07's notes).

## Open questions

### Waiting for the team's answer

- **Track owners:** who takes Track A, B and C, and who runs the OpenCode agents (see [tasks](tasks/README.md)).

### Also open

- **Quality check guardrails:** the proposed list in [AI pipeline](triage-and-dedup.md), including "Send anyway" after 3 Bounces. The architecture assumes they are accepted.
- **Location mismatch:** show staff both "Scanned at" and "Mentioned"? The architecture assumes yes.
- **Quick-start suggestions** under the text field, to help Passengers who struggle to type (stretch).
- **Passengers without a data connection.**
- **Who physically audits QR codes** (stretch).
- **Who owns airline and handling-agent Issues** (stretch).
- **What to measure in the demo:** the proposed metrics are in [AI pipeline](triage-and-dedup.md).
- **What staff carry:** phones are assumed for the Staff app. Confirm.
- **Marketing:** whether the mentors meant something narrower than our reading.
- **Pitch description:** update it for the knowledge base, the Teams, spam and quality handling, and the QR code defence.

## Pitch entry-page description (drafted 2026-10-30)

EN: Flare lets airport passengers report a problem by scanning a QR code and filling in a 30-second form, with no app and no account. Reports are triaged by severity, and repeated reports of the same problem are grouped into one prioritised issue on a live staff dashboard. Designed for stressed passengers and non-technical staff, in Romanian and English, and demonstrated with simulated data for Craiova International Airport.

RO: **Flare** le permite pasagerilor din aeroport să raporteze o problemă scanând un cod QR și completând un formular de 30 de secunde, fără aplicație și fără cont. Rapoartele sunt triate după gravitate, iar sesizările repetate despre aceeași problemă sunt grupate într-un singur incident prioritizat, pe un panou live pentru personal. Gândit pentru pasageri stresați și angajați fără pregătire tehnică, în română și engleză, și demonstrat cu date simulate pentru Aeroportul Internațional Craiova.

Out of date: it predates the Knowledge base, the Staff app for Teams, and the spam, quality and QR code handling.
