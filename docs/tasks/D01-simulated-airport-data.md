# D01: Simulated Areas, Locations, QR codes, staff seed, reset script

Status: IN PROGRESS
Owner: Luciano
Reviewer: Track A owner
Track: D (OpenCode) · Phase 1 · Estimate: 45 min · Agent-friendly: yes
Depends on: A02 (table shapes)
Unblocks: A03 (seeded staff), D06, D07, G1

## Progress note
Split in two so D02 and D03 can use the real Location labels now: **part 1** (steps 1-4, `data/airport/airport.json`) is done first, without waiting for A02. **Part 2** (steps 5-6, the SQL seed and reset script) waits for A02's table shapes.

Part 1 is done. Two fields were added to the shape below:
- `qrCodeStatus` on each Location (`ACTIVE` or `MUTED`, matching `QrCodeStatus`), so the seed knows which QR code is muted
- a top-level `_comment` carrying the "SIMULATED DEMO DATA" header, since JSON has no comments

The muted QR code is at `LND-ENT-03`.

## Goal
A believable but clearly **simulated** airport layout to demo with. Nothing claims to be the real Craiova Airport layout ([CLAUDE.md](../../CLAUDE.md): never invent airport facts).

## Do
1. **`data/airport/airport.json`** (the source of truth), with this shape:
   ```json
   {
     "simulated": true,
     "areas": [ { "id": "ARR", "name": { "ro": "Sosiri (simulat)", "en": "Arrivals (simulated)" } } ],
     "locations": [ { "label": "ARR-WC-02", "area": "ARR", "criticality": 0,
                      "name": { "ro": "Toalete sosiri, lângă banda 2", "en": "Arrivals toilets near belt 2" },
                      "qrCodeId": "k7f3m9q2xd" } ],
     "staff": [ { "email": "cleaning@demo.flare.invalid", "displayName": "Ana (simulat)", "roles": ["STAFF"], "teams": ["CLEANING"] } ]
   }
   ```
2. **Content:**
   - 5 Areas: check-in hall, security checkpoint, departures gates, arrivals and baggage claim, landside entrance and parking
   - about 24 Locations, each with a **unique** label `AREA-TYPE-NN`; types are toilets (several, to show grouping), lifts, escalators, baggage belts, seating, information desk, family room, entrances
   - `criticality` 2 for the security checkpoint and lifts, 1 for baggage belts, 0 otherwise
   - Location names stay short; the "(simulated)" / "(simulat)" marker goes on the Area names, the staff display names and the seed file header
3. **QR code IDs:** one per Location, 10 characters from `abcdefghjkmnpqrstuvwxyz23456789`, unique, generated randomly (not hand-picked). Add **one** `MUTED` QR code at an extra Location, for the demo.
4. **Staff:** the four accounts from [S03](S03-supabase-project-and-accounts.md), plus Teams: `maintenance@` is in `MAINTENANCE` and `PASSENGER_ASSISTANCE`; `manager@` has roles `["DUTY_MANAGER"]` and no Teams; the other three have `["STAFF"]`. The seed fills `staff_member_role` as well as `staff_member_team`. Display names are fictional first names plus "(simulat)".
5. **`apps/flare-backend/src/main/resources/db/seed/R__demo_seed.sql`**, generated from the JSON (by hand or with a Node script in `scripts/seed/`, Node built-ins only):
   - upserts (`on conflict do update`) Areas, Locations, QR codes, staff members and staff Teams
   - links staff by email through `auth.users`; skips silently if the user doesn't exist yet
   - header comment: "SIMULATED DEMO DATA, not real airport data"
6. **`scripts/sql/reset-dev.sql`:** truncates `flare.report`, `flare.issue`, `flare.question` and `flare.audit_event` (cascade), with a big warning header. It does not touch Areas, Locations, QR codes, Teams or staff.

## Don't
Use real shop names, real gate numbers or real airport staff names. Add dependencies.

## Done when
- [ ] Flyway applies the seed on `flare-dev`, and re-running is a no-op.
- [ ] Every QR code ID resolves through A06.
- [ ] All demo accounts have their `staff_member` rows.
- [ ] The reviewer approves.
