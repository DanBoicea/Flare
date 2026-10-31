# Product

What Flare is, who uses it, what we build for the hackathon, and how it behaves. Terms are defined in [GLOSSARY.md](../GLOSSARY.md). Related: [principles](principles.md), [security and abuse](security-and-abuse.md), [AI pipeline](triage-and-dedup.md), [technical](technical.md), [decisions](decisions.md), [ADRs](adr/).

## One-line summary

A Passenger scans a QR code in the airport and types a problem or a question into one simple field. Questions get an instant answer from an airport Knowledge base. Problems go to the right Team's Staff app, grouped with other Reports of the same problem and sorted by Severity.

## Problem

- Passengers who notice a problem (broken lift, dirty or blocked toilet, spill, unclear sign, assistance that never arrived, a lost item or person, a safety hazard) usually do not report it. They do not know who to tell, and walking to a desk costs time.
- Staff learn about problems late, through scattered channels, and the same problem gets reported many times while others are missed.
- Many things Passengers would ask a desk are simple questions that do not need a staff member at all.

## Users

- **Passenger:** any language, possibly stressed, in a hurry, using one hand, possibly elderly or with low digital skill. No app install, no account, no sign-up.
- **Staff:** individual accounts. Each staff member belongs to one Team and sees only that Team's Issues. A duty manager sees all Issues ([ADR-0008](adr/0008-individual-staff-accounts-team-routing.md)). The Team list is still to confirm (proposed: Cleaning, Maintenance, Security, Passenger assistance and information).
- **Admin (later):** manages QR codes, Locations, Teams and the Knowledge base.

## Scope for the hackathon

Time budget: about one full day plus a few hours (1-2 hours on the first evening, a few more on Sunday). The pitch is on Sunday.

**Must-have**

- Passenger web app (React): one text field, emergency strip, language button ([ADR-0004](adr/0004-single-free-text-field.md)).
- LLM classification of each message: Report, Question or Bounced, and for Reports the category, Severity, Team and summary.
- Knowledge base answers to Questions ([ADR-0005](adr/0005-knowledge-base-answers-questions.md)).
- Grouping of Reports into Issues by the LLM, with "N people reported" counts and summaries ([ADR-0007](adr/0007-llm-grouping-and-summaries.md)).
- Staff app (React Native) with individual accounts and Team routing ([ADR-0008](adr/0008-individual-staff-accounts-team-routing.md)).
- Mentor-driven slice:
  - unknown or muted QR code flagged to staff
  - rate limiting with a Quarantined queue
  - the quality check that Bounces garbage ([ADR-0001](adr/0001-bounce-garbage-messages.md))

**Stretch (handled later)**

Photo upload, admin QR code inventory, management trends view, shift handover, airline and handling-agent Teams, push notifications, quick-start suggestions under the text field, staff split and merge of Issues. See [ideas](ideas.md).

**Time risk.** The must-have list is large for about one day with three apps. Planning must start with one thin end-to-end slice (message in, Issue visible in the Staff app) and keep an explicit cut list.

Categories and the Severity scale are a starting point and will probably change. Keep them in one place in the code so they are cheap to change.

## Core flow

1. The Passenger scans a QR code at a Location. The passenger web app opens. The QR code identifies the Location ([ADR-0006](adr/0006-location-specific-qr-codes.md)), but the Passenger is never shown it.
2. They type one message, a problem or a question, and can optionally leave contact details.
3. Cheap checks run: unknown or muted QR code, rate limits, bot check.
4. The message is classified:
   - **Emergency:** emergency instructions are shown at once, and a Critical Issue goes to Security and the duty manager.
   - **Question:** the Passenger gets an answer from the Knowledge base, with "Send to staff anyway" available.
   - **Garbage, joke or spam:** the message is Bounced, and the Passenger is asked to rewrite it so it is relevant to the airport or their flight.
   - **Report:** continues to step 5.
5. The Report joins an existing Issue or starts a new one. The Issue gets a category, Severity, Team and an LLM summary.
6. Members of that Team see the Issue live in the Staff app, with the count and summary, and can acknowledge, take, or resolve it.
7. The Passenger sees a confirmation that explains what happens next. There is no status page.

The exact order of the checks is a planning decision. Emergency handling is never blocked by any other step.

## Passenger web app requirements

