# Accessibility

The target is **WCAG 2.2 level AA** for the passenger web page and the same intent for the Staff app (React Native has no formal WCAG conformance, but every criterion below has a native equivalent). Our users include elderly people with low vision, people under stress, people with one free hand, colour-blind staff and non-Romanian speakers. Accessibility is the design, not an extra.

## Checklist (what each criterion means for Flare)

| WCAG 2.2 | Requirement for Flare | Where |
| --- | --- | --- |
| 1.1.1 Non-text content | Icons next to text are hidden from screen readers (`aria-hidden`). Icon-only buttons have a label ("Mai multe acțiuni"). | Both apps |
| 1.3.1 Info and relationships | Real headings (one `h1` per screen), `label` for every field, lists as lists, the Staff list as a list of articles. | Both |
| 1.3.4 Orientation | No orientation lock. | Both |
| 1.3.5 Input purpose | `autocomplete="tel"` on the phone field; `email` and `current-password` on Staff sign-in. | Both |
| 1.4.1 Use of colour | Severity always shows colour, icon shape and word together. Status always has a word. "New" has a dot and the word "Nou". | Staff app |
| 1.4.3 Contrast (text) | 4.5:1 for body text, 3:1 for text ≥ 24 px (or 18.66 px bold). The checked token pairs are in [foundations](foundations.md#colour). | Both |
| 1.4.4 Resize text | Works at 200% text size. `rem` on the web; font scaling on in React Native. | Both |
| 1.4.10 Reflow | No horizontal scroll at 320 CSS px. | Web |
| 1.4.11 Non-text contrast | Field borders, button boundaries, focus indicators and severity bars ≥ 3:1 against neighbours. Yellow is never a boundary on white: it is always framed in black. | Both |
| 1.4.12 Text spacing | Layouts survive user-increased line height and letter spacing: no fixed-height text boxes. | Web |
| 1.4.13 Content on hover/focus | No tooltips that carry information. | Both |
| 2.1.1 Keyboard | Everything works with a keyboard or switch control: Send, disclosure, language, call link. | Web |
| 2.4.3 Focus order | Strip, language, title, field, contact, Send, trust notes. On each new screen, focus moves to its heading. | Web |
| 2.4.6 Headings and labels | Headings describe the screen ("Răspuns automat", "Mulțumim…"). Labels are visible, not only placeholders. | Both |
| 2.4.7 / 2.4.11 Focus visible / not obscured | Two-tone focus ring (see [foundations](foundations.md#focus)), never hidden by a sticky bar. | Web |
| 2.5.3 Label in name | A button's accessible name starts with its visible text ("Sună la 112"). | Both |
| 2.5.7 Dragging movements | No dragging is needed. Swipe actions in the Staff app always have a button too. | Staff |
| 2.5.8 Target size | We go far beyond the 24 px minimum: 48 px for every target, 56 px for main passenger buttons. | Both |
| 3.1.1 / 3.1.2 Language | `lang="ro"` or `lang="en"` on the page, updated when the language changes. The language button has `lang` set to the language it offers. | Web |
| 3.2.3 / 3.2.6 Consistent navigation and help | The emergency strip and language button sit in the same place on every screen. That consistent help is required by 3.2.6. | Web |
| 3.3.1 Error identification | Errors are text, next to the problem, announced with `role="alert"`. | Both |
| 3.3.7 Redundant entry | Typed text survives errors, Bounces and going back. | Web |
| 3.3.8 Accessible authentication | Staff sign-in allows password managers and paste. No puzzles. | Staff |
| 4.1.3 Status messages | "Verificăm mesajul…" and the result are announced (`aria-live="polite"`). Emergency instructions use `role="alert"`. | Web |

## Beyond the minimum (because of who our users are)

- **Typeface and look-alike letters.** Instrument Sans (ADR-0021) draws capital I and lowercase l identically, and O and 0 almost identically (checked on 2026-10-31). So in Passenger copy and the Knowledge base, avoid words where the confusion changes meaning, above all a sentence starting with "Ia" (it reads as "la"): write "Adu cu tine…" instead of "Ia cu tine…". Location codes like `ARR-WC-02` appear only in the Staff app; They use Instrument Sans in a pill, as in the mock-ups; the letters-dash-digits format keeps O and 0 apart in practice (decided 2026-10-31).
- **Plain language.** Short sentences, everyday words, one instruction per screen. See [content](content.md).
- **Calm emergency.** The 112 strip is clear but not alarming. Fear makes elderly users abandon a page.
- **Colour-vision checks.** Every Staff screen is reviewed with deuteranopia and achromatopsia simulations (`render.mjs` produces them). Severity must be readable from icon shape and word alone.
- **Motion.** One meaningful animation (the sending beacon), switched off under `prefers-reduced-motion`. Nothing flashes more than three times per second.
- **No time limits.** Nothing on the passenger page expires while the Passenger types.

## React Native equivalents (Staff app)

| Need | Use |
| --- | --- |
| Name, role, state | `accessibilityLabel`, `accessibilityRole` ("button", "header", "link"), `accessibilityState` ({ disabled, selected, busy }) |
| A card read as one sentence | `accessible` on the row container, with an `accessibilityLabel` such as "Critic. Persoană căzută lângă poarta 3. Poarta 3. 2 persoane. Acum 1 minut. Nou." |
| Announcements | `AccessibilityInfo.announceForAccessibility` for new Critical Issues and action results; `accessibilityLiveRegion="polite"` on Android banners |
| Text size | `allowFontScaling` on (the default); no fixed heights |
| Targets | `minHeight: 48`, `minWidth: 48`, `hitSlop` for small icons |
| Reduced motion | `AccessibilityInfo.isReduceMotionEnabled()`, which turns off the new-Issue fade |

## How we test (W04, B06)

1. **Automated:** Lighthouse accessibility ≥ 95 and axe DevTools with no violations on every passenger screen.
2. **Screen readers:** VoiceOver (iOS Safari, Staff app on iOS) and TalkBack (Android Chrome, Staff app on Android). Complete the main flow eyes-closed.
3. **Keyboard only** on desktop: complete the passenger flow.
4. **Zoom:** 200% browser zoom at a 640 px window (the same as 320 px), and the maximum phone text size.
5. **Colour vision:** the CVD screenshots from `render.mjs`.
6. **Real people:** the team's Romanian speaker reads every string aloud. If possible, one person over 65 tries the passenger flow on their own phone.
