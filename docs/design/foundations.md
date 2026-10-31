# Foundations

The tokens and rules every Flare surface uses: the passenger web app, the Staff app, posters, slides. The live reference is [reference/](reference/), built from `reference/flare.css`. If this file and that CSS disagree, fix both in the same PR.

## The idea: airfield signage

Airports already have a visual language that everyone obeys without training: the airfield sign families of ICAO Annex 14. Flare borrows its three families and gives each exactly one job:

| Sign family (ICAO) | Look | In Flare it means |
| --- | --- | --- |
| Mandatory instruction | White on red | **Emergency.** The 112 button, the emergency screen, Critical severity |
| Direction | Black on yellow, black frame | **Press this.** The one main action on a screen |
| Location | Yellow on black, yellow border | **This place.** Location plates, the Flare logo, poster headers |

Everything else is quiet: white paper, dark ink, hairlines. The signage colours appear only where they carry meaning. This is inspired by the international standard. It is **not** a copy of Craiova Airport's real signage, which we have not documented.

## Colour

### Tokens

| Token | Hex | Role |
| --- | --- | --- |
| `black` | #000000 | Sign black: frames around yellow, plates, staff header |
| `ink` | #1A1C1F | Body text |
| `ink-2` | #4A4F57 | Secondary text: hints, meta, captions |
| `ink-3` | #646B74 | Placeholder text, quiet control borders |
| `paper` | #FFFFFF | Page |
| `paper-2` | #F2F4F5 | Sunken surfaces: quotes, place box |
| `line` | #D5D9DD | Hairline dividers (decorative only) |
| `signal` | #FFC20E | Yellow: action fill, or ink on black for location signs |
| `danger` / `danger-ink` / `danger-soft` | #C8102E / #A0102A / #FDECEE | Emergency and Critical |
| `warn` / `warn-ink` / `warn-soft` | #D9480F / #9C3B00 / #FFF0E6 | High severity; the Bounce notice |
| `info` / `info-ink` / `info-soft` | #4A86DC / #1A4F91 / #E9F1FB | Medium severity |
| `quiet` / `quiet-ink` / `quiet-soft` | #8A9099 / #474D55 / #EEF0F2 | Low severity |
| `ok` / `ok-ink` / `ok-soft` | #1E7B4A / #17623B / #E7F4EC | Done, resolved, the live indicator |

### Rules

1. **A yellow fill means "press this."** Use one main action per screen, and per list row at most. Yellow is never used as a decorative background, a status, or "new".
2. **Yellow on white always gets a 2 px black frame.** Without it the contrast is 1.6:1. With it the boundary is 21:1.
3. **Yellow ink on black means a location sign:** the logo, Location plates, poster headers. Nothing else.
4. **Red is for emergencies and Critical only.** Normal screens show it as the calm pink strip with a red 112 button. Solid red fills the screen only on the Emergency screen.
5. **Severity is never colour alone:** always colour, icon shape and word together. The severity bars get lighter in order, Critical (luminance 0.128) < High (0.194) < Medium (0.237) < Low (0.276), so greyscale keeps the order.
6. **Light theme only** for the hackathon (`color-scheme: only light`). A dim theme for night shifts in the Staff app is stretch ([reviews](reviews.md)).

### Contrast (WCAG 2.x, computed by script on 2026-10-31)

| Pair | Ratio | Needed |
| --- | --- | --- |
| `ink` on `paper` | 17.1:1 | 4.5 |
| `ink-2` on `paper` / on `paper-2` | 8.2:1 / 7.5:1 | 4.5 |
| `ink-3` on `paper` | 5.4:1 | 4.5 |
| black on `signal` (buttons) | 13.0:1 | 4.5 |
| `signal` on black (plates, logo) | 13.0:1 | 4.5 |
| white on `danger` (112 button, Critical badge) | 5.9:1 | 4.5 |
| `danger-ink` on `danger-soft` (strip) | 7.1:1 | 4.5 |
| `warn-ink` on `warn-soft`, `info-ink` on `info-soft`, `quiet-ink` on `quiet-soft`, `ok-ink` on `ok-soft` | 6.2, 7.2, 7.5, 6.5:1 | 4.5 |
| Severity bars on white (`warn`, `info`, `quiet`) | 4.3, 3.7, 3.2:1 | 3 (non-text) |
| Header grey #C4C8CE on black | 12.5:1 | 4.5 |
| `signal` on `paper` | **1.6:1** | never used without a black frame |

## Typography

### Typeface: Atkinson Hyperlegible Next

