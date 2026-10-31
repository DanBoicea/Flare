# C03: LLM issue matcher

Status: TODO
Owner:
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
2. **`grouping/claude/ClaudeIssueMatcher`** implements `IssueMatcher`:
   - builds `<new_report>` and `<candidate_issues>` from the view records only
   - calls `MATCH` with the schema `{ matchIssueId: string|null, reason: string ≤ 120 }`
   - **rejects an ID that is not among the candidates** by throwing
   - logs the reason length, never the text
3. **Tests:** a unit test with a fake `LlmClient` for ID validation. Real-model checks go in C05's grouping pairs.

## Done when
- [ ] With the LLM on, the "toilet paper ×3 plus dirty toilet" scenario gives two Issues, the first with "3 people reported".
- [ ] "Lift broken" at two different Areas gives two Issues.
- [ ] Invalid-ID handling is tested.
