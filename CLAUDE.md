# Flare: passenger issue reporting for airports (working name)

> "Flare" is a working name and may change. In aviation, a squawk is a reported problem that needs attention.

## One-line summary

A passenger scans a QR code in the airport, describes a problem in a very simple web form, and staff see it in real time on a dashboard that sorts by severity, groups duplicates, and routes each issue to the right team.

## Hackathon context

- City council and town hall hackathon with airport and aviation themes. Team of three.
- Target setting: Craiova International Airport. The demo uses simulated data. This is not a real deployment, and every piece of demo data must be labelled as simulated.
- Track: to be confirmed with the team (likely Control Tower; it also fits Ground Control).
- Nothing about the airport should be invented. If a fact is unknown, mark it as an assumption.

## Problem

- Passengers who notice a problem (broken lift, dirty or blocked toilet, spill, unclear sign, assistance that never arrived, a lost item or person, a safety hazard) usually do not report it. They do not know who to tell, and walking to a desk costs time.
- Staff learn about problems late, through scattered channels, and the same issue gets reported many times while others are missed.

## Users

- **Passenger:** any language, possibly stressed, in a hurry, using one hand, possibly elderly or with low digital skill. No app install, no account, no sign-up.
- **Staff roles:** cleaning, maintenance, security, passenger assistance and information desk, duty or operations manager. Possibly airline or handling-agent staff for issues they own (stretch goal).
- **Admin:** manages QR codes, locations, teams and rules.

## Core flow

1. The passenger scans a code. The form opens, and the location is already known from the code.
2. They pick from a few large options or write free text, optionally add a photo, and optionally leave contact details for follow-up (never required).
3. The submission is checked for abuse and spam, then triaged by an LLM: category, severity, short summary, language, suggested responsible team.
4. Duplicates are grouped. Many reports of the same problem in a short time become one issue with a count, which raises its priority.
5. Staff see a live dashboard, filtered to their role, with one-tap actions: acknowledge, assign, resolve.
6. The passenger sees honest status updates: received, seen, on its way, resolved.
7. Management gets a simple view of trends, hotspots and response times.

## Product principles (in priority order)

1. **Usability.** Passengers: done in about 30 seconds and a few taps, large touch targets, plain language. Staff: zero training, glanceable, role-based views, works on phone, tablet and desktop. Accessible (contrast, text size, screen readers). Romanian and English at minimum.
2. **Trust and transparency.** Tell the passenger what happens to their report and roughly when. Label AI involvement honestly. Never pretend to be a human. Set realistic expectations.
3. **Security and abuse resistance.** See the section below. The back end must not be flooded with spam, and fake QR codes must be treated as a real threat.
4. **Signal over noise.** Severity, deduplication and routing so staff see what matters first. Avoid alert fatigue.
5. **Simplicity.** A small feature set done well beats many half-finished features.

## Safety boundary

- This is not an emergency channel and must never present itself as one.
- The form shows the emergency number prominently. If a report suggests a medical emergency, security threat or missing child, show the passenger emergency instructions immediately and flag the report to staff as top priority. Do not give medical or security advice.
- The system never asks for payments, passwords or sensitive personal data.

## Security and abuse model

The team considers these threats and intends these mitigations. Implementation choices are open.

1. **Fake QR codes (quishing).** Criminals place sticker QR codes over real ones in public places; this is documented on parking meters in several countries. A backend cannot prevent a sticker that points to a different website. Mitigations: official domain only, the short URL printed beneath every code so people can check it, signage stating the page never asks for payment, passwords or personal data, tamper-resistant placement, a unique ID per code with an admin inventory, anomaly alerts for unknown IDs or odd scan patterns, and a routine physical check of codes by staff. Because the form requests nothing sensitive, impersonating it is worth little to an attacker.
2. **Spam and flooding.** Throttling per device, network and code, a bot challenge, daily caps, and a quarantine queue so suspicious submissions never pollute the main dashboard. A stricter mode during an attack, and the ability for admins to mute a code.
3. **Reports from people who are not at the airport.** The code ID is the primary location signal. Device location is only a soft signal because it can be spoofed and is unreliable indoors. Combine signals into a trust score. Never hard-block a report on location alone.
4. **Prompt injection through free text.** Treat all passenger text as untrusted data. LLM output must follow a fixed structure, the LLM cannot take actions, and passenger text is never mixed into privileged instructions. The staff interface must escape or sanitise displayed content.
5. **Malicious uploads.** Validate and size-limit photos, strip metadata, accept no executable content.
6. **Abusive content.** Hide offensive submissions from staff by default, with a way to review them.
7. **Dashboard access.** Staff authentication by role, least privilege, and an audit log.
8. **LLM failure.** A report is never lost or blocked because the LLM is unavailable. A basic fallback triage always exists.
9. **Privacy.** GDPR-minded. Collect minimal data, anonymous by default, limited retention, careful handling of photos that may contain faces, no tracking.

