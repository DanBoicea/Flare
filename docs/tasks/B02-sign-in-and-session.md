# B02: Sign-in, session, `/me`, role-aware tabs

Status: TODO
Owner:
Track: B · Phase 1 · Estimate: 1 h · Agent-friendly: no
Depends on: B01, S03 (and A03 for the real `/me`; use mock mode until then)
Unblocks: B03

## Goal
Staff sign in with their own account and see tabs that match their role ([ADR-0008](../adr/0008-individual-staff-accounts-team-routing.md), [ADR-0010](../adr/0010-supabase-postgres-and-auth-only.md)).

## Read first
[auth and security: staff sign-in](../architecture/auth-and-security.md#staff-sign-in-adr-0010) · [frontends: Staff app](../architecture/frontends.md#staff-app-appsflare-mobile-expo-router).

## Do
1. **`src/auth/supabase.ts`:** a Supabase client from `EXPO_PUBLIC_SUPABASE_URL` and `EXPO_PUBLIC_SUPABASE_ANON_KEY`, with AsyncStorage session storage and auto-refresh. Follow Supabase's current Expo guide, including any polyfill it requires. **Use it for auth only:** no `from(...)` queries, no Realtime.
2. **`src/auth/session.tsx`:** a provider exposing `session`, `signIn(email, password)`, `signOut()`, and the access token for `client.ts`.
3. **`src/app/_layout.tsx`:** the gate. No session goes to `sign-in`. With a session, load `/me` (`useMe`). A `403 NOT_A_STAFF_MEMBER` signs out and shows an explanation.
4. **`src/app/sign-in.tsx`:**
   - big fields and a big button
   - error strings for wrong credentials, network failure and not-staff
   - the note "Demo accounts with simulated data"
   - in mock mode, a role picker instead
5. **`src/app/(tabs)/_layout.tsx`:**
   - tabs Active and Resolved, plus Needs review only for `DUTY_MANAGER`
   - a header with the display name, Team chips, a language toggle and sign-out

## Done when
- [ ] Each of the four demo accounts signs in, and only `manager@` gets Needs review.
- [ ] Sign-out returns to sign-in.
- [ ] Killing and reopening the app keeps you signed in.
- [ ] Wrong password and no-network states are readable in RO and EN.
