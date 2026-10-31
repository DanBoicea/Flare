# B03: Active and Resolved lists, polling, Issue cards

Status: IN PROGRESS
Owner: Luciano
Track: B · Phase 1 · Estimate: 2 h · Agent-friendly: partly (presentational components from a spec can go to OpenCode)
Depends on: B02 (real data needs A04)
Unblocks: B04, G1

## Goal
A glanceable list of the Issues this staff member should act on, updating live ([ADR-0011](../adr/0011-staff-app-polling.md)).

## Read first
[frontends: Staff app](../architecture/frontends.md#staff-app-appsflare-mobile-expo-router) · [api: `IssueCard`](../architecture/api.md#get-apistaffissuesviewactiveresolved) · [product: Staff app requirements](../product.md#staff-app-requirements).

**Design:** [docs/design](../design/README.md): [foundations](../design/foundations.md), [components](../design/components.md), [screens](../design/screens.md), [mobile](../design/mobile.md), [reference screens](../design/reference/README.md).

## Do
1. **`useIssueFeed(view)`:**
   - polls `ACTIVE` every 5 s while `AppState` is `active`; pauses in the background and polls at once on return
   - on error, keeps the last good list and shows an offline banner
   - tracks seen IDs, highlighting new Issues for 10 s
   - exposes `refresh()`
2. **`IssueCard`:**
   - `SeverityBadge` (colour, icon and word)
   - the summary, with an "AI summary" tag, or "Unclassified"
   - the Location name and label
   - "Mentioned: …"
   - the "N people reported · M min" line from priority reasons
   - the age (relative, localised)
   - `takenBy`
   - Emergency and Reopened badges
   
   It has one big main action ("I'm on it" when `NEW` or `ACKNOWLEDGED`, "Resolve" when taken by me), wired in B04. Until then it is visible but disabled.
3. **Active screen:**
   - Critical first (the server already sorts)
   - pull to refresh
   - an empty state ("Nothing needs you right now")
   - the new-Issue highlight and a badge count on the tab
4. **Resolved screen:** a one-time fetch on focus, plus pull to refresh.
5. **Plain text only:** all text through `Text`. Never render data as markup.

## Notes
- **Built:** `hooks/use-issue-feed.ts` (5 s polling while the app is in the foreground, paused in the background and reloaded on return; offline banner keeping the last list; new Issues highlighted for 10 s; `refresh()`), `components/IssueCard.tsx` (extends the V1 card: Severity badge, AI or Unclassified tag, place and label, "Mentioned", "N people reported · M min" from the REPORTERS reason, age, who is on it, Emergency, Reopened and New tags, one quick action), the Active screen (pull to refresh, empty state, new-Issue count on the tab) and the Resolved screen (loaded on focus, pull to refresh).
- The quick action is wired (B04 built at the same time): "I'm on it" when the Issue can be taken, "Resolve" (with a confirmation) when I hold it.
- **Checked:** `npm run typecheck` and `npx eslint .` are clean, `node scripts/check-i18n.mjs` passes (including the new `category`, `history` and `reviewReason` families), and `npx expo export --platform android` bundles. **Not yet checked:** on a real phone in Expo Go, in mock mode and against the back end (the "Done when" items below).
- B03, B04 and B05 were built together and go in one PR (decisions.md, Round 15).

## Done when
- [ ] In mock mode and against the real back end, the lists show cards matching the contract.
- [ ] A new Issue appears within about 5 s, highlighted.
- [ ] Backgrounding the app stops requests (check the back-end log).
- [ ] Gate G1 passes with the web app.
