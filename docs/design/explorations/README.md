# Design explorations (round 1, frozen)

The three directions compared on 2026-10-31. They are kept as a record, not as a source of truth: the chosen design is in [reference/](../reference/) and documented in [docs/design](../README.md). Results are in [reviews](../reviews.md).

| Theme | Direction | Result |
| --- | --- | --- |
| `theme-a.css` | Semnalistică (airfield signage), Atkinson Hyperlegible Next | **Chosen** (unanimous), then refined |
| `theme-b.css` | Serviciu public (civic calm), system font | Second |
| `theme-c.css` | Panou de plecări (departures board, dark), Barlow | Third |

## Viewing and rendering

- **In a browser:** open `index.html?theme=a&screen=compose`. The screens are `palette`, `compose`, `sending`, `answer`, `bounced`, `confirm`, `emergency`, `staff-list` and `staff-detail`. Add `&cvd=deuteranopia|protanopia|achromatopsia` to simulate colour blindness.
- **Screenshots:** `node docs/design/explorations/render.mjs [outDir] [a,b,c]` uses headless Chrome or Edge at 390 × 844, 2×. It writes to `shots/` by default, which git ignores.

The fonts load from Google Fonts in these previews only. The apps self-host them.
