# S04: Anthropic Console API key with spend limit

Status: TODO
Owner:
Track: S (lead: C) · Phase 0 · Estimate: 15 min · Agent-friendly: no
Depends on: none
Unblocks: C01

## Goal
An API key the back end can use, with a hard cost ceiling ([ADR-0003](../adr/0003-llm-only-behind-spring-boot.md), [ADR-0016](../adr/0016-llm-model-per-task-in-config.md)).

## Do
1. In the Claude Console (the API product, separate from any Claude Code or claude.ai subscription), make sure the account has API credits.
2. Create a key named `flare-hackathon`. If the Console offers a workspace or key-level spend limit, set it low (for example $20). Otherwise set the organisation limit.
3. Share the key privately with whoever runs the back end. It goes only in `ANTHROPIC_API_KEY` env vars. Never in git, never in an app, never in a screenshot.
4. After the hackathon, revoke the key.

## Done when
- [ ] The key exists, a spend limit is set, and it has been shared privately.
- [ ] A one-off call from C01's smoke test succeeds.
