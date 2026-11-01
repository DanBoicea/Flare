# scripts

Helper scripts for the team. Node 22 built-ins only: no dependencies.

- `check-i18n.mjs`: checks that the Romanian and English string files match in both apps (task D04).
- `demo/run-scenario.mjs`: plays a demo scenario against an API base URL (task D06).
- `posters/`: printable QR code poster generator (task D07).
- `llm/start-llama-server.mjs`: starts the self-hosted LLM (`llama-server`) for the back end. All settings (model file, port, slots, context, cache type, thinking) are at the top of the file, and each can be overridden with an environment variable for one run (task S04).
- `llm/measure-classify.mjs`: sends simulated test-set messages to a running `llama-server` with a realistic classification prompt and reports speed, JSON validity and a first quality check; `--burst 3` also tests queueing (S04, C06).
- `seed/generate-seed.mjs`: writes the demo seed SQL (`R__demo_seed.sql`) from `data/airport/airport.json` (task D01).
- `sql/reset-dev.sql`: clears Reports, Issues, Questions and audit events in the dev database (task D01).
