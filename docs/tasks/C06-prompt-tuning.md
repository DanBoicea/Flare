# C06: Prompt tuning from eval results, model choice

Status: TODO
Owner:
Track: C · Phase 3 · Estimate: 1 h · Agent-friendly: no
Depends on: C05

## Goal
Fix the worst failure classes, and pick the model per task from data rather than guesses ([ADR-0016](../adr/0016-llm-model-per-task-in-config.md)).

## Do
1. **Group misses by cause:** category confusion, false Bounces, missed Questions, over-merges, and so on. Fix the top two or three causes in the prompts with clearer definitions or examples. Don't overfit single items.
2. **Re-run C05** after each change and record the history row. Keep a change only if it improves the target metric without worsening Emergency recall or the false-Bounce rate.
3. **Model comparison:** run the eval once with `claude-haiku-5-5` for `CLASSIFY` (config only). Compare accuracy, latency and cost with Opus 5.5 in `eval-results.md`. **The team decides** the final model per task, and ADR-0016 is updated (accepted, with the numbers).
4. **Freeze the prompts** for the demo by noting the git hash in `eval-results.md`.

## Done when
- [ ] At least one tuning round is recorded.
- [ ] The model comparison is recorded, and the team's choice is in ADR-0016.
- [ ] Emergency recall is still 100% on the set.
