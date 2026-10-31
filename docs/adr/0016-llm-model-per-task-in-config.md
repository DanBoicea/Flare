---
status: proposed (decide after the eval run, C06)
date: 2026-10-31
---

# The Claude model and effort are set per LLM task in config, defaulting to Claude Opus 5.5

Flare makes four kinds of LLM call: classify, match, summarise, and the Knowledge base answer, which is part of classify. Each reads its model ID, effort level, maximum output tokens and timeout from configuration, so changing the model never needs a code change. The default is `claude-opus-5-5`, the current default Claude model, with effort `low`, because these are short classification and summarising tasks. The team may switch some or all tasks to `claude-haiku-5-5`, which is much cheaper and faster. The rough cost per classification call is about $0.01 on Opus 5.5 and about $0.0003 on Haiku 5.5; see [technical](../technical.md). That choice should be made by running the evaluation script on both models, not by guessing. A spend limit is set on the Console API key.
