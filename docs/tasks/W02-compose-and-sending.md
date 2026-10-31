# W02: Compose and Sending screens, emergency strip, language button

Status: IN PROGRESS
Owner: BoiceaDan
Track: C · Phase 1 · Estimate: 1.5 h · Agent-friendly: no (the main UX surface; main session)
Depends on: W01 (the real back end needs A06)
Unblocks: W03, G1

## Goal
A stressed, elderly or one-handed Passenger can send a message in about 30 seconds ([ADR-0004](../adr/0004-single-free-text-field.md), [product: passenger web app](../product.md#passenger-web-app-requirements)).

## Read first
[frontends: passenger web app](../architecture/frontends.md#passenger-web-app-appsflare-webapp) (state machine and copy deck) · [ADR-0019](../adr/0019-bot-check-without-third-party.md).

**Design:** [Design V1](../design/flare-design-v1/README.md) screens 01, 02 and 10 ([ADR-0021](../adr/0021-design-v1-navy-cobalt-instrument-sans.md)); layout and accessibility rules from [docs/design](../design/README.md): [foundations](../design/foundations.md), [components](../design/components.md), [screens](../design/screens.md), [mobile](../design/mobile.md), [reference screens](../design/reference/README.md).

## Do
1. **`EmergencyStrip`:** always at the top, high contrast, with a `tel:112` link, `role="note"`, and "This is not an emergency line" as its accessible name (visible under Send on Compose).
2. **`LanguageButton`:** top right, shows the *other* language's name ("Română" / "English"), switches instantly, and has an `aria-label`.
3. **Compose:**
   - the title as the visible label of the big `MessageField` (a `textarea` with the placeholder from the copy deck, the dictation hint, a counter near 1000, and `enterKeyHint="send"`)
   - the "I would like a call back (optional)" checkbox, which reveals one phone input and the retention note
   - the privacy line
   - the honeypot `website` field, hidden as in frontends.md
   - a big Send button, disabled while the text is empty
   
   The text is kept in state across every return to Compose.
4. **Sending:** "Checking your message…", "Don't close this page.", the Passenger's text under "You wrote", plus an `aria-live="polite"` region, and "This can take a few seconds." after 6 s. On a network error or timeout, go back to Compose with an inline error and the text kept.
5. **The request:** build `MessageRequest` with the text, `uiLanguage`, `qrCodeId`, `deviceId`, contact, `website`, `elapsedMs` and `sendAnyway`, and send it through `client.ts`.
6. **Keyboard and screen reader:** the order is strip, language, field, contact, Send. Focus moves to the result heading on every screen change.

## Done when
- [x] Sending works in mock mode (Compose and Sending checked in screenshots at 390 px and 320 px, RO and EN).
- [ ] Sending works against the real back end. **Moved to [S06](S06-integration-g1-g2.md) (G1)**, because it needs A06. It does not block DONE for W02.
- [ ] Fully usable with the keyboard alone and with VoiceOver on iOS Safari. *To do by hand in mock mode (`VITE_MOCK=true npm run dev`).*
- [x] At 200% browser zoom nothing is cut off (checked at a 320 px width, which equals 200% zoom on a 640 px window).
- [ ] A slow-3G throttle shows the slow message. *To do by hand in Chrome DevTools, Network tab.*
- [x] A dropped connection shows the error with the text kept (checked with no back end running).

**Before setting DONE:** tick the three open boxes above, except the real back-end one, which S06 covers.
