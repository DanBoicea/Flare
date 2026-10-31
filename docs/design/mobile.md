# Mobile and responsive rules

Almost every Passenger meets Flare on a phone, seconds after pointing the camera at a poster. These rules apply to every direction and both apps. They are as binding as the colours.

## The situation we design for

- **How people arrive.** The phone camera or Google Lens opens the link in Safari, Chrome, Samsung Internet or a QR app's built-in browser. There's no install and no warm-up: the first screen must make sense in two seconds.
- **The network is bad.** Foreign travellers often have roaming switched off, airport Wi-Fi may sit behind a sign-in page, and connections drop between areas. The page must be tiny, render without our fonts, and never lose typed text.
- **The body is busy.** People hold a bag or a coffee in one hand, stand in a queue, deal with glare from windows, or forgot their glasses.

## Widths and layout

| Width (CSS px) | Typical device | Rule |
| --- | --- | --- |
| 320 | iPhone SE (1st gen), small Android, 200% zoom on 640 | Must work: no horizontal scroll, nothing cut off |
| 360-393 | Most Android phones, iPhone 12-16 | **Primary design width** (mock-ups at 390 × 844) |
| 412-430 | Large phones (Pro Max, Galaxy Ultra) | Same single column; more air, not more columns |
| ≥ 600 | Tablets, desktop | Single column, max 30 rem (480 px), centred; page background continues |

- **One column, always,** for the passenger page. No side-by-side layouts.
- **Left-aligned text.** Centred text only for one-line items such as button labels.
- **Page side padding:** 20 px (16 px below 360 px). **Vertical rhythm:** 20 px between blocks, 8 px between a label and its field.
- **Line length:** at most about 34 rem (roughly 60 characters at 18 px) for reading text.

## Viewport and browser chrome (web)

```html
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover, interactive-widget=resizes-content">
<meta name="color-scheme" content="only light">
<meta name="theme-color" content="#FDECEE"><!-- matches the calm emergency strip, so the browser bar blends into it -->
```

- **Never disable zoom.** No `maximum-scale` or `user-scalable=no`.
- **Use `100dvh` / `100svh`, never `100vh`,** for full-height layouts, because iOS toolbars change the viewport height.
- **Safe areas:** pad the strip with `env(safe-area-inset-top)`, the footer and any bottom bar with `env(safe-area-inset-bottom)`, and both sides in landscape with `env(safe-area-inset-left/right)`.
- **Dark mode:** light only for the hackathon. `only light` opts out of Chrome's automatic dark mode. Samsung Internet's own dark mode may still invert colours, so test it.

## Keyboard and input

- **No fixed or sticky bottom bars on the passenger page.** The keyboard covers them, and iOS positions fixed elements badly while typing. The Send button sits directly under the field, in normal page flow.
- **Text size in fields:** at least 16 px, or iOS zooms on focus. We use 20 px.
- **Message field:** grows with its content (`field-sizing: content`, falling back to `rows="5"`), up to about 40% of the viewport height, then scrolls.
- **Attributes:** `enterkeyhint="send"` on the message field. The phone field uses `type="tel"`, `inputmode="tel"` and `autocomplete="tel"`.
- **Focus:** when a field gains focus, keep it visible above the keyboard (`scroll-margin-block: 24px`). Never auto-focus on load, because the keyboard would cover the page before the Passenger has read it.
- **Pasting** always works. Dictation from the phone keyboard is the recommended way for people who struggle to type, and the hint under the field says so.

## Touch

- **Target sizes:** at least **48 × 48 px** for every target (48 dp in the Staff app), and **56 px tall** for main passenger buttons. Keep at least 8 px between adjacent targets.
- **Thumb reach:** main actions are full width, in the lower half of the content, and stacked vertically (main action first). Rarely used controls, such as language, live at the top.
- **Press feedback:** every button shows a pressed state (darker fill or a 1 px shift). Nothing depends on hover.
- **No double submits:** after Send, the button is replaced by the sending state right away.
- **Swipes:** no gestures are required for anything. Swiping is at most a shortcut for a visible button (Staff app).

## Text size and zoom

- **Web:** sizes in `rem`, so the browser's text size setting applies. The layout must hold at 200% zoom: at 320 px wide, that is the same as 640 px at 200%.
- **Staff app:** `allowFontScaling` stays on. Rows and buttons grow with text, with no fixed heights. Tab bar labels may cap at `maxFontSizeMultiplier={1.5}`. Nothing else is capped.
- **Long words:** Romanian has long words ("funcționează", "dezinfectanți"). Allow wrapping with `overflow-wrap: anywhere` on user text, and never truncate a title to one line. Two lines at most in lists, then an ellipsis. Full text in the detail view.

## Orientation

Portrait is primary, but landscape must work: no orientation lock. In landscape the emergency strip stays one line and the content column stays at most 480 px wide.

## Performance budget (passenger page)

| Item | Budget | Now (W01) |
| --- | --- | --- |
| JavaScript (gzip) | ≤ 100 KB | 73 KB |
| CSS (gzip) | ≤ 10 KB | 1.2 KB |
| Fonts | ≤ 55 KB, `font-display: swap`, self-hosted | none yet |
| Images | none (inline SVG icons, CSS shapes) | none |
| First readable render on slow 4G | < 2 s | to measure in W04 |

Self-host the fonts. Google Fonts' CDN would send every Passenger's IP address to a third party, which a GDPR-minded service should avoid, and the page must not depend on it.

## Staff app (React Native)

- **Safe areas** with `react-native-safe-area-context` (already installed). The Issue detail action bar sits above the home indicator.
- **Sign-in:** `KeyboardAvoidingView` (or the navigator's keyboard handling), so the button stays visible above the keyboard.
- **Lists:** `FlatList` with rows that grow with text. Don't set `numberOfLines` on anything except list titles (2 lines).
- **Main actions** sit at the bottom of the detail screen, within thumb reach. List rows have one quick action at most.
- **Both platforms:** test on iOS and Android through Expo Go. Android's back button must close sheets and dialogs before leaving the screen.

## Test matrix (W04, B06, S07)

| Check | How |
| --- | --- |
| 320, 360, 390, 430 px | Chrome DevTools device mode, plus `node docs/design/explorations/render.mjs` for screenshots |
| A real iPhone and a real Android phone | Scan a printed poster, send a message, use the Staff app over mobile data |
| 200% browser zoom, phone text size at maximum | Passenger page and Staff app |
| Landscape | Both apps |
| Slow 3G and offline | DevTools throttling: text kept, honest error, retry works |
| The Passenger's QR route | iOS camera to Safari, Google Lens to Chrome, Samsung camera to Samsung Internet |
| Samsung Internet dark mode | Page stays readable |
