# A09: Needs review API: list, release, dismiss, alerts

Status: IN PROGRESS
Owner: Luciano
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

## Notes
- **Built:** `review/ReviewService`, `review/ReviewController` and the DTOs `ReviewList`, `ReviewItem`, `ReviewAlert`, `ReleaseRequest`. Alerts come from `QrCodeFloodGuard.activeAlerts()` (A07), with the QR code's Location (null for an unknown ID).
- **Release** sets `ACCEPTED` and `team_override`, writes `REPORT_RELEASED` (details: reason and Team), and queues the Report only after the transaction commits. The quarantine reason is kept on the Report, so the history shows why it was held. Releasing or dismissing a Report that is not Quarantined (or doesn't exist) is 404.
- **Grouping worker (A08 territory, needed here):** a Report with `team_override` skips the Emergency and matching path and gets its own Issue for that Team; `IssueService.createFromReport` uses `team_override` before category routing. A released "Send anyway" Report is classified with `passengerInsisted=true`, like a Question sent to staff.
- **Tests:** `ReviewServiceTest` (8), `ReviewEndpointsTest` (8), `ReportGrouperTest` (5), `IssueServiceTest` (3). The whole suite passes (271 tests).

## Done when
- [ ] A Report Quarantined for an unknown QR code can be released to Security and appears in `security@`'s list. (Each step is unit-tested: release stores the Team, the worker creates the Issue with it, and the access policy shows it to Security; still to check once against `flare-dev`.)
- [x] A dismissed Report disappears from review.
- [x] A non-manager gets 403.
