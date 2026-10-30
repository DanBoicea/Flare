# Decisions and open questions

Log of what the team decided in grilling sessions, plus what is still open. Update this file whenever a decision changes.

## Decided

### Round 1 (2026-10-30)

- **Name:** Flare. "Squawk" is dropped.
- **Time:** about one full day plus a few hours (1-2 hours on the first evening, a few on Sunday). Pitch on Sunday.
- **Track:** not decided. Pitch is handled later.
- **Must-have scope:** passenger form, triage, duplicate grouping, user-friendly staff dashboard. Everything else is stretch.
- **Severity scale and categories:** the tentative ones in [triage and dedup](triage-and-dedup.md). They will probably change.
- **Staff roles and teams:** deferred. Phase 1 sends everything to one shared employee app.
- **Anonymity:** anonymous by default, optional contact details. Optional minimal ticket details for luggage-type issues, with a fallback when there is no ticket.
- **Resolution:** hotline model. Employees or management resolve. Passengers do not confirm. No passenger "reopen" button.
- **Languages:** Romanian and English at minimum.

### Round 2: mentor feedback (2026-10-30)

Mentor feedback, verbatim: "De adresat partea de impersonare qr code; Marketingul; Cum limitează Nr de req. Calitatea requesturilor".

- **Scope:** a minimal demonstrable slice of QR impersonation detection, request limiting and request quality moves into must-have. The team also wants a way to tackle each in the implementation, not only in the pitch.
- **QR impersonation:** we say plainly that software cannot stop a sticker over a real code. Proper signage plus security measures (unique IDs, unknown-code flagging, printed URL, staff checks). Details in [security and abuse](security-and-abuse.md).
- **Request limiting:** three layers (per device and per code, global per-code ceiling, bot check). Over-limit reports are quarantined, not rejected. Details in [security and abuse](security-and-abuse.md).
- **Marketing:** all three readings apply (passenger adoption, airport buy-in, pitch positioning), with the emphasis on passenger adoption and convenience and on pitch positioning. See [marketing](marketing.md).
- **Request quality:** an LLM detects garbage, joke and spam reports. The sender gets clear negative feedback and is told to change the request to something relevant to the airport and flights. The garbage report is not sent through. When the rewritten report passes the LLM check, it goes to the back end and the employee app. Details and proposed guardrails in [triage and dedup](triage-and-dedup.md).

## Open questions

- **Quality gate guardrails (proposed, not yet confirmed):** emergency rules bypass the gate; fail open if the LLM is down; retries rate limited; false-rejection rate measured; a "send anyway for human review" option after repeated rejections; whether rejected text is stored.
- **Passenger status page:** none (confirmation only), or a light private link showing received, seen, resolved? The hotline model suggests confirmation only.
- **Ticket and luggage details:** exact fields, how to scan, the fallback, and how to reconcile with "never ask for sensitive personal data" and GDPR.
- **Camera and photos:** photo upload is stretch, but ticket scanning needs the camera.
- **Retention:** how long reports, contact details and ticket details are kept.
- **Passengers without a data connection or a camera.**
- **Who physically audits QR codes**, and how fake codes are detected in practice (stretch).
- **Who owns airline and handling-agent issues** (stretch).
- **What to measure in the demo.** Proposed: category accuracy, severity accuracy, spam and garbage detection, false-rejection rate.
- **Staff roles, teams, and what staff carry** (later phase).
- **Marketing:** whether the mentors meant something narrower than our reading.
- **Stack, hosting, LLM provider, demo setup:** planning phase, not started. See [technical](technical.md).

## Pitch entry-page description (drafted 2026-10-30)

EN: Flare lets airport passengers report a problem by scanning a QR code and filling in a 30-second form, with no app and no account. Reports are triaged by severity, and repeated reports of the same problem are grouped into one prioritised issue on a live staff dashboard. Designed for stressed passengers and non-technical staff, in Romanian and English, and demonstrated with simulated data for Craiova International Airport.

RO: **Flare** le permite pasagerilor din aeroport să raporteze o problemă scanând un cod QR și completând un formular de 30 de secunde, fără aplicație și fără cont. Rapoartele sunt triate după gravitate, iar sesizările repetate despre aceeași problemă sunt grupate într-un singur incident prioritizat, pe un panou live pentru personal. Gândit pentru pasageri stresați și angajați fără pregătire tehnică, în română și engleză, și demonstrat cu date simulate pentru Aeroportul Internațional Craiova.

Update this after the mentor changes: it does not yet mention spam and quality handling or the QR impersonation defence.
