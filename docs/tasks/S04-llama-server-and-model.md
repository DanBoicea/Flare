# S04: `llama-server` on a dev machine, first model, latency measured

Status: DONE
Owner: Florin Cristi
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
6. **Choose the first model** for development with the team, and write the choice and the numbers under **Notes**. Check that the classify timeout in `application.yml` (40 s: Passengers wait rather than get the fallback, ADR-0016) covers a message queued behind two others on this machine; adjust it if the numbers say so. The final choice still comes from the eval (C05, C06).
7. **Share** how to run it in the team chat: the model name and download link, and the start command. Anyone who runs the back end with `FLARE_LLM_ENABLED=true` needs it running, or needs `FLARE_LLM_BASE_URL` pointing at the machine that runs it (then start it with `--host 0.0.0.0 --api-key <key>`, and set `FLARE_LLM_API_KEY` to the same key; share the key privately).

## Don't
Commit model files, put the API key in git or in an app, or expose `llama-server` beyond `127.0.0.1` without `--api-key`.

## Notes
Measured on 2026-10-31 on a MacBook Pro M4 Pro, 24 GB, with llama.cpp build 10470 (Homebrew). Budget: the model may use at most 14 GB.

- **Install:** `brew install llama.cpp` (it was already installed here; build 10470 loads Gemma 4 and Qwen 3.5).
- **Start:** `node scripts/llm/start-llama-server.mjs`. Every setting is at the top of the script, and each can be overridden for one run with an environment variable. The defaults are the measured setup:
  `llama-server -m models/gemma-4-12b-it-Q5_K_M.gguf --alias flare-llm --host 127.0.0.1 --port 8081 -np 1 -c 8192 -fa on -ctk q8_0 -ctv q8_0 --reasoning off --cors-origins localhost --ctx-checkpoints 4 --cache-ram 1024`
- **Model files** (in the git-ignored `models/` folder, both Apache 2.0):
  - Gemma 4 12B Instruct, `gemma-4-12b-it-Q5_K_M.gguf`, 8.4 GB: https://huggingface.co/unsloth/gemma-4-12b-it-GGUF/resolve/main/gemma-4-12b-it-Q5_K_M.gguf
  - Qwen 3.5 9B Instruct, `Qwen3.5-9B-Q6_K.gguf`, 7.5 GB: https://huggingface.co/unsloth/Qwen3.5-9B-GGUF/resolve/main/Qwen3.5-9B-Q6_K.gguf
- **Measure:** `node scripts/llm/measure-classify.mjs --count 20 --burst 3` (simulated test-set messages, the pipeline.md 4.1 schema, a draft system prompt of real size: 14,333 characters, about 3,700 tokens with the whole Knowledge base).
- **Working `json_schema` request** (no secrets):
  ```sh
  curl -s http://127.0.0.1:8081/v1/chat/completions -H 'Content-Type: application/json' -d '{
    "model": "flare-llm", "temperature": 0, "max_tokens": 64,
    "messages": [{"role": "system", "content": "..."}, {"role": "user", "content": "..."}],
    "response_format": {"type": "json_schema", "json_schema": {"name": "kind", "strict": true,
      "schema": {"type": "object", "additionalProperties": false, "required": ["kind"],
                 "properties": {"kind": {"enum": ["REPORT", "QUESTION", "GARBAGE"]}}}}}}'
  ```
  The answer is in `choices[0].message.content` (a JSON string), with `choices[0].finish_reason`, `usage` and llama-server's own `timings` (`prompt_n`, `cache_n`, `predicted_per_second`).
- **`finish_reason` values seen:** `stop` for a complete answer; `length` when `max_tokens` cuts it off (C01 treats anything but `stop` as a failure).
- **Measurements** (20 identical messages, one slot, temperature 0):

  | | Gemma 4 12B Q5_K_M | Qwen 3.5 9B Q6_K |
  | --- | --- | --- |
  | Valid JSON | 20/20 | 20/20 |
  | Kind (and category for Reports) as labelled | 19/20 | 18/20 |
  | First call (cold, whole prompt read) | 23.7 s | 15.0 s |
  | Warm median / max | 8.7 s / 11.3 s | 5.6 s / 7.4 s |
  | Burst of 3 at once: slowest | 26.0 s | 16.4 s |
  | Generation speed | about 20 tokens/s | about 29 tokens/s |
  | Memory: model + KV + compute, plus process (capped caches) | 8.7 + 0.3 + 0.2 GB, plus about 1.2 GB: **about 10.3 GB** | 7.9 + 0.2 + 0.1 GB, plus about 0.6 GB: **about 8.8 GB** |
  | Romanian (human look) | Good, occasional grammar slip ("spatele este detașată"); once called a stuck lift an Emergency | More wording slips ("Sacă lăsată singură"); fills summary and answer for garbage (the validator will drop them) |

- **What we learned about settings:**
  - **One slot beats two on one laptop.** With `-np 2`, two requests run at half speed each (about 10 tokens/s) and a slot can lose the cached prompt; a burst of 3 took 40 s. With `-np 1`, requests queue and each runs at full speed with the cached prompt: 8, 17 and 26 s.
  - **Prompt caching works without `--swa-full`:** after the first call, only about 70 new tokens are processed, about 3,700 come from the cache, thanks to context checkpoints.
  - **Cap the caches**, or memory grows past the budget: each checkpoint is about 170 MiB (default 32 per slot) and the RAM prompt cache defaults to 8 GiB. With `--ctx-checkpoints 4 --cache-ram 1024` the process stayed bounded.
  - **CORS:** llama-server allows every origin by default, so any website open in a browser could call it. The script sets `--cors-origins localhost`.
  - **Thinking off** server-wide with `--reasoning off`.
- **Chosen first model:** Gemma 4 12B Q5_K_M (team, 2026-10-31): more accurate and better Romanian; Qwen 3.5 9B stays the faster fallback. The final choice comes from C05/C06.
- **Timeouts:** the classify timeout of 40 s (ADR-0016) covers a message queued behind two others (26 s worst case measured) with margin. The very first call after a start takes about 24 s, so S10 warms the server up before the demo.

## Done when
- [x] `llama-server` answers `GET /health` with `{"status":"ok"}` on port 8081 with the chosen model.
- [x] A schema-constrained chat completion returns valid JSON, and the working request is recorded above.
- [x] Latency and JSON validity are recorded for at least two candidates, and the team picked the first model.
- [ ] The team chat has the run instructions; no model file or key is in git. (No model file or key is in git: `models/` and `*.gguf` are ignored. Sharing in the team chat is still to do.)
