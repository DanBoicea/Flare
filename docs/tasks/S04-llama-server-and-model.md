# S04: `llama-server` on a dev machine, first model, latency measured

Status: TODO
Owner:
Track: S (lead: C) · Phase 0-1 · Estimate: 1 h · Agent-friendly: no (downloads and hardware checks by a human)
Depends on: none
Unblocks: C01, S10

## Goal
A self-hosted LLM the back end can call during development, with numbers that tell us whether it is fast and good enough ([ADR-0022](../adr/0022-self-hosted-llm-llama-cpp.md), [ADR-0016](../adr/0016-llm-model-per-task-in-config.md)). This replaces the old Anthropic API key task.

## Read first
[technical: llama.cpp server facts](../technical.md#llamacpp-server-facts-checked-2026-10-31-against-the-llama-server-readme) · [pipeline section 4](../architecture/pipeline.md) · the `llama-server` README in the [llama.cpp repository](https://github.com/ggml-org/llama.cpp) (`tools/server/README.md`). Follow it; don't guess flags or field names.

## Do
1. **Install llama.cpp** on the machine that will run the model during development (a MacBook with Apple Silicon is the likely choice; Metal is used automatically). Use the project's release binaries or a package manager. Write the exact command that worked under **Notes**.
2. **Pick 2-3 candidate models** and write them under **Notes** with their licence and file size. Each must be:
   - instruction-tuned, in GGUF format (a 4-bit quantisation such as `Q4_K_M` is a sensible start for a laptop)
   - under a licence that allows this use (check the model card, not memory)
   - known to handle Romanian; if unsure, it gets tested in step 5
   Store models outside the repo, or in the repo's ignored `models/` folder. Never commit a `.gguf` file.
3. **Start the server** for the back end:
   ```sh
   llama-server -m <path/to/model.gguf> --host 127.0.0.1 --port 8081 --alias flare-llm -c 8192
   ```
   Wait until `curl http://127.0.0.1:8081/health` returns `{"status":"ok"}`. Raise `-c` if the Knowledge base plus a message doesn't fit.
4. **Check schema-constrained JSON:** send one `POST /v1/chat/completions` with `response_format` set to `{"type": "json_schema", ...}` (the exact shape is in the README) and a tiny schema, for example `{ "kind": "REPORT" | "QUESTION" | "BOUNCED" }`. Confirm the answer parses and follows the schema. Save the working `curl` command under **Notes** (no secrets in it) so C01 can copy the request shape. Note what `finish_reason` says for a normal answer and when `max_tokens` is hit.
5. **Measure:** for each candidate, run about ten realistic messages from `data/test-set/` (Romanian and English) with a system prompt about the size of the real one (the Knowledge base from `data/knowledge-base/` plus a page of instructions). Record:
   - time to the first call's answer (cold) and the median of the rest (warm, with the prompt prefix cached)
   - whether the JSON was valid every time
   - a quick human judgement of the Romanian classifications (right, wrong, unclear)
6. **Choose the first model** for development with the team, and write the choice and the numbers under **Notes**. Propose the classify timeout for `application.yml` (warm median plus a margin). The final choice still comes from the eval (C05, C06).
7. **Share** how to run it in the team chat: the model name and download link, and the start command. Anyone who runs the back end with `FLARE_LLM_ENABLED=true` needs it running, or needs `FLARE_LLM_BASE_URL` pointing at the machine that runs it (then start it with `--host 0.0.0.0 --api-key <key>`, and set `FLARE_LLM_API_KEY` to the same key; share the key privately).

## Don't
Commit model files, put the API key in git or in an app, or expose `llama-server` beyond `127.0.0.1` without `--api-key`.

## Notes
- Install command: _(fill in)_
- Candidates (name, licence, size): _(fill in)_
- Working `curl` request for `json_schema`: _(fill in)_
- `finish_reason` values seen: _(fill in)_
- Measurements: _(fill in)_
- Chosen first model and proposed timeouts: _(fill in)_

## Done when
- [ ] `llama-server` answers `GET /health` with `{"status":"ok"}` on port 8081 with the chosen model.
- [ ] A schema-constrained chat completion returns valid JSON, and the working request is recorded above.
- [ ] Latency and JSON validity are recorded for at least two candidates, and the team picked the first model.
- [ ] The team chat has the run instructions; no model file or key is in git.
