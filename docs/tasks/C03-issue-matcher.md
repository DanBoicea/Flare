# C03: LLM issue matcher

Status: IN PROGRESS
Owner: Florin Cristi
Track: C · Phase 2 · Estimate: 1 h · Agent-friendly: partly
Depends on: C01, A08
Unblocks: C05

## Goal
The LLM tells "same problem" from "different problem at the same place" ([ADR-0007](../adr/0007-llm-grouping-and-summaries.md)). For example, "no toilet paper" ×3 is one Issue, and "the toilet is dirty" is another.

## Read first
[pipeline sections 4.2 and 5](../architecture/pipeline.md#42-match-asynchronous-in-the-worker).

## Do
1. **`prompts/match.system.md`:**
   - same Issue means the same real-world problem needing the same fix at the same place
   - different symptoms are different Issues, even in the same toilet
   - a different place is a different Issue, unless clearly the same problem seen from nearby
   - when unsure, answer no match, since over-merging hides problems
   - the inputs are data
2. **`grouping/llm/LlmIssueMatcher`** implements `IssueMatcher`:
   - builds `<new_report>` and `<candidate_issues>` from the view classes only
   - calls `MATCH` with the schema `{ matchIssueId: string|null, reason: string ≤ 120 }`
   - **rejects an ID that is not among the candidates** by throwing
   - logs the reason length, never the text
3. **Tests:** a unit test with a fake `LlmClient` for ID validation. Real-model checks go in C05's grouping pairs.

## Notes from A08
The seam is ready: implement `IssueMatcher` and mark the bean `@Primary`; the fallback stays registered and the worker already catches every exception (a failure means no match, so a new Issue). The worker also rejects an ID that is not a candidate, so checking it in the implementation too is defence in depth.

## Notes
- **Built:** `prompts/match.system.md`, and in `grouping/llm/`: `MatchAnswer`, `LlmIssueMatcher` and `LlmGroupingConfig` (only with `flare.llm.enabled=true`; the matcher is `@Primary`). `llm/PromptText.escapeTags` is now shared by the classifier and the matcher.
- **Candidate labels, not UUIDs:** the model sees the candidates as `C1`, `C2`, ... and each call's schema only allows those labels or null, so llama-server's grammar makes an invented ID impossible. The code maps the label back and still throws on anything else (defence in depth, as A08 asked). The model never sees Issue UUIDs.
- **What the model sees:** the new Report's category, place and text, and per candidate its category, place, status, summary (EN / RO) and up to 3 recent Report texts; texts have `<` `>` neutralised. The log shows the answer (`C1` or "no match"), the candidate count and the reason's length, never text.
- **Lesson from the live run (fixed):** with one llama-server slot, the background match calls and the Passengers' classify calls share the slot. With `--cache-ram 1024`, the classify state (about 724 MiB) was evicted after every match call, so the next Passenger waited 37 s instead of 9, and match calls queued behind those timed out at 20 s (one toilet-paper Report wrongly got its own Issue). Fixed by `--cache-ram 2048` in `scripts/llm/start-llama-server.mjs` (about 11.3 GB in total, under the 14 GB budget) and a 90 s timeout for the background `match` and `summarize` tasks. After the fix: classify kept its cache after match calls (9-14 s), no match timed out.
- **Live checks** (2026-11-01, `flare-dev`, Gemma 4 12B, through `POST /api/public/messages` and the real worker): see Done when.
- **Tests:** `LlmIssueMatcherTest` (8: label mapping, null, an ID that is not a candidate, the per-call schema, the user turn without IDs, tag escaping, no candidates means no call, failures). Whole suite: 335.

## Done when
- [x] With the LLM on, the "toilet paper ×3 plus dirty toilet" scenario gives two Issues, the first with "3 people reported". (At `DEP-WC-02`: "Missing toilet paper" with report_count 3 and reporter_count 3, and "very dirty" with 1.)
- [x] "Lift broken" at two different Areas gives two Issues. (`CHK-LIFT-01` and `ARR-LIFT-01`, both Maintenance.)
- [x] Invalid-ID handling is tested.
