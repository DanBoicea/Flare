# B06: Staff app design and accessibility pass, real phones

Status: TODO
Owner:
Track: B · Phase 3 · Estimate: 1 h · Agent-friendly: no (uses the frontend-design skill in the main session)
Depends on: B05

## Goal
Zero-training, glanceable, calm and clearly human-made ([principles](../principles.md), [frontends](../architecture/frontends.md#shared-conventions)).

## Do
1. Use the **frontend-design** skill to refine the tokens, the card layout and the Severity treatment. Keep the shared tokens in step with W04.
2. **Accessibility:**
   - `accessibilityLabel` on every control
   - touch targets at least 48 px
   - check with large system text (iOS Larger Text, Android font scale 1.3)
   - VoiceOver and TalkBack read a card as one sentence: Severity, summary, place, people, age
3. Test on one real iPhone and one real Android phone through Expo Go, on Wi-Fi and on mobile data.
4. Check that both languages fit, since Romanian strings are longer.
5. Show the "simulated data" label in the header or sign-in.

## Done when
- [ ] Screenshots of the list and the detail in both languages are attached to the PR.
- [ ] Large-text and screen-reader checks pass.
- [ ] No layout breaks on either phone.
