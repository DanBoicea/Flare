# W01: Web skeleton, tokens, i18n, API client, mock mode, device and QR code IDs

Status: DONE
Owner: Dan
Track: C · Phases 0-1 · Estimate: 1 h · Agent-friendly: partly
Depends on: S01, S02
Unblocks: W02, D04, D05, S09

## Goal

A tiny, fast passenger web app shell that works in mock mode before the back end exists.

## Read first

[frontends: passenger web app](../architecture/frontends.md#passenger-web-app-appsflare-webapp) · [ADR-0013](../adr/0013-tooling-and-dependencies.md) · [ADR-0017](../adr/0017-qr-code-url-format.md).

**Design:** [docs/design](../design/README.md): [foundations](../design/foundations.md), [components](../design/components.md), [screens](../design/screens.md), [mobile](../design/mobile.md), [reference screens](../design/reference/README.md).

## Do

1. **Start from the existing skeleton** in `apps/flare-webapp` (Vite, React, TypeScript, ESLint). Remove the template's demo content (`App.css`, `index.css`, `assets/`, the counter). Check the app's `.gitignore` covers `node_modules` and `dist`.
2. **`src/styles/tokens.css`:** CSS custom properties for colours, type scale (body at least 18 px), spacing, radii and focus ring. Share the values with B01. Add `global.css` with a sensible reset and system font stack.
3. **`src/i18n`:**
    - `ro.json` and `en.json` from the copy deck in frontends.md (D04 completes them)
    - a `t()` helper and a language context
    - the default is Romanian; English only when `navigator.language` starts with `en`; the manual choice is saved in `localStorage`
4. **`src/lib`:**
    - `qrCode.ts`: parses `/q/{id}` and returns `null` otherwise
    - `deviceId.ts`: a UUID from `crypto.getRandomValues`, stored in `localStorage`
    - `elapsed.ts`: milliseconds since page load
5. **`src/api`:**
    - `gen:api` script (`openapi-typescript ../../contract/openapi.yaml -o src/api/schema.d.ts`), with the output committed
    - `client.ts` (`sendMessage`, `sendQuestionToStaff`, with Problem Details parsing and a 20 s timeout)
    - `mock.ts` (keyword-routed fixtures and a 1.5 s delay, as in frontends.md)
6. **`App.tsx`:** the screen state machine with placeholder screens, plus the `EmergencyStrip` and `LanguageButton` shells.
7. **Docs and env:** `.env.example`, and the README setup section for the web app, including the SPA fallback note for hosting.

## Done when

- [x] `npm run dev` serves `/q/test123`, and the state machine moves through every outcome in mock mode.
- [x] `npm run build` passes, and the gzipped JS is under 100 KB (73 KB).
- [x] `tsc` is clean (and `npm run lint`).
