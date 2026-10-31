# A09: Needs review API: list, release, dismiss, alerts

Status: TODO
Owner:
Track: A · Phase 2 · Estimate: 45 min · Agent-friendly: yes (OpenCode, with main-session review)
Depends on: A07, A08
Unblocks: B05

## Goal
The duty manager sees everything held back (Quarantined Reports and Strict mode alerts) and decides what happens to it.

## Read first
[api: Needs review](../architecture/api.md#needs-review-duty-manager-only-403-forbidden-for-other-roles) · [auth and security](../architecture/auth-and-security.md) (`canReview`).

## Do
1. `review/ReviewService` and `ReviewController`:
   - **`GET /api/staff/review`:** Quarantined Reports, newest first, at most 100, with reason, text, Location or scanned QR code ID; plus `QrCodeFloodGuard` alerts.
   - **`POST …/release`:** status becomes `ACCEPTED`, and the optional Team is stored in `report.team_override`. Enqueue it to the worker, which skips the match step when `team_override` is set and creates a new Issue with that Team; add this branch to `GroupingWorker`. Write a `REPORT_RELEASED` audit event.
   - **`POST …/dismiss`:** status becomes `DISMISSED`, with a `REPORT_DISMISSED` audit event.
2. Everything here requires `canReview`. Everyone else gets 403 `FORBIDDEN`.

## Done when
- [ ] A Report Quarantined for an unknown QR code can be released to Security and appears in `security@`'s list.
- [ ] A dismissed Report disappears from review.
- [ ] A non-manager gets 403.
