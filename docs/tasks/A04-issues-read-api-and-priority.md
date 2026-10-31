# A04: Issue list and detail, priority calculator

Status: IN PROGRESS
Owner: Dan (PriorityCalculator, DTOs), Luciano (list and detail endpoints)
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

## Notes
- **Built:** `StaffIssueController` (`GET /api/staff/issues?view=ACTIVE|RESOLVED` and `GET /api/staff/issues/{id}`), `IssueQueryService` (read-only transaction), `IssueCards` (entity to DTO mapping) and `IssueConfig` (the `PriorityCalculator` bean). `PriorityCalculator` and the DTOs came with A06's branch.
- **Visibility** is only `StaffAccessPolicy.canSee`, applied in Java after loading. The RESOLVED list is capped at 50 **after** filtering, so other Teams' Issues never shorten a member's list.
- **No N+1 queries:** the list queries fetch Location, Area and `takenBy` with the Issue (`findActive`, entity graph on the RESOLVED query).
- **History details** are read from the stored JSON; an unreadable value shows the line with empty details instead of failing the request.
- **For B03:** the endpoints are ready. Without the LLM (C02), every Issue is Unclassified and has no Team, so only the duty manager (`manager@`) sees new Issues until Track C lands.
- **Tests:** `IssueQueryServiceTest` (12: the four demo accounts, sorting, the RESOLVED cap, detail, 404) and `StaffIssueEndpointsTest` (7: JSON shape against the contract, nulls, sign-in, errors). The whole suite passes (164 tests).

## Done when
- [x] The tests pass.
- [ ] With seeded or hand-inserted Issues, each demo account sees exactly the Issues the policy allows. (Covered by unit tests with the four demo accounts; still to check once against `flare-dev`.)
- [x] The JSON matches the contract (spot-check against `contract/openapi.yaml`).
