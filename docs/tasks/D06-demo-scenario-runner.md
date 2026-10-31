# D06: Demo scenario files and runner script

Status: IN PROGRESS
Owner: Luciano
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

## Notes
- 11 scenario files, `01-…` to `11-…`, played in file-name order by `all`. The spam wave is `11-` so it runs last. QR code IDs were generated from the labels in `data/airport/airport.json`, so they cannot drift.
- Additions to the format, documented in `data/demo-scenarios/README.md`: `location` (a label for humans, never sent), `sendToStaff`, and `repeat` with `{n}` in `device` or `text`, so the spam wave is 25 different phones without 25 copied steps.
- Device labels are per scenario, so one simulated phone never hits the per-device limit across scenarios.
- Scenario 6 also covers the muted QR code (`LND-ENT-03`), which D01 created for the demo.
- The runner was tested against a local fake server (all 11 scenarios, the `501` stub, and no server). The real check is G2, once A06, A07 and A08 exist.
- **Rate limits:** see "Notes from D06" in [A07](A07-safety-and-limits.md). Running `all` from one laptop can trip the network limit, and "Send anyway" ends as `RATE_LIMIT`. The README tells the team to restart the back end before a full run.

## Done when
- [ ] `node scripts/demo/run-scenario.mjs all` runs against the local back end, and every outcome matches the expectations at G2.
