# C04: LLM Issue summariser

Status: TODO
Owner:
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

## Done when
- [ ] After the third "no paper" Report, the summary mentions the specific place from the Reports and stays under the limits in both languages.
- [ ] On failure the old summary stays (A08's behaviour).
