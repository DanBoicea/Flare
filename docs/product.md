# Product

What Flare is, who uses it, what we build for the hackathon, and how it behaves. Related: [principles](principles.md), [security and abuse](security-and-abuse.md), [triage and dedup](triage-and-dedup.md), [decisions](decisions.md).

## One-line summary

A passenger scans a QR code in the airport, describes a problem in a very simple web form, and staff see it in real time on a dashboard that sorts by severity and groups duplicates. Routing to specific teams comes in a later phase.

## Problem

- Passengers who notice a problem (broken lift, dirty or blocked toilet, spill, unclear sign, assistance that never arrived, a lost item or person, a safety hazard) usually do not report it. They do not know who to tell, and walking to a desk costs time.
- Staff learn about problems late, through scattered channels, and the same issue gets reported many times while others are missed.

## Users

- **Passenger:** any language, possibly stressed, in a hurry, using one hand, possibly elderly or with low digital skill. No app install, no account, no sign-up.
- **Staff (phase 1):** one shared employee app. Everyone sees everything.
- **Staff roles (later):** cleaning, maintenance, security, passenger assistance and information desk, duty or operations manager. Possibly airline or handling-agent staff (stretch). The team decides roles later.
- **Admin (later):** manages QR codes, locations, teams and rules.

## Scope for the hackathon (decided)

Time budget: about one full day plus a few hours (1-2 hours on the first evening, a few more on Sunday). The pitch is on Sunday.

**Must-have**

- Passenger form
- Triage
- Duplicate grouping
- User-friendly staff dashboard
- Mentor-driven slice, added 2026-10-30 (see [decisions](decisions.md)):
  - unknown or muted QR code ID is flagged to staff
  - rate limiting with a quarantine queue
  - request quality gate (garbage, joke and spam reports are bounced back to the passenger)

**Stretch (handled later)**

Photo upload, admin QR inventory, management trends view, shift handover, role-based views and routing, airline and handling-agent roles, passenger status page, and anything else not listed above. See [ideas](ideas.md).

Categories and the severity scale are a starting point and will probably change. Keep them in one place in the code so they are cheap to change.

## Core flow

1. The passenger scans a code. The form opens, and the location is already known from the code.
2. They pick from a few large options or write free text, optionally leave contact details (never required), and optionally add ticket details for luggage-type issues (see below).
3. Cheap deterministic checks run first: rate limits, spam patterns, emergency keywords.
4. The **quality gate** checks that the report is a genuine airport-related issue. If it is garbage, a joke or spam, the passenger is told clearly why it was not sent and is asked to rewrite it. When the rewritten report passes the check, it goes on (see [triage and dedup](triage-and-dedup.md)).
5. The report is triaged: category, severity, short summary, language, suggested responsible team.
6. Duplicates are grouped. Many reports of the same problem in a short time become one issue with a count, which raises its priority.
7. Staff see the live dashboard with one-tap actions: acknowledge, assign, resolve.
8. The passenger gets a confirmation that explains what happens next. Whether they see later status updates is undecided (see [decisions](decisions.md)).

The exact order of the steps is a planning decision. Emergency handling must never be blocked by any later step.

## Passenger form requirements

- No account, no app, no mandatory personal data.
- Location pre-filled from the code, with a quick correction option.
- A few big category buttons plus an optional free-text box. Photo is stretch.
- Clear feedback when a report is bounced by the quality gate: what is wrong, what to change, and that the text they typed is kept so they can edit it.
- Immediate confirmation that explains what happens next. A separate status page is not confirmed.
- Works on slow connections and small screens. Romanian and English at minimum.

## Staff app, phase 1 (decided)

- Every report goes to one shared employee app. There is no per-team routing, role-based filtering or role login yet.
- Triage still suggests a responsible team, but the suggestion is only displayed. It does not decide who sees the report.
- Roles, teams and role-based views come in a later phase.

## Staff dashboard requirements

- Live updates without refreshing, clear but non-annoying new-issue signals.
- Phase 1: one shared view of all issues, filterable by severity, status and location. Team filters and role-based default views come later.
- One-tap acknowledge, assign and resolve. Notes, photo view and shift handover are stretch.
- Big text, minimal jargon, no complex tables. Works on phone, tablet and desktop.
- Quarantined reports and unknown-code flags are visible in a separate place so they do not pollute the main list.
- The dashboard explains why an issue is ranked where it is (for example "12 reports in 10 minutes").

## Resolution model (decided)

- Flare works like a hotline: the passenger reports and is done. Passengers are not expected to confirm, mark or follow up on resolution.
- Employees or management mark issues acknowledged, assigned and resolved.
- There is no "still a problem?" passenger button.
- Staff can reopen an issue, or a new report about the same problem can reopen or re-prioritise it through dedup.

## Ticket and luggage details (decided in principle, details open)

- For issues like lost luggage, the passenger can optionally provide ticket or boarding pass details, ideally by scanning the code on the ticket instead of typing.
- If there is no ticket or the scan fails, there must be a fallback way to give the minimum details. The team asked for this to be designed.
- GDPR constraint: a ticket holds personal data (for example name and booking reference). Provide only the minimum needed, never make it mandatory, say clearly what is stored and why, and set a short retention period. Prefer extracting only the needed fields (for example flight number or baggage tag) over storing an image of the ticket.
- This conflicts with the rule that the system never asks for sensitive personal data unless it stays optional and minimal. The team must confirm how to reconcile them (open question).
- Flight data integration is out of scope, so any scanned value is stored as text and shown to staff.

## Out of scope for the hackathon

A native mobile app, integration with real airport systems, payments, real emergency dispatch, real deployment, flight data, passenger accounts, two-way chat beyond status updates.

## Demo plan

- Simulated airport layout and fictional data, clearly labelled as simulated.
- Scripted scenarios:
  - a single report triaged
  - a burst of duplicates merged into one escalated issue
  - a garbage or joke report bounced, then rewritten and accepted
  - a spam wave throttled into quarantine
  - an unknown or fake code flagged
  - an emergency phrase showing emergency instructions
  - Romanian and English reports
- Prepare a fallback recording and cached data in case the live demo fails.
