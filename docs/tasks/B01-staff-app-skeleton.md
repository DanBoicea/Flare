# B01: Expo skeleton, theme, i18n, API client, mock mode

Status: TODO
Owner:
Track: B · Phase 1 · Estimate: 1 h · Agent-friendly: partly
Depends on: S01, S02
Unblocks: B02, D04, D05, S09

## Goal
A running Staff app with routing, strings, typed API access and mock data, before the back end exists.

## Read first
[frontends: Staff app](../architecture/frontends.md#staff-app-appsflare-mobile-expo-router) · [repo structure](../architecture/repo-structure.md) · [ADR-0013](../adr/0013-tooling-and-dependencies.md).

**Design:** [docs/design](../design/README.md): [foundations](../design/foundations.md), [components](../design/components.md), [screens](../design/screens.md), [mobile](../design/mobile.md), [reference screens](../design/reference/README.md).

## Do
1. **Start from the existing skeleton** in `apps/flare-mobile` (Expo 58, Expo Router, TypeScript, with routes in `src/app/`). Remove the template's demo screens (`index`, `explore`) and demo components, keeping `src/constants/theme.ts` and the theme hooks. Check the app's `.gitignore` covers `node_modules`, `.expo` and native folders.
2. **Routes:** add exactly the route files from frontends.md, as placeholder screens.
3. **`src/constants/theme.ts`:** colours, type scale, spacing, radii, and Severity colours with matching icons. Agree the values with W01 so both apps share them.
4. **`src/i18n`:** `ro.json`, `en.json`, a `t()` helper, a language context, the default language Romanian (English only when `expo-localization` reports an `en` device language), and the choice saved in AsyncStorage.
5. **`src/api`:**
   - `npm run gen:api` runs `openapi-typescript ../../contract/openapi.yaml -o src/api/schema.d.ts`; commit the output
   - `client.ts`: a typed `fetch` wrapper with base URL from `EXPO_PUBLIC_API_BASE_URL`, bearer token injection (B02 supplies the token), and Problem Details parsing into typed errors
   - `mock.ts`: the same functions served from `src/fixtures/` (D05) when `EXPO_PUBLIC_MOCK=true`, with in-memory mutation for actions
6. Add `.env.example` (from operations.md). Fill in the README setup section for the Staff app.

## Done when
- [ ] `npx expo start` runs, and the app opens in Expo Go on an iPhone and an Android phone.
- [ ] `npx tsc --noEmit` passes.
- [ ] In mock mode, the placeholder Active screen lists fixture Issues.
