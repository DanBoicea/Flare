# Flare Staff app

The phone app staff use to see and act on Issues. Expo 58 + Expo Router + TypeScript, run in Expo Go (no native builds). Design: [docs/architecture/frontends.md](../../docs/architecture/frontends.md) and [docs/design](../../docs/design/README.md).

## Run

Prerequisites: Node 22 and **Expo Go** on your phone (iOS or Android).

```sh
npm install
cp .env.example .env        # Windows PowerShell: Copy-Item .env.example .env
npx expo start              # scan the QR code in the terminal with Expo Go (iOS: the Camera app)
```

- **Mock mode:** set `EXPO_PUBLIC_MOCK=true` in `.env` to run without the back end or Supabase. The Active tab lists simulated Issues from `src/fixtures/`, and actions change them in memory.
- **Real back end:** `EXPO_PUBLIC_MOCK=false`, and `EXPO_PUBLIC_API_BASE_URL` set to your **laptop's LAN IP**, for example `http://192.168.1.23:8080`, not `localhost` (on the phone, `localhost` is the phone). The phone and the laptop must be on the same Wi-Fi. The Supabase values come from the private team chat.
- `EXPO_PUBLIC_*` values are bundled into the app, so only public values go in `.env` (the Supabase publishable key is public; the service-role key never goes here).
- After changing `.env`, restart with `npx expo start --clear`.

## Other commands

- `npx tsc --noEmit` (or `npm run typecheck`) and `npm run lint`: run both before opening a PR.
- `npm run gen:api`: regenerate `src/api/schema.d.ts` from `contract/openapi.yaml` after every contract change, and commit the result. Same `npx` approach as the web app.
- `npx expo install <package>`: always use this to add a package, so the version matches SDK 58. New dependencies need the team's OK ([ADR-0013](../../docs/adr/0013-tooling-and-dependencies.md)).

## Layout

| Path | Contents |
| --- | --- |
| `src/app/` | Routes (every file is a screen): `sign-in`, `(tabs)/index` (Active), `(tabs)/resolved`, `(tabs)/review`, `issue/[id]` |
| `src/api/` | `schema.d.ts` (generated), `types.ts`, `client.ts` (typed fetch, bearer token, Problem Details errors), `mock.ts` |
| `src/i18n/` | `ro.json`, `en.json` (same keys; Romanian is the source), `t()`, language context saved on the phone |
| `src/constants/theme.ts` | Design tokens from [foundations](../../docs/design/foundations.md#tokens-in-code). Light theme only |
| `src/components/` | `Icon` (react-native-svg), `SeverityBadge`, `Placeholder` |
| `src/fixtures/` | Simulated staff and Issues for mock mode (D05 extends them) |
