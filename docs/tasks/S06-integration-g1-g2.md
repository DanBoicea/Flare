# S06: Integration runs at gates G1 and G2

Status: TODO
Owner:
Track: S (all; lead: whoever is free) · Phases 1-2 · Estimate: 30 min per gate · Agent-friendly: no
Depends on: G1: A04, A06, A08, B03, W02 · G2: everything in phase 2
Unblocks: phase 2 (after G1), phase 3 (after G2)

## Goal
Prove the apps work together against the real back end on `flare-dev`, and fix mismatches before they pile up.

## G1: thin slice
1. Start the back end (`local` profile, `FLARE_LLM_ENABLED=false`, so fallbacks run). Start the web app and the Staff app, pointing at it.
2. Open `/q/<seeded qrCodeId>` for a toilet Location and send "Nu mai e hârtie".
3. The `manager@` account sees an **Unclassified** Issue at that Location within about 10 s. `cleaning@` does not, because fallbacks have no Team.
4. Repeat with `FLARE_LLM_ENABLED=true` once C02 is merged. The `cleaning@` account now sees it as Toilet / Cleaning, with an AI summary.
5. Note each contract mismatch, and fix it in the contract and code the same hour.

## G2: every demo scenario
Run each scenario in [product demo plan](../product.md#demo-plan) with D06's runner and by hand on a phone. Tick each one:
- [ ] A single Report routed to the right Team
- [ ] Three "no toilet paper" Reports become one Issue, "3 people reported"; "dirty toilet" stays separate
- [ ] A Question answered; then "Send to staff" creates a Report
- [ ] A joke is Bounced; after editing, it is accepted. After 3 Bounces, "Send anyway" goes to Needs review
- [ ] A spam wave: the device and QR code limits hit, then Strict mode; messages are Quarantined; an alert appears in Needs review
- [ ] An unknown QR code ID: the Report goes to Needs review
- [ ] A message about another place: the card shows "Mentioned: …"
- [ ] An Emergency phrase: the emergency screen appears instantly, and a Critical Issue is seen by `security@` and `manager@`
- [ ] Cleaning and Security accounts see different lists
- [ ] Both Romanian and English messages work
- [ ] The LLM is off (unset key): everything still flows as Unclassified

## Done when
- [ ] G1 and G2 checklists ticked, with any failures turned into tasks or fixes.
