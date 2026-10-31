---
status: accepted
date: 2026-10-31
---

# Tooling and the full dependency list

The tooling and the complete list of libraries we expect to need. Anything not on this list needs the team's agreement first ([code rules](../code-rules.md)).

- **Back end:** Java 21 (installed on the Windows laptop; check the MacBooks), Spring Boot 4.1.2 (pinned in the existing skeleton), and Maven with the Maven wrapper (`mvnw`, `mvnw.cmd`), so nobody installs Maven. Spring Boot 4 renamed some starters (the skeleton uses `spring-boot-starter-webmvc`), so use the names start.spring.io generates for 4.x. Prefer Java records for DTOs. The skeleton already includes Lombok; whether to keep it is decided at S05 (see the note below).
- **Passenger web app:** React and TypeScript, built with Vite's React TypeScript template. No router, no UI kit, no state library, no i18n library. A small screen state machine and JSON string files are enough.
- **Staff app:** Expo with the TypeScript default template (including Expo Router), run on phones through Expo Go during development and the demo. Expo lets the Windows laptop and the MacBooks all work on the app, without native builds.

| Where | Dependency | Why |
| --- | --- | --- |
| Back end | `spring-boot-starter-webmvc` (in the skeleton), `-validation` | REST API and input validation |
| Back end | `spring-boot-starter-data-jpa`, `postgresql` driver | Database access |
| Back end | `flyway-core`, `flyway-database-postgresql` | Versioned schema migrations |
| Back end | `spring-boot-starter-security`, `-oauth2-resource-server` | Verifying Supabase JWTs |
| Back end | `spring-boot-starter-actuator` | Health check for hosting |
| Back end | `com.anthropic:anthropic-java` | Official Claude SDK ([ADR-0003](0003-llm-only-behind-spring-boot.md)) |
| Back end (test) | `spring-boot-starter-test` | JUnit 5, AssertJ, Mockito |
| Passenger web app | `react`, `react-dom`, `vite`, `typescript`, `@vitejs/plugin-react` | Vite template defaults |
| Passenger web app (dev) | `openapi-typescript` | Types from the contract |
| Staff app | Expo default template packages | App framework and navigation |
| Staff app | `@supabase/supabase-js`, `@react-native-async-storage/async-storage` | Staff sign-in and keeping the session ([ADR-0010](0010-supabase-postgres-and-auth-only.md)); follow Supabase's current Expo guide, which may also require a URL polyfill |
| Staff app | `expo-localization` | Device language for the default UI language |
| Staff app (dev) | `openapi-typescript` | Types from the contract |
| Scripts | none | Node 22 built-ins only (`fetch`, `fs`) |

| Scripts | `qrcode` (npm, dev only, in `scripts/posters/`) | Poster QR codes (D07). Approved 2026-10-31 |

**Already in the skeleton, not in the original plan:** `lombok` and the Spring Security test starters. The test starters are fine. Lombok is left for the team to keep or remove at S05.

**Needs explicit team OK before use:**
- a sound package, for a Critical alert sound (stretch)
- GitHub Actions for CI (task S09)
