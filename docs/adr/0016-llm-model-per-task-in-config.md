---
status: proposed (decide after the eval run, C06)
date: 2026-10-31
amended: 2026-10-31 (rewritten for the self-hosted llama.cpp server, ADR-0022; nothing was built on the Claude version)
---

# The model and its sampling settings are set per LLM task in config

Flare makes four kinds of LLM call: classify, match, summarise, and the Knowledge base answer, which is part of classify. Each reads its settings from `flare.llm.tasks.*`: the model name, `temperature`, maximum output tokens and timeout. Changing them never needs a code change.

- **One server, one model at first.** `llama-server` serves one model, under the name given with `--alias` (we use `flare-llm`), so every task starts with the same model. A task can later point at a second server or model if the eval shows it helps.
- **Temperature 0** for every task: these are classification and summarising jobs, where repeatable answers matter more than variety.
- **Passengers wait rather than get the fallback** (decided 2026-10-31). Classify is synchronous, and a busy server queues requests, so its timeout is generous: **40 s**, enough for a message queued behind two others plus background work on one laptop. The fallback answers only when the LLM really can't: the server is unreachable or still loading (fail fast, no waiting), the daily cap is reached, the output is invalid, or 40 s pass. The passenger web app waits longer than the back end (50 s), so the back end always answers first. S04 checks 40 s against real numbers; match and summarise run in the background, so their timeouts matter less.
- **The model is chosen by evaluation, not by guessing** (C05, C06). S04 picks a first model by a quick check; C06 compares at least two candidates on the labelled test set, and this ADR is then accepted with the numbers.

There is no per-call cost with a self-hosted model. `flare.llm.daily-call-limit` stays as a load guard, so a spam wave cannot keep the server busy for everyone.