- No account, no app install, no mandatory personal data.
- An emergency strip at the top of every screen: "Emergency? Call 112. This is not an emergency line." in Romanian and English.
- A language button at the top right. The default comes from the device's language when it is Romanian or English. Proposed: English when it is neither.
- One text field, with the placeholder "Report an issue in the airport or ask for help..." (RO: "Raportează o problemă în aeroport sau cere ajutor..."). Dictation works through the phone keyboard.
- The Location is not shown. It comes from the QR code.
- Optional contact details, collapsed by default and clearly marked optional.
- Answers to Questions are labelled as automatic answers and always offer "Send to staff anyway".
- A Bounced message gets clear, polite feedback on what to change, and the Passenger's text is kept so they can edit it.
- The confirmation says the message was received and what happens next. There is no status page (decided).
- Works on slow connections and small screens. Designed first for elderly and non-technical people. Clean, calm UI that does not look AI-generated.

## Staff app requirements

- React Native mobile app ([ADR-0002](adr/0002-three-apps-spring-boot-supabase.md)). Each staff member signs in to their own account and sees their Team's Issues ([ADR-0008](adr/0008-individual-staff-accounts-team-routing.md)).
- A live list of Issues, sorted by priority. Each card shows:
  - Severity, using colour, icon and word together
  - the LLM summary, labelled AI
  - the Location from the QR code, plus any differing place mentioned in the message
  - "3 people reported..."
  - the age of the Issue
  - one-tap actions
- The original Reports are one tap away, with their text always escaped.
- Actions: acknowledge, "I'm on it" (takes the Issue under your name), resolve, override category or Severity. Proposed: hand an Issue to another Team.
- Tabs: Active, Needs review (Quarantined Reports and unknown QR code flags; who sees this tab is still open) and Resolved.
- A subtle signal for new Issues. Sound only for Critical.
- Big text, minimal jargon, zero training needed.

## Resolution model (decided)

- Flare works like a hotline: the Passenger reports and is done. Passengers are not expected to confirm, mark or follow up on resolution.
- Staff or the duty manager mark Issues acknowledged, taken and resolved.
- There is no "still a problem?" button for Passengers.
- Staff can reopen an Issue, and a new Report about the same problem shortly after resolution can reopen it through grouping.

## Lost luggage and Tickets (under review)

- **The original idea:** let a Passenger attach an anonymised code tied to their Ticket, so staff can connect a lost-luggage Report to the actual bag without seeing the whole Ticket.
- **How airports usually handle it** (general industry practice, not verified for Craiova):
  - A missing checked bag is reported at the baggage services or lost-and-found desk in the baggage claim area. That desk is usually run by the airline or its ground handling agent, not by airport operations.
  - The Passenger fills in a property irregularity report there, using the bag tag number on the receipt they got at check-in.
  - The bag is traced through the airlines' own systems. WorldTracer is the widely used one.
- **What that means for Flare:** matching a Report to a bag needs airline systems, which are out of scope. The proposal is to treat "my bag didn't arrive" as a Question and answer it from the Knowledge base: which desk to go to, before leaving the baggage area, and what to bring. Ticket data would then never pass through Flare. Decision pending, see [decisions](decisions.md).
- If any Ticket data is kept, it must be optional and minimal, and it is never a photo of the Ticket.

## Out of scope for the hackathon

A native passenger app, integration with real airport or airline systems, payments, real emergency dispatch, real deployment, flight data, Passenger accounts, two-way chat beyond the Knowledge base answer and the confirmation.

## Demo plan

- Simulated airport layout, Locations, Knowledge base and staff accounts, all clearly labelled as simulated.
- Scripted scenarios:
  - a single Report classified and routed to the right Team
  - three "no toilet paper" Reports grouped into one Issue ("3 people reported"), while "the toilet is dirty" at the same toilet stays a separate Issue
  - a Question answered from the Knowledge base, then sent to staff anyway
  - a joke message Bounced, rewritten and accepted
  - a spam wave throttled into Quarantined
  - an unknown or fake QR code flagged
  - a message about a different place than the scanned Location
  - an emergency phrase showing emergency instructions
  - Cleaning and Security accounts seeing different Issues
  - Romanian and English messages
- Prepare a fallback recording and cached data in case the live demo fails.
