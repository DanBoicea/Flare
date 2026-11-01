# Brand assets

| File | What it is |
| --- | --- |
| `flare-logo.png` | The master logo, 1024 × 1024, navy "F" with a white plane and a cobalt trail on white. Every other file is made from it. |
| `flare-mark.png` | The logo trimmed and centred on a white square, 512 px, for posters and slides (300 dpi print at 64 px CSS). |

## Rules

- The mark is navy on white and the plane is white, so on navy (headers, posters) it always sits on its **own white tile** with rounded corners (12 px at 40 px). Never put the bare mark on navy: the F disappears.
- It is decorative next to the word "Flare": `alt=""` on the web, not accessible in the Staff app.
- This is Flare's own logo, not an airport mark. The airport's name stays plain text ([foundations: brand](../foundations.md#brand)).

## Where the derived files live

Made from `flare-logo.png` (trimmed to the mark, then scaled with Lanczos). If the logo changes, regenerate all of them.

| App | File | Size and form |
| --- | --- | --- |
| Web | `apps/flare-webapp/public/favicon.ico` | 16, 32, 48 px, white rounded square |
| Web | `apps/flare-webapp/public/icon-192.png` | 192 px, white rounded square |
| Web | `apps/flare-webapp/public/apple-touch-icon.png` | 180 px, opaque white (iOS rounds it) |
| Web | `apps/flare-webapp/src/assets/flare-mark.png` | 192 px, white square, the header tile |
| Staff | `apps/flare-mobile/assets/images/flare-mark.png` | 192 px, white square, the header tile |
| Staff | `apps/flare-mobile/assets/images/icon.png` | 1024 px, opaque white, mark at 72 % (iOS and fallback) |
| Staff | `apps/flare-mobile/assets/images/android-icon-foreground.png` | 1024 px, transparent, the whole mark inside the 66/108 safe circle; background colour white in `app.json` |
| Staff | `apps/flare-mobile/assets/images/android-icon-monochrome.png` | the foreground's alpha only (Android 13+ themed icons) |
| Staff | `apps/flare-mobile/assets/images/splash-icon.png` | 1024 px, transparent, on a white splash |
| Staff | `apps/flare-mobile/assets/images/favicon.png` | 48 px, white rounded square |
