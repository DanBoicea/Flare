# D09: Pitch description update, README setup, docs consistency

Status: TODO
Owner:
Reviewer: Track A owner (docs) and the presenter (pitch text)
Track: D (OpenCode) · Phase 3 · Estimate: 45 min · Agent-friendly: yes
Depends on: C05 (numbers), most features merged

## Do
1. **Pitch description:** rewrite the entry-page description in [decisions](../decisions.md#pitch-entry-page-description-drafted-2026-10-30), RO and EN, 2-3 sentences. Cover: the QR code, one field for a problem or a question, instant Knowledge base answers, routing to the right Team, grouping ("3 people reported"), spam and quality handling, honest QR code stance, simulated data.
2. **README:** check that the "Setup" section matches reality for all three apps (commands, env files, mock mode), and that "How it works" matches the built behaviour.
3. **Consistency pass:**
   - every glossary term is used consistently in `docs/**` and UI strings (no "ticket" for Issues, no "employee app")
   - links between docs aren't broken
   - every ADR's status is current
   - the task board statuses match the task files
   
   List findings in the PR, and fix the obvious ones.
4. **Pitch draft:** a short `docs/pitch-notes.md` with the numbers from `docs/eval-results.md` (copied, with the date), the limits list, and the mentor points with how each is addressed.

## Done when
- [ ] The reviewers approve.
- [ ] No broken links.
- [ ] The pitch description is updated in both languages.
