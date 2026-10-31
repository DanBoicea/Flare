# A02: Flyway schema, entities, repositories

Status: DONE
Owner: Florin Cristi
Track: A · Phase 1 · Estimate: 1.5 h · Agent-friendly: partly (once `V1__schema.sql` is written, OpenCode can generate the entities and repositories from it; the main session reviews)
Depends on: A01, S03
Unblocks: A03, A06, A08, D01

## Goal
The `flare` schema exists on `flare-dev`, and JPA maps it exactly (`ddl-auto: validate`).

## Read first
[data model](../architecture/data-model.md) (every table, column, enum and index) · [ADR-0010](../adr/0010-supabase-postgres-and-auth-only.md).

## Do
1. **`db/migration/V1__schema.sql`:**
   - every table, column, `check` constraint (enums), FK and index from the data model
   - `gen_random_uuid()` defaults
   - `alter table … enable row level security` on every table, with **no policies**
2. **`db/migration/V2__teams_and_roles.sql`:** the four `team` rows and the two `role` rows (real configuration, not demo data):

   | ID | RO | EN |
   | --- | --- | --- |
   | `CLEANING` | Curățenie | Cleaning |
   | `MAINTENANCE` | Mentenanță | Maintenance |
   | `SECURITY` | Securitate | Security |
   | `PASSENGER_ASSISTANCE` | Asistență pasageri | Passenger assistance |

   | Role ID | RO | EN |
   | --- | --- | --- |
   | `STAFF` | Personal | Staff |
   | `DUTY_MANAGER` | Manager de tură | Duty manager |
3. **Flyway config:**
   - `spring.flyway.schemas=flare`, `create-schemas=true`
   - the `local` and `demo` profiles add `classpath:db/seed`
   - create an empty `db/seed/R__demo_seed.sql` placeholder; D01 fills it
4. **Entities** in their feature packages (`location`, `staff`, `question`, `report`, `issue`, `audit`):
   - explicit `@Table(schema = "flare", name = …)` and `@Column(name = …)`
   - enums as `@Enumerated(STRING)`
   - `StaffMember` maps its Teams and roles as `@ElementCollection` sets of `TeamId` and `StaffRole` over `staff_member_team` and `staff_member_role` (the access policy only needs the IDs; `Team` is its own entity for `/teams`)
   - `@Version` on `Issue.version`
   - `jsonb` for `audit_event.details` (map it as a `String` holding JSON, to avoid extra libraries)
5. **Repositories** (Spring Data) with the queries the other tasks need:
   - `IssueRepository.findActive()` (every non-resolved Issue; Team visibility is filtered in Java by `StaffAccessPolicy`), `findByStatusAndResolvedAtAfterOrderByResolvedAtDesc(status, since, limit)`, `findCandidates(areaId, resolvedSince, limit)` (every Location has an Area, so no `locationId`)
   - `ReportRepository.findUngrouped()`, `findQuarantined()`
   - `QrCodeRepository.findById` (loads the Location and Area too)
   - `AuditEventRepository.findByIssueIdOrderByCreatedAt` (Issue history for A04)
   
   Keep queries readable (derived names or short JPQL).
6. Run against `flare-dev`. Flyway migrates and Hibernate validates.

## Don't
Put demo data in `db/migration`, or store Passenger text in `audit_event.details`.

## Files
`apps/flare-backend/src/main/resources/db/**`, entity and repository classes under `apps/flare-backend/src/main/java/com/fsociety/flarebackend/**`.

## Done when
- [x] The app starts on `flare-dev`, with Flyway at V2 (Teams and roles seeded) and Hibernate validation passing.
- [x] RLS is enabled on every `flare` table, checked in the Supabase dashboard. (Checked with a `pg_class.relrowsecurity` query on 2026-10-31: true on all 12 tables.)
- [x] The schema matches the data model doc, and any intentional difference is fixed in the doc too.
