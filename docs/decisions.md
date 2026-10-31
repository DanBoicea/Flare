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

## Open questions

### Waiting for the team's answer

- **Lombok:** the skeleton includes it, but ADR-0013 planned Java records instead. Keep it or remove it (S05).
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
