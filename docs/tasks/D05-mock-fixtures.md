# D05: Mock fixtures for both apps

Status: TODO
Owner:
Reviewer: Track B owner
Track: D (OpenCode) · Phase 1 · Estimate: 45 min · Agent-friendly: yes
Depends on: S02 (contract shapes)

## Goal
Realistic, contract-exact, **simulated** data so both apps can be built and demoed without a back end ([frontends](../architecture/frontends.md), [operations: demo fallback](../architecture/operations.md#demo-fallback)).

## Do
1. **`apps/flare-mobile/src/fixtures/`:**
   - `me.*.json` for the four personas
   - `teams.json`
   - `issues-active.json` with about 12 `IssueCard`s covering: every Severity, an Emergency, unclassified, reopened, taken, a "Mentioned" place, multi-reporter ("4 people reported"), and each Team including unrouted
   - `issues-resolved.json`
   - `issue-detail.<id>.json` for 3 Issues, with Reports and history
   - `review.json` with items for each Quarantine reason and one Strict mode alert
2. **`apps/flare-webapp/src/fixtures/`:** one `MessageResponse` per outcome (`REPORTED`, `ANSWERED` with found true and false, `BOUNCED` with each reason and with `sendAnywayAvailable`, `EMERGENCY`).
3. Every object must type-check against the generated `schema.d.ts`. Write the fixtures as `.ts` files typed with the schema types, so `tsc` checks them.
4. All names, texts and places are simulated and match D01's labels.

## Done when
- [ ] `tsc --noEmit` passes in both apps with the fixtures imported by `mock.ts`.
- [ ] Every UI state in both apps is reachable in mock mode.
