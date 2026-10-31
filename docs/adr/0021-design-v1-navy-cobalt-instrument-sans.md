---
status: accepted
date: 2026-10-31
supersedes: 0020
---

# Design V1 replaces airfield signage: navy and cobalt, self-hosted Instrument Sans

Every surface (passenger web app, Staff app, QR poster, pitch) follows the mock-ups in [docs/design/flare-design-v1](../design/flare-design-v1/README.md), made by the team with Claude Design after ADR-0020. Their tokens are in [foundations](../design/foundations.md):
- navy #0B2545 for the header and text, on a pale blue page #EAF0FB
- cobalt #2457E6 fill for the one main action on a screen
- red #C8102E only for 112, the Emergency screen and Critical
- generous radii: 16 px controls, 20-24 px cards, a navy header with 28 px rounded bottom corners
- Instrument Sans (SIL OFL), weights 400-700, nothing below 16 px

The team preferred V1's look and calmer layout to the signage direction, which was still only proposed.

**The font is self-hosted** in `apps/flare-webapp/src/assets/fonts/` (woff2, latin and latin-ext subsets, about 41 KB, with `OFL.txt`). The mock-ups load it from Google Fonts; the app must not, because that sends every Passenger's IP address to a third party (GDPR), and the page should not depend on another service. latin-ext carries ș ț ă â î.

**V1 also changes the Compose and Sending screens** (accepted with this ADR, reflected in [frontends](../architecture/frontends.md#passenger-web-app-appsflare-webapp)):
- Contact is a "Vreau să fiu sunat înapoi (opțional)" checkbox. Ticking it reveals the phone field and the retention note; unticked means no contact is sent.
- The screen title is the message field's label, with a dictation hint under the field.
- "This is not an emergency line" sits under the Send button. The strip carries it as its accessible name.
- Sending echoes the Passenger's text ("Ai scris") and says "Nu închide pagina".

## Consequences

- W01's signage styles were replaced in W02. `docs/design/reference/` and the parts of components.md and screens.md that describe signage colours are out of date until they are updated; the V1 mock-ups win where they disagree.
- **Lost from ADR-0020:** Atkinson Hyperlegible's I/l/1 distinction. Instrument Sans has a plain capital I, so "Ia cu tine" may read as "la". Check it in W04 and reword the copy if needed.
- **Open for the Staff app (B01):** V1 only gives Critical its own colour (red). High, Medium and Low still need distinct colour, icon and word so that severity is never colour alone. Until B01 decides, keep the severity tokens and icon shapes from ADR-0020.
- The Staff app needs Instrument Sans as static `.ttf` files loaded with `expo-font` (already installed).

## Considered options

- **Keep airfield signage (ADR-0020):** more distinctive and tested by five persona reviewers, but the team chose V1.
- **V1 colours with Atkinson Hyperlegible Next:** keeps the legibility gain, but does not match the mock-ups the team approved.
- **Load Instrument Sans from Google Fonts as the mock-ups do:** zero setup, but a third-party request from every Passenger's phone.
