# C05: Evaluation runner and results doc

Status: IN PROGRESS
Owner: Florin Cristi
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

## Notes
- **Built:** `apps/flare-backend/src/test/java/com/fsociety/flarebackend/eval/EvaluationRunnerTest.java`, tagged `eval` (`./mvnw test -Dgroups=eval -Dtest=EvaluationRunnerTest`). It builds the same classes the app uses by hand (classifier, matcher, fallback, word list, routing; no database, no Spring context), binds the real `application.yml`, and fails early with a clear message if `llama-server` is not running. `LlamaServerClient` now counts prompt and completion tokens for the report.
- **Grouping pairs** follow the worker: pairs in different Areas are never compared (counted as "different" without an LLM call); otherwise A becomes a candidate Issue (category and summary from classifying it) and the matcher decides whether B joins it.
- **The header** records the model file and llama.cpp build (from `llama-server`'s `/props`), the machine, the temperature and the git version of the prompts.
- **First run** (2026-11-01, Gemma 4 12B Q5_K_M on the M4 Pro, 28 minutes): kind 99%, category 93%, severity 75% exact and 99% within one level, routing 94%, **Emergency recall 100% (11/11)**, garbage 100%, **false Bounces 0%**, Questions 100%, grouping 91% (1 false merge, 2 false splits), no LLM failures, p50 8.6 s and p90 9.9 s. Full results and every miss: [eval-results.md](../eval-results.md).

## Done when
- [x] One full run is recorded in `docs/eval-results.md`, with every metric above.
- [x] Emergency misses, if any, are fixed (word list or prompt) or explicitly listed as limits. (None: 11/11. One false alarm, m035, is listed and accepted.)
