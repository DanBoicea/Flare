# Reference screens

The visual source of truth for Flare: real HTML and CSS rendering every key screen with real copy. Simulated data only.

- **In a browser:** open `index.html?screen=<id>`. Use Chrome DevTools device mode at 390 × 844 to see it as a phone.
- **Screenshots:** `node docs/design/reference/render.mjs [outDir] [screen,screen]` uses headless Chrome or Edge at 2×. It writes to `shots/` by default, which git ignores.

| `screen` | What |
| --- | --- |
| `compose`, `compose-open` | Write a message; with the call-back phone field open |
| `sending` | Waiting state with the beacon |
| `answer` | Automatic answer to a Question |
| `bounced` | Message not understood |
| `confirm` | Report sent |
| `emergency` | Emergency instructions |
| `staff-list`, `staff-detail` | Staff app: Active list and one Issue |
| `poster` | QR poster, A4 portrait (the QR pattern is a fake placeholder) |
| `palette` | Colours, type specimen, buttons, plates, severity badges |

`render.mjs` also renders `compose` and `staff-list` at 320 px wide, the minimum we support.

`flare.css` holds the tokens (`:root`) that the apps copy, as described in [foundations](../foundations.md#tokens-in-code). The font loads from Google Fonts in this preview only; the apps self-host it.
