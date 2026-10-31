# A03: Supabase JWT verification, `CurrentStaff`, `/me`, `/teams`, access policy

Status: DONE
Owner: Florin Cristi
Track: A · Phase 1 · Estimate: 1 h · Agent-friendly: no (security-critical; main session)
Depends on: A02 (and S03 notes on the JWT method)
Unblocks: A04, B02

## Goal
Only real, active staff members reach `/api/staff/**`, and the server knows their Teams and roles.

## Read first
[auth and security](../architecture/auth-and-security.md) · [ADR-0008](../adr/0008-individual-staff-accounts-team-routing.md) · [ADR-0010](../adr/0010-supabase-postgres-and-auth-only.md).

## Do
1. **JWT decoder:**
   - with asymmetric signing, `jwk-set-uri` from `SUPABASE_JWKS_URI`
   - with legacy HS256, `NimbusJwtDecoder.withSecretKey` from a new env var (add it to `.env.example`)
   
   Validate the issuer and the `authenticated` audience.
2. **`staff/CurrentStaff`:** turns the JWT `sub` into a `StaffMember` with Teams and roles. A missing or inactive member gets `403 NOT_A_STAFF_MEMBER`. Never read roles or Teams from JWT claims.
3. **`staff/StaffAccessPolicy`** (pure, **test first**): `canSee`, `canAct`, `canReview`, exactly as in auth-and-security.md. Tests cover:
   - a staff member in several Teams
   - an unrouted Issue
   - Critical plus Security
   - the duty manager
   - a member with both `STAFF` and `DUTY_MANAGER`, and a member with no roles (ordinary staff access)
   - an Issue from another Team
4. **Endpoints:**
   - `GET /api/staff/me` (`MeController`)
   - `GET /api/staff/teams` (`TeamController`)
   
   DTOs match the contract.
5. **Manual test:** get a token by signing in through B02, or with the Supabase Auth REST password grant from your terminal (never commit the command with credentials). Call `/me` for each demo account.

## Notes
- `config/SupabaseJwtConfig` builds the `JwtDecoder` explicitly: JWKS from `SUPABASE_JWKS_URI`, ES256 (what flare-dev uses) or RS256, and the expiry, issuer and `authenticated` audience checks in one tested `validator()`. No HS256 secret is needed (S03).
- `staff/StaffOnlyConfig` runs `CurrentStaff.require()` before every `/api/staff/**` handler, so a new staff endpoint can't forget the staff-member check. `CurrentStaff` caches the member for the request.
- 401 responses are Problem Details with `WWW-Authenticate: Bearer` and never say why a token failed.
- The `/teams` item DTO is `TeamResponse` (contract schema `Team`), because `Team` is the entity.

## Done when
- [x] `/me` works for all four demo accounts once D01's seed is loaded. Before that, insert one `staff_member` by hand. (Checked in Bruno with real Supabase tokens, 2026-10-31.)
- [x] No token, an expired or invalid token, or a valid token for a user with no `staff_member` row each give 401 or 403.
- [x] The policy unit tests pass.
