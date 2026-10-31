# Operations

How to run, configure, seed and demo Flare. Hosting itself is the team's call ("we'll handle the demo"). This file lists what any host must provide.

## Environments

| Environment | Database and Auth | Back end | Passenger web app | Staff app |
| --- | --- | --- | --- | --- |
| **mock** | none | none | `VITE_MOCK=true` | `EXPO_PUBLIC_MOCK=true` |
| **local** | Supabase project `flare-dev` (shared by the team) | `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` on a laptop | `npm run dev` (Vite, port 5173) | `npx expo start`, opened in Expo Go on a phone on the same Wi-Fi, using the laptop's LAN IP as the API base URL |
| **demo** | Supabase project `flare-demo`, created fresh from migrations and seed (S07) | Hosted container (A11 Dockerfile), profile `demo`, HTTPS | Static hosting with HTTPS and SPA fallback | Expo Go on the team's phones, pointing at the demo back end |

Why two Supabase projects: the dev database collects junk during the day. On Sunday, the demo project is rebuilt from scratch, which also proves the migrations and seed work. If the free plan limits projects, reset `flare-dev` instead (operations step in S07).

## Environment variables (each app keeps a matching `.env.example`)

**Back end** (`apps/flare-backend/.env.example`):
```
SPRING_PROFILES_ACTIVE=local
SPRING_DATASOURCE_URL=jdbc:postgresql://<supabase-pooler-host>:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.<project-ref>
SPRING_DATASOURCE_PASSWORD=change-me
SUPABASE_JWT_ISSUER=https://<project-ref>.supabase.co/auth/v1
SUPABASE_JWKS_URI=https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json
FLARE_HASH_SECRET=change-me-32-random-bytes
FLARE_ALLOWED_ORIGINS=http://localhost:5173
FLARE_LLM_ENABLED=false
FLARE_LLM_BASE_URL=http://localhost:8081
FLARE_LLM_API_KEY=
```

Notes:
- Use the connection string the Supabase dashboard shows for **session mode** (or the direct connection if your network has IPv6). Transaction-mode pooling (port 6543) breaks the prepared statements Hibernate uses.
- Check the exact host and username format in the dashboard. The values above are placeholders.
- The Maven run loads env vars from your shell. On Windows PowerShell set them with `$env:NAME="value"`, or keep a local, ignored `apps/flare-backend/.env` and load it with your IDE.

**Passenger web app** (`apps/flare-webapp/.env.example`):
```
VITE_API_BASE_URL=http://localhost:8080
VITE_MOCK=false
```

**Staff app** (`apps/flare-mobile/.env.example`):
```
EXPO_PUBLIC_API_BASE_URL=http://192.168.1.10:8080
EXPO_PUBLIC_SUPABASE_URL=https://<project-ref>.supabase.co
EXPO_PUBLIC_SUPABASE_ANON_KEY=change-me
EXPO_PUBLIC_MOCK=false
```
`EXPO_PUBLIC_*` values are bundled into the app, so only public values go there.

## Running locally (for the README "Setup" section, filled in by S01 and the skeleton tasks)

1. **Prerequisites:** Java 21, Node 22, and Expo Go on your phone. Maven and Expo come with the projects (wrapper and npx).
2. **LLM (optional):** start `llama-server` as described in S04 (`--port 8081 --alias flare-llm`), then set `FLARE_LLM_ENABLED=true`. Without it, the back end runs on the fallbacks.
3. **Back end:** set the env vars, then `cd apps/flare-backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local` (on Windows `mvnw.cmd`). Flyway migrates and seeds on start. Check that `GET http://localhost:8080/actuator/health` returns UP.
4. **Web:** `cd apps/flare-webapp && npm install && npm run dev`, then open `http://localhost:5173/q/<a seeded qrCodeId>`.
5. **Staff:** `cd apps/flare-mobile && npm install && npx expo start`, scan the terminal QR with Expo Go, and sign in with a simulated demo account.
6. **Tests:** `cd apps/flare-backend && ./mvnw test`, which excludes the `eval` tag. The evaluation is `./mvnw test -Dgroups=eval` (needs a running `llama-server`; C05). Strings: `node scripts/check-i18n.mjs`.

## Seeding and resetting

- **Schema:** Flyway `db/migration`, always applied.
- **Demo data:** `db/seed/R__demo_seed.sql`, a repeatable migration loaded only in `local` and `demo`. Flyway re-runs it when its checksum changes. It upserts, so it is safe to re-run.
- **Staff accounts:** create them in Supabase Auth by hand (S03), one per demo persona:
  - `cleaning@demo.flare.invalid`: Cleaning
  - `maintenance@…`: Maintenance and Passenger assistance
  - `security@…`: Security
  - `manager@…`: duty manager
  
  The seed links them by email. Use the reserved `.invalid` domain so no real address is ever involved.
- **Resetting dev during the day:** a `scripts/sql/reset-dev.sql` (D01) that truncates `report`, `issue`, `question` and `audit_event`. Run it from the Supabase SQL editor.

## Deploying (requirements for whatever host the team picks)

- **Back end:** one instance ([ADR-0015](../adr/0015-single-instance-in-memory-limits.md)), Java 21 or the A11 Docker image, HTTPS termination, env vars set, health check on `/actuator/health`, and forwarded headers from the host's proxy (see [auth and security](auth-and-security.md)).
- **Passenger web app:** `npm run build` then upload the static `dist/`. HTTPS, SPA fallback to `index.html`, and the security headers from the hardening checklist.
- **Staff app:** no deployment. Expo Go on staff phones (iOS and Android). A standalone build is stretch.
- **Domain:** the host's subdomain for the demo. The pitch explains that a real deployment uses the airport's official domain.

## Demo fallback

1. **Live demo:** the demo environment plus scripted scenarios (`scripts/demo/run-scenario.mjs`, D06).
2. **No network or a broken back end:** both apps in mock mode with demo fixtures (D05). This runs entirely on a laptop.
3. **Everything broken:** the recording in `recordings/` (S07), which is not in git.

## Observability (minimal)

- Structured log lines with an event name, IDs, latency, LLM tokens and outcome. No Passenger text.
- A daily LLM call counter and per-call latency in the logs, plus `llama-server`'s own console output and `GET /health`.
- The Needs review list doubles as the abuse monitor.
