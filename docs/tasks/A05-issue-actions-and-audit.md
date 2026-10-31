# A05: Issue actions, optimistic locking, audit log

Status: TODO
Owner:
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

## Done when
- [ ] Transition tests pass.
- [ ] Two parallel "take" calls with the same `version` give one 200 and one 409.
- [ ] Every action shows up in the Issue's history with the staff member's display name.
