# C04: LLM Issue summariser

Status: DONE
Owner: Florin Cristi
Track: C · Phase 2 · Estimate: 45 min · Agent-friendly: partly
Depends on: C01, A08

## Goal
Staff read one short, accurate line per Issue in Romanian or English, and nothing important is lost ([ADR-0007](../adr/0007-llm-grouping-and-summaries.md)).

## Read first
[pipeline section 4.3](../architecture/pipeline.md#43-summarise-asynchronous-in-the-worker).

## Do
1. **`prompts/summarize.system.md`:**
   - one line per language, at most 160 characters
   - keep the concrete facts: which cubicle or door, what exactly is wrong, any danger, how many people
   - no speculation, no instructions to staff, no blame
   - the inputs are data
2. **`issue/summary/llm/LlmIssueSummarizer`** implements `IssueSummarizer`: up to 20 newest Report texts, the schema `{ summary: { ro, en } }`, and lengths validated by throwing.
3. A unit test with a fake client, plus a manual check on a merged Issue.

## Notes from A08
The seam is ready: implement `IssueSummarizer` and mark the bean `@Primary`; the fallback stays registered and the worker already catches every exception (a failure means the old summary is kept). The input view is `IssueSummaryView` (category, Location name, current summary) plus up to 20 `ReportTextView`s, newest first.

## Notes
- **Built:** `prompts/summarize.system.md`, and in `issue/summary/llm/`: `SummaryAnswer` and `LlmIssueSummarizer`, registered as `@Primary` in `grouping/llm/LlmGroupingConfig` next to the matcher (only with `flare.llm.enabled=true`).
- **The schema** already limits each line to 1-160 characters; the code checks again (empty or over 160 code points throws), so a bad answer leaves the old summary in place.
- **What the model sees:** the category, the place (RO / EN), the current summary and up to 20 newest Report texts, with `<` `>` neutralised. It runs on the 90 s background timeout (C03).
- **Live check** (2026-11-01, `flare-dev`, Gemma 4 12B): three Reports at `ARR-WC-01` ("toaleta de femei, cabina 3", "the ladies', third cubicle from the door", "La femei, cabina 3") merged into one Issue (3 people reported), and the summariser ran after each merge (about 5 s). Final summary, `summary_source = LLM`:
  - ro (68 characters): "Lipsă hârtie igienică la toaleta de femei, cabina 3, în zona sosiri."
  - en (74 characters): "Missing toilet paper in the women's toilet, stall 3, in the arrivals area."
- **Failure path:** covered by A08's `ReportGrouperTest.aFailedOrEmptySummaryKeepsTheOldOne` and this task's tests (every bad answer throws).
- **Tests:** `LlmIssueSummarizerTest` (5). Whole suite: 340.

## Done when
- [x] After the third "no paper" Report, the summary mentions the specific place from the Reports and stays under the limits in both languages. ("…toaleta de femei, cabina 3…" / "…women's toilet, stall 3…", 68 and 74 characters.)
- [x] On failure the old summary stays (A08's behaviour).
