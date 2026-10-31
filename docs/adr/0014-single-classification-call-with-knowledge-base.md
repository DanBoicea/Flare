---
status: accepted
date: 2026-10-31
---

# One LLM call classifies a message and answers it if it is a Question; the whole Knowledge base goes into the prompt

Each passenger message gets exactly one synchronous LLM call. It returns the kind (Report, Question or garbage) and the classification. For a Question it also returns the answer, written only from the Knowledge base included in the system prompt. One call instead of "classify, then answer" halves the time a Passenger waits for an answer. The Knowledge base is small (tens of entries), stored as Markdown files in `data/knowledge-base/`, and loaded whole into the system prompt, where the LLM server's prompt caching makes repeated use fast (amended 2026-10-31: `llama-server` reuses the identical prompt prefix, ADR-0022). There is no vector database and no search step. Grouping and Issue summaries are separate calls that run in the background after the Passenger already has a response. If the Knowledge base ever outgrows the prompt, add retrieval then.
