# W03: Answer, Bounced, Confirmation, Emergency screens

Status: IN PROGRESS
Owner: BoiceaDan
Track: C · Phases 1-2 · Estimate: 1.5 h · Agent-friendly: partly (once W02 sets the patterns)
Depends on: W02
Unblocks: W04

## Goal
Every outcome tells the Passenger honestly and plainly what happened and what to do next ([ADR-0001](../adr/0001-bounce-garbage-messages.md), [ADR-0005](../adr/0005-knowledge-base-answers-questions.md), [principles](../principles.md)).

## Read first
[frontends: state machine and copy deck](../architecture/frontends.md#passenger-web-app-appsflare-webapp) · [api: response](../architecture/api.md#post-apipublicmessages).

**Design:** [Design V1](../design/flare-design-v1/README.md) screens 03-06 and 11 ([ADR-0021](../adr/0021-design-v1-navy-cobalt-instrument-sans.md), changes in [decisions round 8](../decisions.md#round-8-design-v1-2026-10-31)); rules from [docs/design](../design/README.md): [foundations](../design/foundations.md), [components](../design/components.md), [screens](../design/screens.md), [mobile](../design/mobile.md), [reference screens](../design/reference/README.md).

## Do
1. **Answer:**
   - the label "Automatic answer", which honestly marks AI involvement
   - the answer text as plain text, keeping line breaks
   - two buttons, "That helped" and "Send to staff"; when `answer.found` is false, show `answer.notFound` and make "Send to staff" the main button
   - "Send to staff" calls `sendQuestionToStaff(questionId)`, then shows Confirmation
2. **Bounced:**
   - the reason text from `bounce.<REASON>`
   - "Edit message", which returns to Compose with the text kept and focus in the field
   - when `sendAnywayAvailable`, also "Send anyway for staff review", which resends with `sendAnyway: true`
   - never accusing, never revealing thresholds
3. **Confirmation:** the title, the "what happens next" line, and "Send another message", which clears the text.
4. **Emergency:**
   - the title and body from the copy deck
   - a huge "Call 112" `tel:` button
   - "Back", which returns to Compose
   - no medical or security advice beyond the fixed text
5. **Demo label:** the "Demo with simulated data" label in the footer, controlled by a build flag that defaults to on.

## Done when
- [x] Each outcome is reachable in mock mode (checked in screenshots, RO at 390 px and EN at 320 px).
- [ ] Each outcome is reachable against the real back end. **Moved to [S06](S06-integration-g1-g2.md) (G1)**, because it needs A06. It does not block DONE for W03.
- [x] Texts come from the string files in both languages.
- [ ] Focus lands on each screen's heading (and in the field after "Edit message"). *Implemented; check by hand with a keyboard and VoiceOver.*
- [ ] The Emergency screen appears within one round trip for an Emergency phrase (A07). *Mock mode: yes. Real back end: S06.*

**Before setting DONE:** tick the focus check above; the real back-end items are covered by S06.
