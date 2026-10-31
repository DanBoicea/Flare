# S10: `llama-server` on the demo machine

Status: TODO
Owner:
Track: S (lead: A) · Phase 3 · Estimate: 45 min · Agent-friendly: no (hardware and network by a human)
Depends on: S04, C06 (the final model), A11 (how the back end is hosted)
Unblocks: S07

## Goal
During the pitch, the demo back end reaches a running `llama-server` with the chosen model, fast enough, and the demo still works if it doesn't ([ADR-0022](../adr/0022-self-hosted-llm-llama-cpp.md)).

## Read first
S04 (run instructions and measurements) · [operations](../architecture/operations.md) · [auth and security: secrets and the hardening checklist](../architecture/auth-and-security.md#secrets-and-keys).

## Do
1. **Decide where it runs**, with the team, and write it under **Notes**:
   - **Option A, a team laptop at the venue,** next to the back end (simplest if the back end also runs on a laptop; `llama-server` stays on `127.0.0.1`).
   - **Option B, the same host as the deployed back end,** if that host has the memory (and ideally a GPU) for the model. Check the host's limits before choosing.
   - **Option C, a team laptop reached over the network** by a hosted back end: needs `--host 0.0.0.0`, `--api-key`, and a reachable address. Least robust on venue Wi-Fi; avoid unless A and B are impossible.
2. **Set it up** with the model and settings from C06: the start command, `--alias flare-llm`, and a context size that fits the Knowledge base. Download the model **before** the event; venue networks are slow.
3. **Point the demo back end at it:** `FLARE_LLM_ENABLED=true`, `FLARE_LLM_BASE_URL`, and `FLARE_LLM_API_KEY` when the server is reachable beyond `127.0.0.1` (shared privately, never in git).
4. **Warm it up** before going on stage: send a few messages so the model is loaded and the prompt prefix is cached. The first call after a start is slow.
5. **Test the failure path** once: stop `llama-server` during a rehearsal and check that messages still become Unclassified Reports for the duty manager, and that Emergency words still show 112 and reach Security.
6. **Note the numbers** for the pitch: warm classify latency on the demo machine, under **Notes**.

## Don't
Expose `llama-server` without `--api-key`, rely on downloading the model at the venue, or let the demo depend on the LLM for Emergency handling.

## Notes
- Where it runs and why: _(fill in)_
- Start command: _(fill in)_
- Warm classify latency on the demo machine: _(fill in)_

## Done when
- [ ] The demo back end classifies a message through `llama-server` on the demo machine.
- [ ] With `llama-server` stopped, the demo still works on the fallbacks, and Emergency handling is unaffected.
- [ ] `llama-server` is on `127.0.0.1` or protected by `--api-key`; no key is in git.
