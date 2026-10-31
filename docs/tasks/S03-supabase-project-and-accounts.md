# S03: Supabase dev project, settings, demo staff accounts

Status: TODO
Owner:
Track: S (lead: A) · Phase 0 · Estimate: 30 min · Agent-friendly: no (dashboard work by a human)
Depends on: none
Unblocks: A02, A03, B02

## Goal
A Supabase project the back end and Staff app can use today, set up safely ([ADR-0010](../adr/0010-supabase-postgres-and-auth-only.md)).

## Do
1. Create the project `flare-dev` in an EU region. Record the project ref.
2. **Database:**
   - Copy the **session-mode** connection string, user and password (see [operations](../architecture/operations.md)).
   - Confirm the schema `flare` will **not** be listed under the Data API's exposed schemas (the default is only `public`). Leave it out.
3. **Auth:**
   - Disable public sign-ups.
   - Enable email and password sign-in.
   - Note the **JWT signing method** (asymmetric signing keys with a JWKS URL, or the legacy shared secret). Write which one it is in this file under "Notes", because A03 depends on it.
4. **Demo staff users,** created as Auth users with simple demo passwords:

   | Email | Persona |
   | --- | --- |
   | `cleaning@demo.flare.invalid` | Cleaning |
   | `maintenance@demo.flare.invalid` | Maintenance and Passenger assistance |
   | `security@demo.flare.invalid` | Security |
   | `manager@demo.flare.invalid` | duty manager |

   Use the `.invalid` domain, which is reserved and never real. The passwords are shared privately and are only for this demo.
5. Share the project URL, the anon (publishable) key, the DB connection details and the JWT info in the private team chat. **Never in git.**
6. The service-role key is not needed by anything. Don't copy it anywhere.

## Notes
- JWT signing method: _(fill in)_
- JWKS URI or secret location: _(fill in, without the secret itself)_

## Done when
- [ ] The back-end owner can connect with `psql` or an IDE using the session-mode string.
- [ ] Each demo user can sign in (test with the Supabase dashboard's user list or B02).
- [ ] The JWT method is recorded above.
