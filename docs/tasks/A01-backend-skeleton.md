# A01: Spring Boot skeleton, config, errors, health, CORS

Status: DONE
Owner: Florin Cristi
Track: A · Phase 1 · Estimate: 1 h · Agent-friendly: partly (the main session makes structural choices)
Depends on: S01
Unblocks: A02, A06, C01, S09

## Goal
A running, empty back end that every other back-end task plugs into.

## Read first
[backend](../architecture/backend.md) · [ADR-0013](../adr/0013-tooling-and-dependencies.md) · [auth and security](../architecture/auth-and-security.md) (Spring Security rules).

## Do
1. **Start from the existing skeleton** in `apps/flare-backend`: Spring Boot 4.1.2, Java 21, group `com.fsociety`, package `com.fsociety.flarebackend`, with the Maven wrapper already committed. Add the missing dependencies from ADR-0013, using their Spring Boot 4 starter names: validation, data JPA, Flyway with its PostgreSQL module, OAuth2 resource server, actuator. The LLM client is added by C01 (it needs no extra library since ADR-0022). Lombok is kept and used instead of records (decided 2026-10-31).
2. **Create the package folders** from [backend](../architecture/backend.md), each with a `package-info.java` holding a one-line purpose.
3. **Typed config:**
   - `config/FlareProperties` as `@ConfigurationProperties("flare")` Lombok `@Value` classes, covering the whole `flare:` block in backend.md
   - replace the generated `application.properties` with `application.yml`, plus `application-local.yml` and `application-demo.yml`, using the values from backend.md, secrets only as `${ENV}`
4. **`common/`:**
   - a `@RestControllerAdvice` that maps validation and our `FlareException(ErrorCode)` to RFC 9457 `ProblemDetail` with a `code` property
   - `ErrorCode` enum (from [api](../architecture/api.md))
   - `LocalizedText` (Lombok `@Value`)
   - a `Clock` bean (UTC)
5. **`config/SecurityConfig`:**
   - stateless, CSRF off
   - `/api/public/**` and `/actuator/health` `permitAll`
   - `/api/staff/**` authenticated (the JWT decoder is configured in A03, so leave a TODO)
   - everything else `denyAll`
   - body size limit 16 KB
6. **`config/CorsConfig`:** `/api/public/**` allows only the `flare.cors.allowed-origins` values, for `POST` and `OPTIONS`.
7. **Pom resources:** add `../../data/knowledge-base` as a resource folder with target path `knowledge-base` ([repo structure](../architecture/repo-structure.md)).
8. **Tests:** exclude JUnit tag `eval` by default (Surefire `excludedGroups=eval`). Add one pure unit test so the build has a test. Avoid `@SpringBootTest` tests that need a database.
9. **Docs:** fill in the back-end part of the README "Setup" section, with the commands from [operations](../architecture/operations.md).

## Don't
Add starters outside ADR-0013, or a database-dependent context test. The generated `FlareBackendApplicationTests` context test needs a database, so remove it or make it run without one.

## Files
`apps/flare-backend/**` (existing skeleton), `README.md` (setup section).

## Done when
- [ ] `./mvnw test` passes on Windows (`mvnw.cmd`) and macOS. (macOS passes; the Windows run was skipped by decision on 2026-10-31 and is still unverified.)
- [x] `./mvnw spring-boot:run` starts once the database env vars are set (A02 adds migrations) and `/actuator/health` returns UP.
- [x] A bad request to a stub public endpoint returns `application/problem+json` with `code`.
