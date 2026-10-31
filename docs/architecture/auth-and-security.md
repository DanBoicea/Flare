# Auth and security

The technical side of [security and abuse](../security-and-abuse.md).

## Staff sign-in ([ADR-0010](../adr/0010-supabase-postgres-and-auth-only.md))

1. A staff member signs in on the Staff app with email and password through `supabase-js` (`signInWithPassword`). The session is kept in AsyncStorage and refreshed automatically by `supabase-js`.
2. Every call to Spring Boot carries `Authorization: Bearer <access_token>`.
3. Spring Security (OAuth2 resource server) checks the JWT's signature, expiry, issuer `https://<project-ref>.supabase.co/auth/v1`, and audience `authenticated`.
   - **Check the project's JWT signing method** in the Supabase dashboard. With asymmetric signing keys, configure the JWKS URI `https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json`. With the legacy shared secret (HS256), configure a `NimbusJwtDecoder.withSecretKey(...)` from an env var instead. S03 records which one applies.
4. `CurrentStaff` maps the JWT `sub` to `staff_member.id`, and an interceptor (`staff.StaffOnlyConfig`) runs it before every `/api/staff/**` handler. If the row is missing or inactive, the call gets `403 NOT_A_STAFF_MEMBER`. Teams and roles are loaded from our tables (`staff_member_team`, `staff_member_role`), **never from JWT claims**.
5. Sign-out goes through `supabase-js`. A disabled account stops working at its next token refresh, within the token lifetime.

Accounts are created by hand in the Supabase dashboard for the demo (S03) and linked by the seed. There is no sign-up screen and no password reset flow, because both are out of scope.

## Visibility and permissions (`staff.StaffAccessPolicy`, pure, tested first)

```
canSee(member, issue) =
     DUTY_MANAGER ∈ member.roles
  || (issue.team != null && issue.team ∈ member.teams)
  || (issue.severity == CRITICAL && SECURITY ∈ member.teams)

canAct(member, issue)       = canSee(member, issue)          // acknowledge, take, resolve, reopen, override, hand over
canReview(member)           = DUTY_MANAGER ∈ member.roles     // Needs review list, release, dismiss
```

- Unrouted Issues (`team == null`) are visible only to the duty manager.
- Every staff endpoint checks the policy on the server. Lists are filtered by the policy, and detail views and actions return `404` (not `403`) when `canSee` is false, so Issue IDs can't be probed.
- Every staff action writes an `audit_event` with the staff member's ID.

## Spring Security rules

| Path | Rule |
| --- | --- |
| `/api/public/**` | `permitAll`. CORS limited to `FLARE_ALLOWED_ORIGINS` |
| `/api/staff/**` | Authenticated with a valid JWT, plus the policy checks above |
| `/actuator/health` | `permitAll` |
| Everything else | `denyAll` |

Also:
- Stateless sessions, no cookies.
- CSRF is disabled, because the API uses only bearer tokens and JSON. This is safe only because no cookie-based authentication exists.
- Request body size is limited (16 KB).
- `server.forward-headers-strategy=framework` is enabled **only when deployed behind the host's proxy**. Otherwise `X-Forwarded-For` could be spoofed to dodge network limits ([ADR-0018](../adr/0018-hashed-device-and-network-keys.md)).

## Secrets and keys

| Secret | Where it lives | Who needs it |
| --- | --- | --- |
| `FLARE_LLM_API_KEY` | Back-end env, and `llama-server --api-key` on the LLM machine | Back end (C01). Required whenever `llama-server` listens beyond `127.0.0.1`. Never in the apps ([ADR-0022](../adr/0022-self-hosted-llm-llama-cpp.md)) |
| `SPRING_DATASOURCE_PASSWORD` | Back-end env only | Back end |
| `FLARE_HASH_SECRET` | Back-end env only (32+ random bytes) | Back end. Changing it resets all limits |
| Supabase service-role key | Nobody | Not used anywhere. Never put it in an app or the back end |
| Supabase anon (publishable) key | Staff app env | Public by design. Safe because our schema isn't exposed and RLS is on with no policies |

Share secrets in a private chat, never in git, an issue or a PR. Rotate any secret that leaks ([CLAUDE.md](../../CLAUDE.md) "Never commit").

## Hardening checklist (A11 checks it before the demo)

- [ ] The `flare` schema is not in Supabase's exposed schemas, and RLS is enabled on every table.
- [ ] Supabase sign-ups are disabled, so only invited demo accounts exist.
- [ ] The passenger web app is served over HTTPS, with security headers set at the host: CSP `default-src 'self'; connect-src <api origin>`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`.
- [ ] No Passenger text in back-end logs (grep the logs after running the demo scenarios).
- [ ] Neither app renders HTML from data. There is no `dangerouslySetInnerHTML` and no `WebView`.
- [ ] Prompt-injection messages from the test set are classified as data, not obeyed (C05).
- [ ] `llama-server` listens only on `127.0.0.1`, or uses `--api-key` and is reachable only from the back end; `flare.llm.daily-call-limit` is set.

## Threat-to-mitigation map

| Threat ([security and abuse](../security-and-abuse.md)) | Mitigation in this design | Task |
| --- | --- | --- |
| 1. Fake QR codes | Opaque IDs, unknown or muted IDs Quarantined, posters with the short URL and "never asks" line | A06, D07 |
| 2. Spam and flooding | Device, network and QR code limits, Strict mode, honeypot and timing, silent quarantine, Needs review | A07, A09 |
| 3. Reports from outside the airport | The QR code ID is the location signal, never device location; nothing is hard-blocked | A06 |
| 4. Prompt injection | Text kept in tagged user content, a fixed schema, validation in our code, no tools, Knowledge-base-only answers, plain-text rendering | C02-C04, W02, B03 |
| 5. Malicious uploads | No uploads in the hackathon scope | none |
| 6. Abusive content | Bounced as `ABUSIVE` before reaching staff | C02 |
| 7. Staff access | Supabase JWT, roles and Teams from our tables, a policy check on every endpoint, 404 on invisible IDs, audit log | A03-A05 |
| 8. LLM failure | Fallback implementations, timeouts, daily cap, worker recovery | A06, A08, C01 |
| 9. Privacy | Hashed identifiers, optional contact, retention job, no Tickets, no tracking, no text in logs | A07, A10 |
