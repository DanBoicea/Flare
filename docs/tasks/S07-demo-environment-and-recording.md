# S07: Demo environment, rehearsal, fallback recording

Status: TODO
Owner:
Track: S (all) · Phase 4 (Sunday morning) · Estimate: 2-3 h · Agent-friendly: no
Depends on: G3 (A11, B06, W04, C06 done), S10

## Do
1. **Fresh database:** create Supabase project `flare-demo` (or reset `flare-dev` if the plan limits projects), with the same settings and accounts as S03. Start the back end with the `demo` profile. Flyway creates the schema and seed from scratch.
2. **Deploy:** the team's chosen host for the back end and the web app ([operations](../architecture/operations.md) "Deploying"). Check HTTPS, `/actuator/health`, the security headers, and that CORS allows only the web origin.
3. **Phones:** Expo Go on 2 or 3 phones, signed in as `cleaning@`, `security@` and `manager@`, pointing at the demo back end.
4. **Posters:** print 2 or 3 posters (D07) for the stage. Scan each with a phone that is not on the laptop's Wi-Fi, so it goes through the real public route.
5. **Rehearse twice:** run the full scenario list from S06 G2 in the pitch order, timed.
6. **Record the fallback:** a screen recording of the whole run (web, then staff phones), saved to `recordings/`, which is ignored by git. Also check that mock mode on both apps runs offline on a laptop.
7. **After the pitch:** stop `llama-server` on the demo machine (S10), change its API key if it was exposed on a network, and delete or pause the demo Supabase project.

## Done when
- [ ] Two clean rehearsals.
- [ ] The recording is saved.
- [ ] Mock-mode fallback has been checked offline.
- [ ] Every demo screen shows the "simulated data" label.
