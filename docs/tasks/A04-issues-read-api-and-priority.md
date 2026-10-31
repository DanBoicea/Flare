# A04: Issue list and detail, priority calculator

Status: TODO
Owner:
Track: A · Phase 1 · Estimate: 1 h · Agent-friendly: partly (`PriorityCalculator` with tests is a good OpenCode job; endpoints by the main session)
Depends on: A03
Unblocks: A05, B03

## Goal
The Staff app can list and open the Issues the signed-in staff member may see, sorted by priority, with the "why".

## Read first
[api](../architecture/api.md) (`IssueCard`, `IssueDetail`) · [pipeline section 6](../architecture/pipeline.md#6-priority-computed-when-the-list-is-read-issueprioritycalculator-pure) · [auth and security](../architecture/auth-and-security.md).

## Do
1. **`issue/PriorityCalculator`** (pure, **test first**): the score and reasons from pipeline.md section 6, with weights from `flare.priority`. Tests cover:
   - Critical always first
   - the reporter cap
   - the age cap
   - criticality
   - reopened
   - tie-breaking
   - the `REPORTERS` minutes value
2. **`GET /api/staff/issues?view=ACTIVE|RESOLVED`:**
   - load candidates (`ACTIVE`: not resolved; `RESOLVED`: last 24 h, at most 50)
   - filter with `StaffAccessPolicy.canSee`
   - map to `IssueCard`, including Location name, label and `takenBy`
   - sort `ACTIVE` by score
   - return `serverTime`
3. **`GET /api/staff/issues/{id}`:** `IssueDetail` with Reports (newest first, at most 50, text as stored) and history from `audit_event`. Return 404 if missing **or** not visible.
4. Keep queries simple (this is demo scale). Avoid N+1 queries for Location and `takenBy` with a join fetch or a small map.

## Done when
- [ ] The tests pass.
- [ ] With seeded or hand-inserted Issues, each demo account sees exactly the Issues the policy allows.
- [ ] The JSON matches the contract (spot-check against `contract/openapi.yaml`).
