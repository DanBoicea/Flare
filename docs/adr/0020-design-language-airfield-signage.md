---
status: proposed
date: 2026-10-31
---

# One design language for every surface: airfield signage, set in Atkinson Hyperlegible Next

The passenger web app, the Staff app, the QR posters and the pitch share one visual language, documented in [docs/design](../design/README.md), with live reference screens in `docs/design/reference/`. It borrows the ICAO airfield sign families:
- white on red means emergency
- black on yellow means press this
- yellow on black means this place

It also uses Atkinson Hyperlegible Next, a free (SIL OFL) typeface designed for low-vision readers, self-hosted in both apps (about 52 KB on the web).

We chose it by building three directions with identical screens: airfield signage, a civic NHS-style look, and a dark departures-board look. Five persona reviewers judged them from phone screenshots: an elderly passenger, a rushed traveller and tourist, airport staff, an accessibility auditor, and a design lead acting as judge. All five ranked airfield signage first, for legibility in glare and for older eyes, for severity that survives colour-blindness, and for a concept that reads as official and stands out in a pitch. The system font in the other directions made "Ia" read as "la".

**Consequences:**
- Adding the font files to both apps is a new asset, which this ADR asks the team to accept.
- The Staff app needs `react-native-svg` to share the icon shapes; that is a new dependency, decided in B01.
- A design change goes through the reference screens and these docs first.

## Considered options

- **Civic calm (NHS/GOV.UK-like blue, system font):** trustworthy but generic and nearly a copy of the NHS palette. Pale secondary buttons looked disabled.
- **Departures board (dark navy and amber, Barlow):** striking, but glare-prone, hard for older eyes, close to a "dark app with one accent" cliché, and its Critical severity was the least visible. It might return as an optional Staff night theme.
