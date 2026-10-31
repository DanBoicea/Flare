# W02: Compose and Sending screens, emergency strip, language button

Status: TODO
Owner:
Track: C · Phase 1 · Estimate: 1.5 h · Agent-friendly: no (the main UX surface; main session)
Depends on: W01 (the real back end needs A06)
Unblocks: W03, G1

## Goal
A stressed, elderly or one-handed Passenger can send a message in about 30 seconds ([ADR-0004](../adr/0004-single-free-text-field.md), [product: passenger web app](../product.md#passenger-web-app-requirements)).

## Read first
[frontends: passenger web app](../architecture/frontends.md#passenger-web-app-appsflare-webapp) (state machine and copy deck) · [ADR-0019](../adr/0019-bot-check-without-third-party.md).

## Do
1. **`EmergencyStrip`:** always at the top, high contrast, with a `tel:112` link, "This is not an emergency line", and `role="note"`.
2. **`LanguageButton`:** top right, shows the *other* language's name ("Română" / "English"), switches instantly, and has an `aria-label`.
3. **Compose:**
   - the title, then the big `MessageField` (a `textarea` with a visible label, the placeholder from the copy deck, a counter near 1000, and `enterKeyHint="send"`)
   - the collapsed "Want us to contact you? (optional)" with one input and the retention note
   - the privacy line
   - the honeypot `website` field, hidden as in frontends.md
   - a big Send button, disabled while the text is empty
   
   The text is kept in state across every return to Compose.
4. **Sending:** "Checking your message…", plus an `aria-live="polite"` region, and "This can take a few seconds." after 6 s. On a network error or timeout, go back to Compose with an inline error and the text kept.
5. **The request:** build `MessageRequest` with the text, `uiLanguage`, `qrCodeId`, `deviceId`, contact, `website`, `elapsedMs` and `sendAnyway`, and send it through `client.ts`.
6. **Keyboard and screen reader:** the order is strip, language, field, contact, Send. Focus moves to the result heading on every screen change.

## Done when
- [ ] Sending works in mock mode and against the real back end (G1).
- [ ] Fully usable with the keyboard alone and with VoiceOver on iOS Safari.
- [ ] At 200% browser zoom nothing is cut off.
- [ ] A slow-3G throttle shows the slow message, and a dropped connection shows the error with the text kept.
