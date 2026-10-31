# D04: RO/EN string files for both apps, i18n check script

Status: TODO
Owner:
Reviewer: Track B owner (the team's Romanian speaker reviews the RO text)
Track: D (OpenCode) · Phase 1 · Estimate: 45 min · Agent-friendly: yes
Depends on: W01, B01 (where the files live and their key style)

## Goal
Every UI string in both languages, plain and short, with a check that the two files never drift apart.

**Design:** [docs/design](../design/README.md): [foundations](../design/foundations.md), [components](../design/components.md), [screens](../design/screens.md), [mobile](../design/mobile.md), [reference screens](../design/reference/README.md).

## Do
1. **`apps/flare-webapp/src/i18n/{ro,en}.json`:** every key from the [passenger copy deck](../architecture/frontends.md#passenger-copy-deck-draft-d04-turns-it-into-string-files), plus `error.*` for every Problem Details `code` the public API can return, and the screen `<title>`s.
2. **`apps/flare-mobile/src/i18n/{ro,en}.json`:** every key from the [staff copy](../architecture/frontends.md#staff-copy-draft-d04-completes-it), plus:
   - `severity.*`, `category.*`, `team.*`, `status.*`
   - `priority.*` (with `{count}` and `{minutes}` placeholders)
   - `reviewReason.*`, `error.*`
   - empty states, sign-in texts, toasts, the confirmation dialogs, relative-time strings
3. **`scripts/check-i18n.mjs`** (Node built-ins only): for each app, load both files and report keys missing in either one, and placeholders (`{name}`) that differ. It exits with code 1 on any problem.
4. **Style:** plain words, short sentences, no jargon, polite second person (Romanian "tu" form is fine for the passenger app; keep it consistent). Correct Romanian diacritics (ș and ț with comma below).

## Done when
- [ ] `node scripts/check-i18n.mjs` passes.
- [ ] Neither app shows a raw key in any screen in mock mode.
- [ ] The team's Romanian speaker has read the RO strings.
