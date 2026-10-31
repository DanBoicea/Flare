# D06: Demo scenario files and runner script

Status: TODO
Owner:
Reviewer: Track A owner
Track: D (OpenCode) · Phase 2 · Estimate: 1 h · Agent-friendly: yes
Depends on: S02, D01 (QR code IDs)
Unblocks: S06 (G2), A10 (log check), S07

## Goal
One command replays each demo scenario against any back end, so rehearsals and the live demo are repeatable ([product: demo plan](../product.md#demo-plan)).

## Do
1. **`data/demo-scenarios/*.json`**, one file per scenario:
   ```json
   { "name": "toilet-paper-burst", "description": "…", "steps": [
       { "qrCodeId": "k7f3m9q2xd", "text": "Nu mai e hârtie", "uiLanguage": "ro", "device": "d1", "waitMs": 1500 },
       { "qrCodeId": "k7f3m9q2xd", "text": "No toilet paper in cubicle 2", "uiLanguage": "en", "device": "d2", "waitMs": 1500 } ],
     "expect": "One Issue at ARR-WC-02, 3 people reported; a separate 'dirty' Issue" }
   ```
   `device` is a label that the runner maps to a stable random UUID, so you can simulate several Passengers or one.
2. **Scenarios:**
   - single Report
   - toilet-paper burst plus a dirty-toilet Report
   - Question, then send to staff
   - joke Bounced, then a rewritten Report
   - three Bounces, then send anyway
   - spam wave (25 messages, one device, then many devices, one QR code), giving Strict mode
   - unknown QR code
   - mentioned other place
   - Emergency in RO and in EN
   - Cleaning vs Security routing
   - mixed RO/EN
3. **`scripts/demo/run-scenario.mjs <scenario|all> [--base-url URL]`** (Node 22 built-ins only):
   - posts each step to `/api/public/messages` with `elapsedMs` 5000 and an empty honeypot, and the step's `sendAnyway` flag when set
   - for "send to staff" steps, uses the returned `questionId`
   - prints each outcome, then the `expect` line for a human to check
   - never prints secrets
   - default base URL `http://localhost:8080`
4. **`data/demo-scenarios/README.md`:** how to run it, and a warning that it creates data, so reset dev with `scripts/sql/reset-dev.sql` (D01) first.

## Done when
- [ ] `node scripts/demo/run-scenario.mjs all` runs against the local back end, and every outcome matches the expectations at G2.