The typeface was designed by the Braille Institute for low-vision readers and is free under the SIL Open Font License. Its letters are drawn to be told apart: I/l/1, O/0 (slashed zero), and rn/m. In the round-1 tests, the system font turned "Ia cu tine" into "la cu tine"; this one did not.

- **Romanian support was checked** by parsing the font's character map on 2026-10-31: ș ț Ș Ț (comma below), ă â î, „ ” and … are all present.
- **Size:** one variable font covers weights 200-800. Romanian needs two subsets, latin (33 KB) and latin-ext (19 KB), so about 52 KB in total, loaded only when needed.
- **Self-host it, never load it from a CDN.** Google's CDN would send every Passenger's IP address to a third party (GDPR), and without the font the I/l confusion returns.
- **Web:** put `woff2` files in `apps/flare-webapp/src/assets/fonts/` with `@font-face`, `font-display: swap`, and `unicode-range` per subset, plus `OFL.txt`.
- **Staff app:** use static `.ttf` files (Regular 400, Bold 700, ExtraBold 800) in `apps/flare-mobile/assets/fonts/`, loaded with `expo-font`, which is already installed. React Native handles variable fonts unreliably, so use the static files.
- **Fallback stack:** `system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`.
- **Open question:** the font draws a slashed zero, so times read "10:Ø2". If the font offers an unslashed alternate, use it in running text and keep the slashed zero in codes. W04 checks this.
- **Decision:** adding the font files is a new asset. [ADR-0020](../adr/0020-design-language-airfield-signage.md) asks the team to accept it.

### Scale

| Token | Web | Staff app (dp) | Weight | Use |
| --- | --- | --- | --- | --- |
| `display` | 32 px / 1.15 | none | 800 | Emergency title only |
| `h1` | 28 px / 1.2 | 30 / 36 | 700 (staff 800) | Screen titles |
| `h2` | 24 px / 1.25 | 24 / 30 | 800 | Staff detail title |
| `h3` | 22 px / 1.25 | 20 / 26 | 700 | Notice titles; Staff row titles (20) |
| `lead` | 20 px / 1.45 | 18 / 26 | 400 | Typed text, answers, button labels |
| `body` | 18 px / 1.5 | 17 / 24 | 400 / 700 | Everything else |
| `caption` | 16 px / 1.5 | 16 / 22 | 400 / 700 | Meta, footers, badges. **The minimum, everywhere** |

- **Weights:** 400 for text, 700 for emphasis and headings, 800 for the logo, emergency title and buttons. Nothing lighter than 400.
- **Sentence case,** with no all-caps labels. Codes such as `ARR-WC-02` are content, not styling.
- **Numbers:** use `font-variant-numeric: tabular-nums` for times, counts and codes.
- **Line length:** reading text is at most about 60 characters (the page column is at most 36 rem).

## Space

Space comes from a 4 px grid: **4, 8, 12, 16, 20, 24, 32, 40**.

| Setting | Value |
| --- | --- |
| Page side padding | 20 px (16 px below 360 px wide) |
| Between blocks | 20 px |
| Between a label and its field | 8 px |
| Between stacked buttons | 12 px |
| Staff rows | 16 px vertical padding |

## Shape, lines, elevation

| Token | Value | Use |
| --- | --- | --- |
| `r-sign` | 3 px | Plates, logo, severity badges, tags: square like physical signs |
| `r-control` | 8 px | Buttons, fields, the language button |
| `r-card` | 12 px | Answer and notice cards, quotes, the place box, sheets |
| `frame` | 2 px solid black | Main and secondary buttons, the message field, the answer card |
| `hairline` | 1 px solid `line` | Dividers, list rows, footer |

- **No drop shadows** on cards or buttons. Elevation exists only for bottom sheets and modals (`0 -8px 24px rgba(0,0,0,.16)`).
- **Corner radius differs by hierarchy** on purpose. Signs are square, controls are softer, containers softest.

## Focus

All interactive elements get a two-tone ring that is visible on every background:

```css
:focus-visible { outline: 3px solid #000; outline-offset: 2px; box-shadow: 0 0 0 2px #fff; }
.s-head :focus-visible, .strip :focus-visible { outline-color: #fff; box-shadow: 0 0 0 2px #000; }
```

## Icons

- **Style:** one stroke style everywhere, Feather/Lucide shapes (MIT/ISC licences), on a 24 px grid with a 2 px stroke and round caps.
- **Sizes:** 20-24 px next to text, 18 px inside badges.
- **Meaning:** icons always sit next to a word, except the "Mai multe" (⋯) button, which has an accessible label.
- **Web:** inline SVG symbols (see `reference/index.html`). No icon font and no library.
- **Staff app:** the same SVG shapes need `react-native-svg` (Expo-supported, but a **new dependency**: B01 asks the team). The alternative is `expo-symbols`, which is already installed but iOS-first and looks different on Android.

