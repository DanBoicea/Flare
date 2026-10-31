# S09: CI on GitHub Actions (optional; needs team OK)

Status: TODO
Owner:
Track: S (lead: A) · Phase 2 · Estimate: 30 min · Agent-friendly: yes
Depends on: A01, W01, B01, and the team's OK ([ADR-0013](../adr/0013-tooling-and-dependencies.md) lists CI as needing approval)

## Goal
Every PR shows whether the back-end tests, the web build and the string check pass, so `main` stays green.

## Do
`.github/workflows/ci.yml` with three jobs, on pull requests and pushes to `main`:
- **back end:** Java 21, `./mvnw -B test` (the `eval` tag is excluded by default)
- **passenger web:** Node 22, `npm ci && npm run build` in `apps/flare-webapp`
- **strings:** `node scripts/check-i18n.mjs`

Also a type check for the Staff app, `npx tsc --noEmit` in `apps/flare-mobile`. No secrets are needed: tests use no database and no LLM.

## Don't
Add deploy steps, or any step that needs a secret.

## Done when
- [ ] A PR shows all jobs green, and a broken test turns the PR red.
