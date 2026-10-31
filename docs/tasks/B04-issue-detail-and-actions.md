# B04: Issue detail, actions, hand over, overrides, conflicts

Status: IN PROGRESS
Owner: Luciano
Track: B · Phase 2 · Estimate: 2 h · Agent-friendly: partly
Depends on: B03 (real actions need A05)
Unblocks: B05

## Goal
One-tap handling of an Issue, with the full context one tap away and never hidden by the summary ([ADR-0007](../adr/0007-llm-grouping-and-summaries.md)).

## Read first
[api: Issue actions](../architecture/api.md#issue-actions) · [frontends: Staff app](../architecture/frontends.md#staff-app-appsflare-mobile-expo-router).

**Design:** [docs/design](../design/README.md): [foundations](../design/foundations.md), [components](../design/components.md), [screens](../design/screens.md), [mobile](../design/mobile.md), [reference screens](../design/reference/README.md).

## Do
1. **`src/app/issue/[id].tsx`:** the card header, then the action bar, the original Reports, and the history.
   - **Reports:** text, time, language, plus contact and flight number when present (label them "provided by passenger").
   - **History:** action, who, when.
2. **Actions:**
   - Acknowledge, I'm on it, Resolve and Reopen, shown only when the transition is valid
   - Hand over: `TeamPicker` from `/api/staff/teams` in a bottom sheet or modal (no new library)
   - Edit category or Severity: a simple picker
   
   Every action sends `version`. Wire the card's main action from B03 too.
3. **`useAction()`:**
   - optimistic disabling while sending
   - on `409 VERSION_CONFLICT`, refetch and show a toast ("{name} already took this" when `takenBy` changed, otherwise "This Issue changed, check again")
   - on `404`, go back with "No longer available"
4. **Confirmations** only for Resolve and Hand over: one extra tap, to avoid accidents.

## Notes
- **Built:** `app/issue/[id].tsx` (the card, category and Team, a "More actions" section, the Passengers' Reports with contact and flight number labelled "provided by the passenger", the history with from → to for changes), `hooks/use-issue.ts`, `hooks/use-action.ts` (busy while sending; on 409 it reloads and says "{name} already took this" or "This issue changed"; on 404 "No longer available" and back), `components/PickerSheet.tsx` (React Native `Modal`, no new library; Android back closes it), `components/Toast.tsx`, and `issues/transitions.ts`, the same rules as the back end's `IssueTransitions`, so only valid actions are shown.
- **Layout:** the status actions (I'm on it or Resolve, Acknowledge, Reopen) sit in the bottom bar within thumb reach; Hand over and Change category or Severity, used less often, sit in "More actions" in the page, so the bottom bar stays short. Resolve and Hand over ask for confirmation.
- **API:** `client.ts` gained `overrideIssue`, `handOverIssue`; a `200` with an empty body is no longer a network error. Mock mode follows the server's transitions and writes history lines.
- **Checked:** `npm run typecheck` and `npx eslint .` are clean, `node scripts/check-i18n.mjs` passes (including the new `category`, `history` and `reviewReason` families), and `npx expo export --platform android` bundles. **Not yet checked:** on a real phone in Expo Go, in mock mode and against the back end (the "Done when" items below).
- B03, B04 and B05 were built together and go in one PR (decisions.md, Round 15).

## Done when
- [ ] Every action works against the real back end and updates the list on the next poll.
- [ ] Two phones taking the same Issue show the conflict toast on the second.
- [ ] Hand over to Security makes the Issue disappear for Cleaning and appear for Security.