## AI triage rules

- Outputs: category, severity, short summary for staff, responsible team, confidence, spam or abuse likelihood, duplicate candidates.
- Deterministic rules override the LLM where the stakes are high (for example emergency keywords).
- Cheap checks run first, so obvious spam does not cost an LLM call.
- Staff can always override category and severity.
- Build a small labelled test set of realistic reports in Romanian and English and measure category accuracy, severity accuracy and abuse detection. Report the results honestly in the demo.

## Deduplication and prioritisation

- Same zone, same category, short time window and similar content become one issue with a count.
- Priority combines severity, number of independent reports, age and how critical the location is. The dashboard explains why (for example "12 reports in 10 minutes").
- Count independent reporters only. Allow staff to split or merge issues.
- Avoid over-merging distinct problems.

## Staff dashboard requirements

- Live updates without refreshing, clear but non-annoying new-issue signals.
- Filters by team, severity, status and location. Default view is based on role (cleaning sees cleaning issues).
- One-tap acknowledge, assign and resolve; notes; photo view; shift handover view.
- Big text, minimal jargon, no complex tables.

## Passenger form requirements

- No account, no app, no mandatory personal data.
- Location pre-filled from the code, with a quick correction option.
- A few big category buttons plus an optional free-text box and photo.
- Immediate confirmation that explains what happens next, and a status page for the report.
- Works on slow connections and small screens.

## Out of scope for the hackathon

- A native mobile app, integration with real airport systems, payments, real emergency dispatch, real deployment, flight data, passenger accounts, two-way chat beyond status updates.

## Demo plan

- Simulated airport layout and fictional data, clearly labelled.
- Scripted scenarios: a single report triaged; a burst of duplicates merged into one escalated issue; a spam wave throttled or quarantined; an unknown or fake code flagged; an emergency phrase showing emergency instructions; Romanian and English reports.
- Prepare a fallback recording and cached data in case the live demo fails.

## Differentiation for the pitch

- Designed for non-technical staff and stressed passengers.
- Built-in defence against fake QR codes and spam, which is rarely addressed.
- Transparent to passengers.
- Duplicate grouping that turns noise into priority.
- Romanian-first.
- Cheap pilot: a few printed codes and a web page, no hardware.
- Competing products already exist (for example AeroSimple, Sensfix, AirVote, Zello QR Assist, Fixalert). Do not claim novelty. Verify their features before making any comparison in the pitch.

## Working agreements for Claude

- The technology stack is undecided. Do not assume, add or install any framework, language, database, service or library unprompted. When a technical decision is needed, present options and wait for the team.
- The team will run a grilling session before building. Ask clarifying questions instead of guessing, and record decisions in the Decisions section below.
- Prefer simple, explainable logic over clever logic.
- Be honest about limits: location can be spoofed, LLMs make mistakes, and demo data is simulated.
- Never commit secrets.
- UI text in plain Romanian and English.
- Keep changes small and explain security-relevant choices.

## Decisions (to fill in)

- Name: TBD
- Stack: TBD
- Severity scale: TBD
- Staff roles and teams: TBD
- Languages: Romanian and English at minimum

## Open questions for the grilling session

- Exact categories and the severity scale, with examples for each level.
- Which staff roles exist at the airport, and what do they actually carry (phone, radio, tablet)?
- How should a passenger without a data connection or without a camera be handled?
- What is the policy for anonymous versus contactable reports?
- How are fake codes detected in practice, and who audits the physical codes?
- Who owns issues that belong to airlines or handling agents, and how are they handed off?
- What does "resolved" mean, and who confirms it?
- What is the minimum data we keep, and for how long?
- What will we measure in the demo to show it works?
