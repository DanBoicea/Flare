# C06: Prompt tuning from eval results, model choice

Status: DONE
Owner: Florin Cristi
Track: C · Phase 3 · Estimate: 1 h · Agent-friendly: no
Depends on: C05

## Goal
Fix the worst failure classes, and pick the model per task from data rather than guesses ([ADR-0016](../adr/0016-llm-model-per-task-in-config.md)).

## Do
1. **Group misses by cause:** category confusion, false Bounces, missed Questions, over-merges, and so on. Fix the top two or three causes in the prompts with clearer definitions or examples. Don't overfit single items.
2. **Re-run C05** after each change and record the history row. Keep a change only if it improves the target metric without worsening Emergency recall or the false-Bounce rate.
3. **Model comparison:** run the eval with at least two of S04's candidate models (restart `llama-server` with the other model file; no code change). Compare accuracy, Emergency recall and latency in `eval-results.md`. **The team decides** the final model per task, and ADR-0016 is updated (accepted, with the numbers).
4. **Freeze the prompts** for the demo by noting the git hash in `eval-results.md`.

## Notes
- **Misses grouped by cause** (from C05): severity one level too high (15 of 17 severity misses, mostly assistance requests, a stopped escalator with stairs, and comfort problems rated MEDIUM); borderline categories (a sign's light, trolleys, an empty desk, a false alarm, a described lost item); grouping (two different people needing help merged; shared trolleys at nearby spots split).
- **Round 1** (`classify.system.md`, `match.system.md`): severity anchors following triage-and-dedup.md's definitions plus "when two levels seem possible, choose the lower"; a missing adult, even elderly or confused, is a HIGH Report and only a missing child is an Emergency; clearer category boundaries; two grouping rules (different people are different Issues; a shared-equipment shortage at nearby spots in one Area is one Issue). Result: category, routing, severity and grouping up, but 2 false Bounces, so not kept as is.
- **Round 2:** airport-related questions are never garbage (answerFound false when not covered); slang or a joke around a real complaint is still a Report. False Bounces back to 0%, Emergency recall 100%, every round 1 gain kept. One abusive message is now a LOW "Other" Report instead of a Bounce (accepted, listed in eval-results).
- **Model comparison:** Qwen 3.5 9B on round 1 prompts vs Gemma; the team chose **Gemma 4 12B** (ADR-0016 accepted with the numbers).
- **Runner:** classifies each distinct pair sentence once per run.
- **Frozen prompts:** `classify.system.md` and `match.system.md` as committed with this task (the round 2 run used them uncommitted on top of `b2991e09`); `summarize.system.md` is unchanged from C04. Record the commit hash in [eval-results](../eval-results.md) when this is merged, and re-run C05 before any later prompt change.

## Done when
- [x] At least one tuning round is recorded. (Two rounds, in eval-results.md.)
- [x] The model comparison is recorded, and the team's choice is in ADR-0016. (Gemma 4 12B; ADR-0016 accepted.)
- [x] Emergency recall is still 100% on the set. (11/11 in every run.)
