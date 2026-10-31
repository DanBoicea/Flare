# Flare passenger web app

The page a Passenger opens by scanning a QR code (`/q/{qrCodeId}`). React + Vite + TypeScript. Design: [docs/architecture/frontends.md](../../docs/architecture/frontends.md).

## Run

```sh
npm install
cp .env.example .env        # Windows PowerShell: Copy-Item .env.example .env
npm run dev                 # http://localhost:5173/q/test123
```

- **Mock mode:** set `VITE_MOCK=true` in `.env` to run without a back end. Mock answers depend on the text:
  - "fire" or "foc": Emergency screen
  - "joke", "glumă" or "test": Bounced (from the 3rd one, "Send anyway" appears)
  - a "?": automatic answer (also containing "wifi": no answer found)
  - anything else: confirmation
- **Real back end:** `VITE_MOCK=false` and `VITE_API_BASE_URL` pointing at Spring Boot (default `http://localhost:8080`).
- **From a phone on the same Wi-Fi:** `npm run dev -- --host`, then open `http://<laptop-ip>:5173/q/<id>`.

## Other commands

- `npm run build`: type-check and build to `dist/`. Hosting must serve `index.html` for every path (SPA fallback), over HTTPS.
- `npm run lint`
- `npm run gen:api`: regenerate `src/api/schema.d.ts` from `contract/openapi.yaml` after every contract change, and commit the result. It runs `openapi-typescript` through `npx` instead of installing it, because the package's TypeScript peer range doesn't include our TypeScript 6 yet.

## Layout

| Path | Contents |
| --- | --- |
| `src/App.tsx` | Screen state machine (Compose, Sending, Answer, Bounced, Confirmation, Emergency) |
| `src/screens/`, `src/components/` | Screens; always-visible emergency strip and language button |
| `src/api/` | Generated contract types, typed `fetch` client, mock mode |
| `src/i18n/` | `ro.json` (default language, source of the key list), `en.json`, provider and `t()` |
| `src/lib/` | QR code ID from the URL, device ID, elapsed time for the bot check |
| `src/styles/` | Design tokens and global styles |
| `src/fixtures/` | Simulated mock responses |
