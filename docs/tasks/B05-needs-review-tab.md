# B05: Needs review tab for the duty manager

Status: TODO
Owner:
Track: B · Phase 2 · Estimate: 1 h · Agent-friendly: yes (OpenCode, from the spec plus the existing components)
Depends on: B04 (real data needs A09)

## Goal
The duty manager sees what the system held back and decides with one tap ([security and abuse](../security-and-abuse.md)).

## Do
1. **`src/app/(tabs)/review.tsx`:** polls `/api/staff/review` every 10 s while focused.
2. **Alerts first:** Strict mode banners with the QR code's Location label, how long it has lasted and the message count.
3. **Items:**
   - the reason as a translated label (Rate limit, Unknown QR code, Muted QR code, Bot check, Sent anyway, Strict mode)
   - the text, the time, and the Location or "Unknown QR code: {id}"
   - actions: Release (opens `TeamPicker` with "No Team" as the first option) and Dismiss
4. An empty state, and RO/EN strings for every reason.

## Done when
- [ ] With the spam and unknown-QR-code scenarios (D06), the items and the alert appear.
- [ ] Release to a Team makes an Issue appear for that Team.
- [ ] Dismiss removes the item.
