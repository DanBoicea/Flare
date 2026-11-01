# A11: Dockerfile, demo profile, hardening checklist

Status: TODO
Owner:
Track: A · Phase 3 · Estimate: 45 min · Agent-friendly: partly
Depends on: A09
Unblocks: S07

## Goal
Whatever host the team picks can run the back end safely with one image and env vars ([operations: deploying](../architecture/operations.md#deploying-requirements-for-whatever-host-the-team-picks)).

## Do
1. **`apps/flare-backend/Dockerfile`:** multi-stage (a Maven wrapper build, then a Java 21 runtime), a non-root user, `EXPOSE 8080`, no secrets baked in, `SPRING_PROFILES_ACTIVE=demo` by default.
2. **`application-demo.yml`:**
   - the seed location on
   - `server.forward-headers-strategy=framework` (only safe behind the host's proxy; note this in a comment)
   - only `health` exposed
3. **Hardening:** go through the checklist in [auth and security](../architecture/auth-and-security.md#hardening-checklist-a11-checks-it-before-the-demo) and tick each item in that file in your PR. The web hosting header items are for whoever deploys the web app, so tell them.
4. **Smoke test:** run the image locally against `flare-dev`, then run the health check plus one message from each D06 scenario.

## Done when
- [ ] `docker build` and `docker run` work on at least one laptop.
- [ ] The hardening checklist is ticked or each gap is noted.
- [ ] `operations.md` is updated with anything learned.

## Notes

- 2026-11-01: `apps/flare-backend/Dockerfile` (multi-stage, wrapper build, Java 21 JRE, non-root user `flare`, `EXPOSE 8080`, `demo` profile by default, no secrets) and the root `compose.yaml` exist. `docker build` and `docker compose up` worked on a MacBook (M4 Pro) against `flare-dev`, with `llama-server` on the host. Still to do: `server.forward-headers-strategy` and the hardening checklist.
