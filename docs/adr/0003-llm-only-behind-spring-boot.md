---
status: accepted
date: 2026-10-30
---

# Only the Spring Boot back end talks to the LLM, and the LLM is Claude

Every LLM call goes through the Spring Boot back end: classification, Knowledge base answers, grouping and summaries. The passenger web app and the Staff app never call the LLM and never hold its API key. We use Claude through Anthropic's official Java SDK because the team already works with Claude. A single LLM entry point gives us one place to enforce structured output, timeouts, cost limits and the fallback behaviour, and lets us change the model or provider later. The API key comes from the Claude Console and lives in a server environment variable. A Claude Code subscription does not pay for these calls. Which Claude model to use is still open.
