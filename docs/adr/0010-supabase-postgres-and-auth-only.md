---
status: accepted
date: 2026-10-31
---

# Supabase provides the database and staff sign-in; all data goes through Spring Boot

Supabase gives us two things:
- **A managed Postgres database,** which Spring Boot reaches over JDBC.
- **Supabase Auth for staff accounts:** the Staff app signs in with email and password and gets a JWT, which Spring Boot verifies.

Neither app ever reads or writes the database directly. Supabase's auto-generated Data API, Realtime and Storage are not used. That way every rule (Team visibility, rate limits, quarantine) lives in one place, Spring Boot, and is not duplicated in database policies. Our tables live in a `flare` schema that is not exposed through the Data API, with row-level security enabled and no policies, as a second lock.

## Considered options

- **Supabase Realtime and the Data API used straight from the Staff app.** This gives live updates for free, but Team visibility would then have to be written twice, in Spring Boot and as row-level-security policies. A mistake in either would leak Issues across Teams.
- **Our own password handling in Spring Security.** Rejected, because we would be storing passwords and issuing tokens ourselves, which is more code and more risk.
