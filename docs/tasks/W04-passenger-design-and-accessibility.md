# W04: Web design and accessibility pass, slow-network test

Status: IN PROGRESS
Owner: BoiceaDan
Track: B (or C if B is behind) · Phase 3 · Estimate: 1 h · Agent-friendly: no (frontend-design skill in the main session)
Depends on: W03

## Goal
The passenger web app looks calm, trustworthy and clearly human-made, and works for elderly and non-technical people ([principles](../principles.md)).

**Design:** [docs/design](../design/README.md): [foundations](../design/foundations.md), [components](../design/components.md), [screens](../design/screens.md), [mobile](../design/mobile.md), [reference screens](../design/reference/README.md).

## Do
1. Use the **frontend-design** skill on all six screens: hierarchy, spacing, the emergency strip's prominence without alarm, and button sizes (main buttons at least 56 px tall). Keep the tokens in step with B06.
2. **Accessibility audit:**
   - contrast checked with browser dev tools
   - 200% zoom
   - keyboard only
   - VoiceOver on iOS Safari and TalkBack on Android Chrome
   - `lang` attribute switches with the language
   - meaningful page `<title>` per screen
3. **Performance:** Lighthouse mobile, with performance and accessibility each at 90 or above. No web fonts or images over 50 KB.
4. **Throttling:** slow 3G gives a usable Compose in under 3 s.
5. **Copy:** the team's Romanian speaker reads every RO string, and fixes go to `ro.json`.

## Progress (2026-10-31)
- **Design pass (frontend-design):** header 12 px shorter (more room for the field with the keyboard open); the question on Answer is plain text instead of a second card (V1 design 03); the rest already met the brief (56 px main buttons, one cobalt action per screen, one animation, reduced motion respected).
- **Contrast, computed for every V1 pair in use:** all text passes AA, the lowest being white on cobalt and on red at 5.9:1. Fixed: the phone field's border was #CBD8EF (1.4:1 against the 3:1 needed for field outlines, WCAG 1.4.11), now `--ink-2` (7.2:1).
- **`lang`:** switches with the language (I18nProvider). The Passenger's own text is marked `lang=""`.
- **Page title per screen:** "<screen> – Flare", in both languages (`page.*` keys).
- **Look-alike letters:** Instrument Sans draws "Ia" and "la" identically. The one Knowledge base sentence starting "Ia cu tine" now says "Adu cu tine"; the rule is in [accessibility](../design/accessibility.md).
- **Fonts and images:** fonts are 30 KB (latin) and 11 KB (latin-ext); there are no images. Production transfer is about 0.4 KB HTML + 75 KB JS + 2.7 KB CSS (gzip) + the fonts.
- **200% zoom:** checked at a 320 px width in W02 and W03.

## Done when
- [ ] Lighthouse scores and screenshots in RO and EN are attached to the PR. *By hand: Chrome DevTools, Lighthouse tab, Mobile, on `npm run build && npm run preview`.*
- [ ] The accessibility checks pass. *Done: contrast, zoom, `lang`, titles. By hand: keyboard only, VoiceOver on iOS Safari, TalkBack on Android Chrome, slow-3G load of Compose under 3 s.*
- [ ] The RO copy has been reviewed by the team's Romanian speaker (`apps/flare-webapp/src/i18n/ro.json`).