| Meaning | Icon |
| --- | --- |
| Call 112, call-back | `phone` |
| Language | `globe` |
| Never asks for payment | `lock` |
| Information, automatic answer | `info` |
| Edit, Bounce | `edit` (pencil) |
| Done | `check` (in a round green badge) |
| Critical | octagon, **filled**, with "!" |
| High | triangle with "!" |
| Medium | circle with a horizontal bar |
| Low | circle with a down arrow |
| Reported by N | `users` |
| Taken by someone | `user-check` |
| Opens detail | `chevron-right` |
| Active, Resolved, Needs review tabs | `list`, `check-circle`, `inbox` |
| More actions | `more-horizontal` |

**Never:** sparkles or robots for AI, emoji, flags for languages (flags are countries, not languages), or arrows appended to button text.

## Motion

- **One designed moment: the sending beacon.** A black-framed ring pulses out from the yellow dot, 1.8 s ease-out, looping. It means "your message is on its way".
- **Everything else answers an action:** a pressed button moves 1 px down, and sheets slide in within 200 ms. Nothing animates on page load.
- **Staff app:** a new Issue appears with a 10-second soft highlight, with no movement.
- **`prefers-reduced-motion`** (web) and **Reduce Motion** (React Native) turn all of it off. The beacon then shows as a static ring.

## Brand

- **The logo is a location sign:** "Flare" set in Atkinson Hyperlegible Next 800, yellow #FFC20E on black, with a 2 px yellow inner border and a 3 px radius.
  - On the passenger page it sits top left, above "Aeroportul Internațional Craiova".
  - In the Staff app it sits on the black header next to "Personal".
  - On posters it appears next to the airport name.
- **App icon** (stretch): the plate on a black square, with only the "F".
- **No airport logos or official marks without the airport's written permission.** We use the airport's name as plain text only, always next to "Demonstrație cu date simulate".
- **The name:** in a sentence, "Flare". On the plate, "Flare". Never "FLARE" or "flare".

## Tokens in code

**Web** (`apps/flare-webapp/src/styles/tokens.css`) uses the custom properties in [`reference/flare.css`](reference/flare.css) (the `:root` block). Copy it verbatim.

**Staff app** (`apps/flare-mobile/src/constants/theme.ts`, extends the template file):

```ts
export const color = {
  black: '#000000', ink: '#1A1C1F', ink2: '#4A4F57', ink3: '#646B74',
  paper: '#FFFFFF', paper2: '#F2F4F5', line: '#D5D9DD', signal: '#FFC20E',
  danger: '#C8102E', dangerInk: '#A0102A', dangerSoft: '#FDECEE',
  warn: '#D9480F', warnInk: '#9C3B00', warnSoft: '#FFF0E6',
  info: '#4A86DC', infoInk: '#1A4F91', infoSoft: '#E9F1FB',
  quiet: '#8A9099', quietInk: '#474D55', quietSoft: '#EEF0F2',
  ok: '#1E7B4A', okInk: '#17623B', okSoft: '#E7F4EC',
  headerMuted: '#C4C8CE', live: '#3DD68C',
} as const

export const severity = {
  CRITICAL: { bar: color.danger, badgeBg: color.danger, badgeText: color.paper, rowBg: color.dangerSoft, icon: 'octagon-filled' },
  HIGH: { bar: color.warn, badgeBg: color.warnSoft, badgeText: color.warnInk, rowBg: color.paper, icon: 'triangle' },
  MEDIUM: { bar: color.info, badgeBg: color.infoSoft, badgeText: color.infoInk, rowBg: color.paper, icon: 'circle-bar' },
  LOW: { bar: color.quiet, badgeBg: color.quietSoft, badgeText: color.quietInk, rowBg: color.paper, icon: 'circle-down' },
} as const

export const font = { regular: 'AtkinsonHyperlegibleNext-Regular', bold: 'AtkinsonHyperlegibleNext-Bold', extraBold: 'AtkinsonHyperlegibleNext-ExtraBold' }
export const size = { caption: 16, body: 17, lead: 18, rowTitle: 20, detailTitle: 24, title: 30 }
export const space = { 1: 4, 2: 8, 3: 12, 4: 16, 5: 20, 6: 24, 8: 32 }
export const radius = { sign: 3, control: 8, card: 12, sheet: 16 }
export const tap = { min: 48, main: 56 }
```
