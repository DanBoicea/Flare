# A08: Grouping worker: candidates, Issue create, merge, reopen

Status: IN PROGRESS
Owner: Dan (worker, merge rules), Luciano (seams, candidates, matching, reopen, summary, final fallback)
Track: A · Phases 1-2 · Estimate: 1.5 h · Agent-friendly: partly (`CandidateSelector` and its tests can go to OpenCode)
Depends on: A06
Unblocks: A09, A10, C03, C04, G1

## Goal
Every accepted Report ends up in exactly one Issue, quickly, even with no LLM.

## Read first
[pipeline section 5](../architecture/pipeline.md#5-grouping-worker-adr-0007-adr-0015) (the spec) · [ADR-0007](../adr/0007-llm-grouping-and-summaries.md) · [ADR-0015](../adr/0015-single-instance-in-memory-limits.md).

## Do
1. **Seams:**
   - `grouping/IssueMatcher` with `NoMatchIssueMatcher`
   - `issue/summary/IssueSummarizer` with `FallbackIssueSummarizer`
   - view classes as in [backend](../architecture/backend.md)
2. **`grouping/GroupingWorker`:**
   - a single-thread executor and a `LinkedBlockingQueue<UUID>`
   - `enqueue(reportId)`
   - on `ApplicationReadyEvent`, enqueue every `ACCEPTED` Report with `issue_id is null`
   - each Report in its own transaction: retry once, then make it a new Unclassified Issue
3. **The steps from pipeline.md section 5, in order:**
   - classify if unclassified (call `MessageClassifier` with the same fallback rule as intake; Emergency stays `CRITICAL`)
   - Emergency shortcut
   - `grouping/CandidateSelector` (pure, **test first**)
   - matcher
   - create or merge or reopen through `issue/IssueService`
   - summary (a new Issue copies the Report summary; a merge calls the summariser, keeping the old summary on failure)
   - audit
   - `grouped_at`
4. **`IssueService` merge rules:**
   - `report_count` and `reporter_count` (distinct `device_hash`)
   - Severity is the highest of the Reports unless overridden
   - `emergency` is OR'd across Reports
   - `unclassified` is true only while every Report is a fallback
   - update `last_reported_at` and `mentioned_place`
   - a match on a resolved Issue reopens it
5. **Expose** `enqueue` to intake, send-to-staff and review release.

## Notes
- **Built (with what came from A06 and A09):** the seams `grouping/IssueMatcher` → `NoMatchIssueMatcher` and `issue/summary/IssueSummarizer` → `FallbackIssueSummarizer`, with the views `NewReportView`, `CandidateIssueView`, `IssueSummaryView` (not `IssueView`, which is the ACTIVE/RESOLVED enum) and `ReportTextView`; `grouping/CandidateSelector` (pure); `GroupingConfig`; the full flow in `ReportGrouper` (team override → own Issue; Emergency → open Emergency Issue; otherwise candidates → matcher → merge, reopen a resolved Issue, refresh the summary; else a new Issue); `IssueService.reopenByReport`; and the final failure rule in `GroupingWorker` (after the retry, `ReportGrouper.groupAsUnclassified`).
- **Matcher safety:** a matcher exception, or an answer that is not one of the candidates, means no match (a new Issue). A summariser exception, an empty summary or the same summary keeps the old one; a new summary sets `summary_source = LLM`.
- **For C03 and C04:** implement `IssueMatcher` and `IssueSummarizer` and mark the beans `@Primary`; the fallbacks stay registered for the failure path. The worker sends at most 3 recent texts per candidate and 20 newest texts for a summary.
- **Tests:** `CandidateSelectorTest` (6), `ReportGrouperTest` (17), `GroupingWorkerTest` (5), `IssueServiceTest` (reopen). The whole suite passes (295 tests).

## Done when
- [x] `CandidateSelector` tests pass: same Area, the open and recently-resolved window, the limit of 10, no Location.
- [x] With fallbacks: three Reports at one Location become three separate Issues (no matcher), each visible to the duty manager.
- [ ] With a fake matcher in a test that always returns the first candidate: three Reports become one Issue with `reporter_count` 3, or 1 if they come from the same device. (The merge into one Issue is unit-tested; `reporter_count` is the database's distinct count of device hashes, still to check once against `flare-dev`.)
- [ ] Killing and restarting the app with ungrouped Reports groups them on startup. (The startup re-queue is unit-tested; still to try once with the real app.)
