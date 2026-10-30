# Security and abuse

The threats the team considers and the mitigations it intends. Implementation choices are open and belong to the planning phase. The mentors raised QR impersonation, request limiting and request quality as points to address (see [decisions](decisions.md)).

## Built in the demo vs described in the pitch

| Area | Built in the demo (must-have) | Described in the pitch (design) |
| --- | --- | --- |
| QR impersonation | Unique ID per code. Unknown or muted ID is flagged to staff. | Signage, printed short URL, tamper-resistant placement, routine staff checks, admin inventory |
| Request limiting | Per-device and per-code caps, global per-code ceiling, bot check, quarantine queue | Stricter attack mode, daily caps, admin mute |
| Request quality | LLM quality gate with feedback to the passenger (see [triage and dedup](triage-and-dedup.md)) | Accuracy measured on a labelled RO/EN test set |

## 1. Fake QR codes (quishing)

Criminals place sticker QR codes over real ones in public places; this is documented on parking meters in several countries. **A backend cannot prevent a sticker that points to a different website.** We say so plainly and present Flare as defence in depth.

- **Official domain only** and the short URL printed beneath every code so people can check it.
- **Signage** next to every code, in Romanian and English: the page never asks for payment, passwords or personal data. The team called proper signage a very good solution alongside the technical measures. Draft text is in [marketing](marketing.md).
- **Tamper-resistant placement** and a **routine physical check** of codes by staff (stretch: who does this is an open question).
- **A unique ID per code**, with an admin inventory (stretch). In the demo, a scan from an unknown or muted ID is flagged to staff instead of silently accepted. Anomaly alerts for odd scan patterns are stretch.
- **Low value for attackers:** the form requests nothing sensitive, so impersonating it gains little. Optional ticket details are the exception and need extra care (see [product](product.md)).

To design at planning time: how code IDs are issued and checked (for example unguessable random IDs vs signed ones), and what a flagged scan looks like to staff and to the passenger.

## 2. Spam and flooding: limiting the number of requests

Three layers (decided). Numbers are demo values and labelled as assumptions.

1. **Per device and per code:** a small cap on reports per short window, for example a few per 10 minutes per device.
2. **Global per-code ceiling:** if one code suddenly gets a flood, flag a possible attack and move that code to a stricter mode, or let an admin mute it.
3. **Bot check:** one lightweight challenge. Which one depends on the stack.

**Over-limit reports are accepted and put in a quarantine queue** that staff can review. They are not rejected, and the exact thresholds are not revealed to the sender. Quarantined reports never reach the main dashboard. Duplicate grouping counts independent reporters only, so one device repeating itself cannot inflate priority.

Quality-gate retries count as attempts. Each retry can cost an LLM call, so they are limited per device as well.

## 3. Reports from people who are not at the airport

The code ID is the primary location signal. Device location is only a soft signal because it can be spoofed and is unreliable indoors. Combine signals into a trust score. Never hard-block a report on location alone.

## 4. Prompt injection through free text

- Treat all passenger text as untrusted data. Passenger text is never mixed into privileged instructions.
- LLM output must follow a fixed structure, and the LLM cannot take actions. This applies to the quality gate and to triage.
- The staff interface must escape or sanitise displayed content.

## 5. Malicious uploads (stretch with photos)

Validate and size-limit photos, strip metadata, accept no executable content.

## 6. Abusive content

The quality gate rejects most abusive, joke and garbage content up front. Anything abusive that still gets through is hidden from staff by default, with a way to review it.

## 7. Dashboard access

Staff authentication by role, least privilege, and an audit log. In phase 1 there is a single shared staff view, so how staff sign in for the demo is a planning decision.

## 8. LLM failure

A report is never lost because the LLM is unavailable. A basic fallback triage always exists, and the quality gate must not turn an outage into rejected reports (see the proposed handling in [triage and dedup](triage-and-dedup.md)).

## 9. Privacy

See [principles](principles.md).
