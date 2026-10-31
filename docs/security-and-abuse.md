# Security and abuse

The threats the team considers and the mitigations it intends. The hackathon mentors raised QR code impersonation, request limiting and request quality as points to address (see [decisions](decisions.md)).

## Built in the demo vs described in the pitch

| Area | Built in the demo (must-have) | Described in the pitch (design) |
| --- | --- | --- |
| QR code impersonation | A unique ID per QR code, mapped to one Location ([ADR-0006](adr/0006-location-specific-qr-codes.md)). Unknown or muted IDs are flagged to staff. | Signage, the printed short URL, tamper-resistant placement, routine staff checks, an admin inventory |
| Request limiting | Per-device and per-QR-code caps, a global per-QR-code ceiling, a bot check, the Quarantined queue | A stricter attack mode, daily caps, admin mute |
| Request quality | The LLM quality check with feedback to the Passenger ([ADR-0001](adr/0001-bounce-garbage-messages.md)) | Accuracy measured on a labelled Romanian and English test set |

## 1. Fake QR codes (quishing)

Criminals place sticker QR codes over real ones in public places; this is documented on parking meters in several countries. **A back end cannot prevent a sticker that points to a different website.** We say so plainly and present Flare as defence in depth.

- **Official domain only,** with the short URL printed beneath every QR code so people can check it.
- **Signage** next to every QR code, in Romanian and English: the page never asks for payment, passwords or personal data. The team considers proper signage a very good measure alongside the technical ones. Draft text is in [marketing](marketing.md).
- **Location label** printed beneath each QR code, so staff place posters correctly and can spot a poster in the wrong place.
- **Tamper-resistant placement** and a **routine physical check** of QR codes by staff (stretch; who does it is an open question).
- **A unique ID per QR code,** mapped to one Location. Proposed: the URL carries an opaque ID, never the Location name, so editing the URL cannot fake a Location. A scan with an unknown or muted ID is flagged to staff instead of being silently accepted. Anomaly alerts for odd scan patterns are stretch.
- **Low value for attackers:** the page asks for nothing sensitive, so impersonating it gains little.

## 2. Spam and flooding: limiting the number of requests

Three layers (decided). The numbers are demo values, labelled as assumptions.

1. **Per device and per QR code:** a small cap on messages per short window, for example a few per 10 minutes per device.
2. **Global per-QR-code ceiling:** if one QR code suddenly gets a flood, flag a possible attack and move that QR code to a stricter mode, or let an admin mute it.
3. **Bot check:** one lightweight challenge. Which one is chosen at planning.

**Over-limit Reports are accepted and Quarantined** for staff review. They are not rejected, and the thresholds are not revealed to the sender. Quarantined Reports never reach a Team's main list. Grouping counts independent Passengers only, so one device repeating itself cannot inflate priority. Retries after a Bounce count as attempts, since each can cost an LLM call.

## 3. Messages from people who are not at the airport

The QR code ID is the primary location signal. Device location is only a soft signal, because it can be spoofed and is unreliable indoors. Combine signals into a trust score. Never hard-block a message on location alone.

## 4. Prompt injection through free text

- Treat all Passenger text as untrusted data. It is never mixed into the instructions given to the LLM.
- Only Spring Boot calls the LLM ([ADR-0003](adr/0003-llm-only-behind-spring-boot.md)). LLM output follows a fixed structure, is validated, and cannot trigger actions.
- Knowledge base answers come only from the Knowledge base. A Passenger cannot make the LLM say arbitrary things in an official-looking answer.
- Both apps display Passenger text and LLM output as plain text, never as markup.

## 5. Malicious uploads (stretch, with photos)

Validate and size-limit photos, strip metadata, accept no executable content.

## 6. Abusive content

The quality check Bounces most abusive, joke and garbage content up front. Anything abusive that still gets through is hidden from staff by default, with a way to review it.

## 7. Staff access

- Individual accounts, each in one Team, from phase 1 ([ADR-0008](adr/0008-individual-staff-accounts-team-routing.md)).
- Least privilege: a staff member can read and act only on their Team's Issues. The duty manager sees all. The server checks this on every request.
- An audit log of who did what to which Issue.
- The sign-in mechanism (Supabase Auth or Spring Security) is a planning decision.

## 8. LLM failure

A message is never lost because the LLM is unavailable. Proposed: it is stored as Unclassified and sent to the duty manager, and an outage never turns into Bounced messages. See [AI pipeline](triage-and-dedup.md).

## 9. Privacy

See [principles](principles.md). Ticket data is under review in [product](product.md), and the proposal is that it never passes through Flare.
