# S02: API contract v1 (`contract/openapi.yaml`)

Status: DONE
Owner: Dan
Track: S (lead: A; B and C review) · Phase 0 · Estimate: 45 min · Agent-friendly: yes for the first draft, then human review (`Review: required` by one person from each of B and C)
Depends on: none
Unblocks: A06, B01, W01, D05, D06

## Goal
One OpenAPI 3.1 file that all three apps build against from the first hour ([ADR-0012](../adr/0012-contract-first-openapi.md)).

## Read first
[api](../architecture/api.md) (the source) · [data model](../architecture/data-model.md) (enums).

## Do
1. Write `contract/openapi.yaml` covering **every** endpoint, request, response and error in [api](../architecture/api.md):
   - public messages and send-to-staff
   - `me`, `teams`
   - issues list, detail and all actions
   - review list, release and dismiss
   - health
2. Define reusable schemas:
   - the enums: `Category`, `Severity`, `TeamId`, `StaffRole`, `IssueStatus`, `QuarantineReason`, `BounceReason`, `Outcome`, `Language`, `ClassificationSource`, `PriorityReasonCode`
   - the objects: `LocalizedText`, `LocationRef`, `StaffRef`, `PriorityReason`, `Priority`, `IssueCard`, `IssueDetail`, `ReportView`, `HistoryEntry`, `MessageRequest`, `MessageResponse`, `ReviewItem`, `ReviewAlert`, `Problem`
3. Mark required and nullable fields exactly. Add `maxLength` and patterns from the data model and API docs.
4. Add a `bearerAuth` security scheme on `/api/staff/**`, and none on `/api/public/**`.
5. Add one example per response. Use only simulated names, for example "Ana (simulat)".
6. Validate the file with any OpenAPI linter you already have, or `npx openapi-typescript contract/openapi.yaml -o /tmp/x.d.ts` as a smoke test, since it fails on invalid specs.

## Don't
Invent endpoints that aren't in [api](../architecture/api.md). If you need a change, update `api.md` in the same PR and say so.

## Files
`contract/openapi.yaml`, `docs/architecture/api.md` (only if something had to change).

## Done when
- [ ] Every endpoint in `api.md` is present, with request, response and error shapes.
- [ ] The types generate without errors.
- [ ] One reviewer from Track B and one from Track C approve the PR.
- [ ] Announced in the team chat. This is gate G0.
