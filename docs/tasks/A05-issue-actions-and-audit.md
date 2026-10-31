# A05: Issue actions, optimistic locking, audit log

Status: IN PROGRESS
Owner: Luciano
Track: A · Phase 2 · Estimate: 1 h · Agent-friendly: partly
Depends on: A04
Unblocks: B04

## Goal
Staff can acknowledge, take, resolve, reopen, override and hand over Issues safely, and every action is recorded.

## Read first
[data model: Issue state machine](../architecture/data-model.md#issue-state-machine) · [api: Issue actions](../architecture/api.md#issue-actions).

## Do
1. **`issue/IssueActionService`:** one method per action, each one:
   - checks `canAct` (404 if the Issue is invisible)
   - checks `version` (409 `VERSION_CONFLICT`)
   - checks the transition (409 `INVALID_TRANSITION`)
   - applies the change and writes an `audit_event` through `audit/AuditService`
   
   Return the new `IssueDetail`.
   - **Take:** `IN_PROGRESS`, `taken_by` = caller, `acknowledged_at` set if empty.
   - **Resolve:** `resolved_at` = now.
   - **Reopen:** `NEW`, `reopened_count` + 1, `taken_by` cleared, `resolved_at` cleared.
   - **Override:** set the field and its `*_overridden` flag, and record from and to.
   - **Hand over:** set `team_id`, clear `taken_by`, record from and to Team. A target Team that doesn't exist is a validation error.
2. **Controllers** for every action endpoint in api.md.
3. **State transition rules** live in one pure class with **tests first**: every allowed and refused transition.

## Notes
- **Built:** `issue/IssueActionService` (one method per action, in the order visibility → version → transition → change → audit), `issue/IssueTransitions` (the pure rules table), `audit/AuditService` (also used by `IssueService` for system events), the six endpoints in `StaffIssueController`, and the request DTOs `VersionRequest`, `OverrideRequest`, `HandOverRequest`.
- **Rules decided while building** (also in data-model.md): take is allowed on an `IN_PROGRESS` Issue nobody holds (a hand over clears the holder but keeps the status); a hand over to the same Team, or of a resolved Issue, is `INVALID_TRANSITION`; an override needs at least one of category and Severity (`INVALID_REQUEST`), and only real changes get a history line. The Team does not follow an overridden category; moving an Issue is a hand over.
- **Parallel takes:** both requests can pass the version check; the action flushes inside its transaction, so the second write fails on the `@Version` column. `ApiExceptionHandler` now maps that to `409 VERSION_CONFLICT` (it was a 500).
- **After a hand over** the response still carries the Issue's detail, even if the caller can no longer see it (`IssueQueryService.detailOf`).
- **Tests:** `IssueTransitionsTest` (25 cases), `IssueActionServiceTest` (18), `AuditServiceTest` (2), action cases in `StaffIssueEndpointsTest`. The whole suite passes (219 tests).

## Done when
- [x] Transition tests pass.
- [ ] Two parallel "take" calls with the same `version` give one 200 and one 409. (Both paths unit-tested: a stale version, and the version-column conflict mapped to 409; still to try once against `flare-dev` with two phones.)
- [x] Every action shows up in the Issue's history with the staff member's display name.
