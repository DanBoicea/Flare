# Flare design system

One visual language for the passenger web app, the Staff app, the QR posters and the pitch. Read this before building any screen.

> **Current design: [Design V1](flare-design-v1/README.md)** ([ADR-0021](../adr/0021-design-v1-navy-cobalt-instrument-sans.md)): navy and cobalt, Instrument Sans. Its tokens are in [foundations](foundations.md). The airfield-signage text below, [components](components.md), [screens](screens.md) and [reference/](reference/README.md) describe the superseded direction (ADR-0020); keep their layout, behaviour and accessibility rules, but take colours, type and shapes from V1. Where they disagree, V1 wins.

## In one paragraph

Flare looks like **airfield signage**: the international sign families that airport people already obey. **White on red** is mandatory, so it means *emergency* (112). **Black on yellow** is direction, so it means *press this*. **Yellow on black** is location, so it means *this place* (Location codes and the logo). Everything else is quiet white paper and dark ink, set in **Atkinson Hyperlegible Next**, a typeface designed for low-vision readers. It was chosen by testing three directions with five persona reviewers ([reviews](reviews.md)). All five picked it, and their must-fixes are built into the reference.

## Principles

1. **Readable first.** Elderly eyes, glare, one thumb: 18 px text, 56 px main buttons, high contrast, a hyperlegible typeface.
2. **Every colour has one meaning.** Red is emergency, yellow fill is action, yellow on black is place, the severity colours are severity. If a colour appears without its meaning, remove it.
3. **Official, not flashy.** Flare must feel like part of the airport, because people decide in two seconds whether a QR page is a scam. No gradients, glows, emoji or "AI sparkle", and no template chrome.
4. **Calm under stress.** One clear action per screen. The emergency is clear but not alarming. Honest words about what happens next and about AI.
5. **Mobile by default.** Designed at 390 px, working at 320 px and at 200% zoom, with the keyboard open, on slow networks ([mobile](mobile.md)).
6. **Accessible by construction.** WCAG 2.2 AA is the floor ([accessibility](accessibility.md)).

## Files

| File | Contents |
| --- | --- |
| [foundations.md](foundations.md) | Colour (with measured contrast), type, space, shape, focus, icons, motion, brand, and tokens for both apps |
| [components.md](components.md) | Every component: anatomy, sizes, states, accessibility |
| [screens.md](screens.md) | Every passenger and staff screen, and the QR poster: layout and behaviour |
| [mobile.md](mobile.md) | Widths, viewport, keyboard, touch, performance budget, test matrix |
| [accessibility.md](accessibility.md) | WCAG 2.2 AA checklist mapped to Flare, React Native equivalents, how we test |
| [content.md](content.md) | Voice, honesty rules, Romanian typography, RO/EN UI vocabulary |
| [reviews.md](reviews.md) | How the direction was chosen: persona reviews, findings and changes |
| [flare-design-v1/](flare-design-v1/README.md) | **The visual source of truth:** the chosen V1 mock-ups (static HTML) |
| [reference/](reference/README.md) | The superseded signage reference screens and the render script |
| [explorations/](explorations/README.md) | The three round-1 directions (frozen record) |
| [open-design-prompt.md](open-design-prompt.md) | Open brief for a **new, different** design from Claude Design, Lovable or v0 (product, content and constraints fixed, look free) |
| [claude-design-prompt.md](claude-design-prompt.md) | A prompt for producing high-fidelity mock-ups or slides in another Claude session |

## How to use it

- **Building a screen (W02-W04, B01-B06):** open its reference screen (`docs/design/reference/index.html?screen=…`), read its section in [screens](screens.md) and its components, and copy the tokens. Don't invent new colours, sizes or components. If something is missing, add it here first, in the same PR.
- **Checking your work:** render your real screen at 390 px and 320 px, and compare it with the reference. Run the checks in [accessibility](accessibility.md#how-we-test-w04-b06).
- **Changing the design:** edit `reference/flare.css` and `reference/index.html` together with these docs, re-render (`node docs/design/reference/render.mjs`), and note the change in [decisions](../decisions.md). A change of direction needs a new ADR.

## Open decisions (team)

- **Typeface files:** accept adding Atkinson Hyperlegible Next to both apps (ADR-0020).
- **Severity colours** for High, Medium and Low in V1 (B01, ADR-0021).
- **Staff app icons:** `react-native-svg`, a new dependency (B01).
- **"tu" or "dumneavoastră"** when addressing Passengers ([reviews](reviews.md)).
- **Location on the passenger page:** showing the scanned code would help Passengers verify the poster, but conflicts with ADR-0006 ([reviews](reviews.md)).
