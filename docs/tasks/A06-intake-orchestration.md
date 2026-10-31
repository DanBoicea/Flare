# A06: Public endpoints, intake pipeline, fallbacks, Questions

Status: TODO
Owner:
Track: A · Phase 1 · Estimate: 1.5 h · Agent-friendly: no (core orchestration; main session)
Depends on: A02, S02
Unblocks: A07, A08, C02, W02 (real back end), D06

## Goal
`POST /api/public/messages` and send-to-staff work end to end on fallbacks, so the thin slice (G1) runs without any LLM code.

## Read first
[pipeline sections 1-2](../architecture/pipeline.md) (the step table is the spec) · [backend: seams](../architecture/backend.md#seams-between-track-a-and-track-c) · [api: public endpoints](../architecture/api.md#public-endpoints-passenger-web-app).

## Do
1. **Seams and fallbacks:**
   - `classification/MessageClassifier`, `Classification`, `ClassificationInput`, `FallbackClassifier`, exactly as in backend.md
   - `CategoryTeamRouting` (pure, **test first**): category to Team from `flare.routing`; `OTHER`, fallback or low confidence give no Team
2. **`location/QrCodeService`:** validates the format (at most 32 characters of `[a-z0-9]`; anything else is unknown) and returns `ACTIVE` with the Location, `MUTED`, or `UNKNOWN`.
3. **`intake/MessageIntakeService`:** the orchestrator, following pipeline.md section 1 step by step.
   - Steps 4-7 (Emergency, bot check, limits, send anyway) call interfaces whose **no-op implementations** you add now. A07 replaces them, so the order is already wired.
   - Commit the Report first, then enqueue it (to `GroupingWorker`; until A08 lands, a no-op queue).
   - Catch **every** exception from the classifier and use `FallbackClassifier`.
   - Questions: create a `question` row. Bounced: store nothing.
4. **Controllers:** `PublicMessageController` (`POST /api/public/messages`) and `PublicQuestionController` (send-to-staff, with idempotence, a 30-minute limit, and a device hash match).
5. **Hashing placeholder:** `ratelimit/ClientFingerprint` (HMAC-SHA256 with `flare.security.hash-secret`) is needed here for `device_hash` and `network_hash`. Implement it now, since it's small; A07 reuses it.
6. **Logging:** outcome, ID, latency. Never the text.

## Done when
- [ ] With the LLM disabled, a message to a seeded QR code returns `REPORTED`, and a `report` row is stored with `classification_source=FALLBACK`.
- [ ] An unknown `qrCodeId` gives a stored Report with status `QUARANTINED` / `UNKNOWN_QR_CODE`.
- [ ] Validation errors give a Problem Details response with `code`.
- [ ] Routing tests pass.
- [ ] A fake classifier in a unit test returning `QUESTION` creates a `question` row and returns `ANSWERED`. One returning `GARBAGE` stores nothing and returns `BOUNCED`.
