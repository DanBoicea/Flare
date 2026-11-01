---
status: accepted
date: 2026-10-31
amended: 2026-11-01 (accepted after the evaluation, C05 and C06; first rewritten for the self-hosted llama.cpp server, ADR-0022)
---

# The model and its sampling settings are set per LLM task in config; the demo runs Gemma 4 12B

Flare makes four kinds of LLM call: classify, match, summarise, and the Knowledge base answer, which is part of classify. Each reads its settings from `flare.llm.tasks.*`: the model name, `temperature`, maximum output tokens and timeout. Changing them never needs a code change.

- **One server, one model.** `llama-server` serves one model under the name `flare-llm` (`--alias`), so every task uses it. Switching models means restarting the server with another file (`LLAMA_MODEL=... node scripts/llm/start-llama-server.mjs`); a task could point at a second server later if needed.
- **The model is Gemma 4 12B Instruct, Q5_K_M** (chosen by the team on 2026-11-01 from the evaluation in [eval-results](../eval-results.md)). On the simulated test set with the C06 prompts: Emergency recall 100%, no false Bounces, Questions 100%, kind 99%, category 96%, severity 81% exact and 100% within one level, grouping 94% with no false merges, 8.7 s median per classification on the M4 Pro.
- **Qwen 3.5 9B Q6_K was the alternative:** about a third faster (5.8 s) and lighter, but it claimed Knowledge base answers it did not have (Questions 85%), merged two different problems twice and flagged two false Emergencies. It stays the fallback if speed ever matters more than accuracy.
- **Temperature 0** for every task: repeatable answers matter more than variety.
- **Passengers wait rather than get the fallback** (decided 2026-10-31). Classify is synchronous and a busy server queues requests, so its timeout is a generous **40 s**. The fallback answers only when the LLM really can't: the server is unreachable or loading (fail fast), the daily cap is reached, the output is invalid, or 40 s pass. The passenger web app waits 50 s, so the back end always answers first. Match and summarise run in the background with a **90 s** timeout, because they may queue behind classifications on the one slot (C03).
- **C02's 5 s target is not met** (8.7 s median): accepted, since every call is well inside the timeout and accuracy won.

There is no per-call cost with a self-hosted model. `flare.llm.daily-call-limit` stays as a load guard, so a spam wave cannot keep the server busy for everyone.
