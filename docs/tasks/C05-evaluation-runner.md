# C05: Evaluation runner and results doc

Status: TODO
Owner:
Track: C · Phase 3 · Estimate: 1 h · Agent-friendly: partly (the runner code can go to OpenCode; interpreting the results is human work)
Depends on: C02, C03, D02
Unblocks: C06, S08

## Goal
Honest numbers for the pitch and a safety net for prompt changes ([AI pipeline: measuring](../triage-and-dedup.md#measuring-quality)).

## Do
1. **The runner:** a JUnit test class tagged `eval` (run with `./mvnw test -Dgroups=eval`) that reads `../../data/test-set/messages.jsonl` and `grouping-pairs.jsonl` (D02's format) and calls the **real** classifier and matcher. Print a table, and write the same as Markdown to `target/eval-results.md`:
   - kind accuracy
   - category accuracy (Reports only)
   - Severity accuracy, exact and within one level
   - Team routing accuracy (through `CategoryTeamRouting`)
   - Emergency recall (word list plus LLM), which must be 100%, with every miss listed
   - garbage catch rate
   - **false-Bounce rate** of genuine Reports and Questions
   - grouping accuracy on pairs, with false merges and false splits shown separately
   - p50 and p90 latency on the machine that ran it (name it), and total tokens
2. **Misses:** list every misclassified item (ID, expected, got). These are simulated texts, so printing them is fine.
3. **Run time:** before running, print the item count and check `llama-server` answers `GET /health`. A full run on a laptop takes minutes; run it once per prompt change, not in a loop.
4. **Results doc:** copy the result, with date, model file (name and quantisation), machine, `temperature` and prompt version (git hash), into `docs/eval-results.md`, keeping a history table.

## Done when
- [ ] One full run is recorded in `docs/eval-results.md`, with every metric above.
- [ ] Emergency misses, if any, are fixed (word list or prompt) or explicitly listed as limits.
