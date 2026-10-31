# W04: Web design and accessibility pass, slow-network test

Status: TODO
Owner:
Track: B (or C if B is behind) · Phase 3 · Estimate: 1 h · Agent-friendly: no (frontend-design skill in the main session)
Depends on: W03

## Goal
The passenger web app looks calm, trustworthy and clearly human-made, and works for elderly and non-technical people ([principles](../principles.md)).

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

## Done when
- [ ] Lighthouse scores and screenshots in RO and EN are attached to the PR.
- [ ] The accessibility checks pass.
- [ ] The RO copy has been reviewed by the team's Romanian speaker.
