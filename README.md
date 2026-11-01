# Flare

Passenger issue reporting and help for airports. A passenger scans a QR code and types a problem or a question into one simple field. Questions get an instant answer from an airport knowledge base. Problems go to the right staff team's mobile app, grouped with other reports of the same problem and sorted by severity.

## Status

Hackathon prototype. The architecture and the workplan are done, and the build is starting. The documents are:
- the product brief: [CLAUDE.md](CLAUDE.md)
- the vocabulary: [GLOSSARY.md](GLOSSARY.md)
- the system design: [docs/architecture](docs/architecture/README.md)
- decisions: [docs/adr](docs/adr/)
- the task board: [docs/tasks](docs/tasks/README.md)

## How it works

1. A passenger scans a QR code. Each QR code belongs to one specific place in the airport, so staff know exactly where the problem is.
2. They type a problem or a question in one text field, in Romanian or English. Contact details are optional.
3. An AI assistant on the server sorts the message:
   - a **question** gets an answer from the airport knowledge base, and the passenger can still send it to staff
   - **garbage, a joke or spam** is sent back with a request to rewrite it
   - an **emergency** shows emergency instructions (112) immediately
   - a **real problem** becomes a report
4. Reports of the same problem are grouped into one issue ("3 people reported missing toilet paper").
5. Staff see their own team's issues live in a mobile app, with an AI summary and the original reports, and acknowledge, take or resolve them in one tap.
6. The passenger gets a confirmation that explains what happens next. Flare works like a hotline: staff close issues, and the passenger does not need to follow up.

## Stack

- **Passenger web app:** React
- **Staff app:** React Native
- **Back end:** Spring Boot
- **Data:** Supabase
- **AI:** a self-hosted open-weight model served by llama.cpp, called only by the back end; passenger text never leaves our machines

## Scope

- **Target setting:** Craiova International Airport, for the hackathon demo only.
- **Data:** all demo data is simulated and fictional. It is labelled as simulated wherever it appears.
- **Languages:** Romanian and English at minimum.
- **Time:** about one day plus a few hours. The pitch is on Sunday.

### Out of scope

- Real deployment, or integration with real airport or airline systems, or flight data
- A native passenger app, payments, passenger accounts
- Real emergency dispatch

This is not an emergency channel. The passenger web app shows the emergency number on every screen, and messages that suggest a medical emergency, security threat or missing child are flagged to staff as top priority.

## Security and privacy

- Software cannot stop a fake sticker over a real QR code. Flare relies on signage next to each code, printed short URLs, unique code IDs and flagging of unknown codes.
- The passenger web app never asks for payments, passwords or sensitive personal data.
- Spam is limited by throttling, a bot check and a quarantine queue that keeps suspicious reports out of staff lists.
- Garbage, joke and spam messages are caught by a quality check. The passenger is told why and asked to rewrite the message.
- Passenger text is treated as untrusted data, and both apps display it as plain text only.
- Staff have individual accounts and see only their own team's issues. There is an audit log.
- Data is collected minimally and is anonymous by default. Contact details are deleted 30 days after the issue is resolved. No tracking.

## Limits

- The AI makes mistakes. Staff can always override category and severity, and the original reports are always visible.
- Accuracy will be measured on a small labelled test set in Romanian and English, and reported honestly.
- Competing products already exist. This project does not claim novelty.

## Run it locally

Five steps: install the tools, add the secrets, start the LLM, start the back end, open the apps. Every command runs from the **repo root** unless a step says otherwise. The commands work in macOS and Linux terminals and in Windows PowerShell, except where a line shows both.

### 1. Install the tools (once)

| Tool | Why | Install |
| --- | --- | --- |
| **Node 22** | the LLM start script and both apps | https://nodejs.org (LTS) |
| **Docker Desktop** | runs the back end | https://www.docker.com/products/docker-desktop |
| **llama.cpp** | runs the AI model on your machine | see below |
| **Expo Go** | runs the Staff app on your phone | App Store or Google Play |

Install llama.cpp:
- **macOS:** `brew install llama.cpp`
- **Windows:** `winget install llama.cpp`
- **Linux:** `brew install llama.cpp` (Homebrew for Linux), or a prebuilt binary from https://github.com/ggml-org/llama.cpp/releases

Check it worked: `llama-server --version`. The model needs about **10 GB of free memory** and **9 GB of disk**. An Apple Silicon Mac with 16 GB or more is what we tested on (24 GB).

### 2. Add the secrets (once)

```sh
cp apps/flare-backend/.env.example apps/flare-backend/.env
```

Open `apps/flare-backend/.env` and replace every `change-me` and `<project-ref>` with the real `flare-dev` values from the private team chat. Never commit this file (it is git-ignored). What each variable means: [docs/architecture/operations.md](docs/architecture/operations.md).

### 3. Start the LLM (leave this terminal open)

```sh
node scripts/llm/start-llama-server.mjs
```

It looks for the model at `models/gemma-4-12b-it-Q5_K_M.gguf`. **The first time,** the file isn't there yet, so the script warns you and asks:
1. **Let llama-server download it now** (about 8.4 GB from Hugging Face). Next time, start it with `--download` so it reuses that copy.
2. **Download it yourself:** the script prints the link and the exact command, then exits. Run it again when the file is in `models/`.

| Command | When |
| --- | --- |
| `node scripts/llm/start-llama-server.mjs` | The model is in `models/` (default) |
| `node scripts/llm/start-llama-server.mjs --model <path/to/file.gguf>` | The model file is somewhere else |
| `node scripts/llm/start-llama-server.mjs --download` | Let llama-server download it, or reuse its earlier download |

It is ready when http://localhost:8081/health shows `{"status":"ok"}`. Stop it with Ctrl+C.

### 4. Start the back end (a second terminal)

```sh
docker compose up --build
```

The first build takes a few minutes. It is ready when http://localhost:8080/actuator/health shows `"status":"UP"`, and the log shows `LLM enabled; llama-server at ... answered /health with 200`. Stop it with Ctrl+C, then `docker compose down`.

### 5. Open the apps

**Passenger web app** (a third terminal):
```sh
cd apps/flare-webapp
npm install
npm run dev
```
Open a simulated QR code, for example http://localhost:5173/q/skp4fq3r5e (arrivals toilets near belt 2). Every QR code ID is in [data/airport/airport.json](data/airport/airport.json).

**Staff app** (a fourth terminal; your phone and laptop on the same Wi-Fi):
```sh
cd apps/flare-mobile
npm install
cp .env.example .env
```
In `apps/flare-mobile/.env`, set `EXPO_PUBLIC_API_BASE_URL` to `http://<your laptop's IP>:8080` (macOS: `ipconfig getifaddr en0`; Windows: `ipconfig`, the IPv4 address), and the two Supabase values from the team chat. Then:
```sh
npx expo start
```
Scan the QR code in the terminal with your phone (iOS: the Camera app; Android: Expo Go), and sign in with a simulated demo account from the team chat.

### Other ways to run

- **Without any back end (mock mode):** `VITE_MOCK=true` in `apps/flare-webapp/.env`, `EXPO_PUBLIC_MOCK=true` in `apps/flare-mobile/.env`. Both apps then use simulated answers. Details in each app's README: [web app](apps/flare-webapp/README.md), [Staff app](apps/flare-mobile/README.md).
- **Back end without Docker:** Java 21 or newer. In IntelliJ, add `apps/flare-backend/.env` as the run configuration's environment file and run `FlareBackendApplication`. In a terminal, export the variables from `.env` first (`./mvnw` doesn't read the file), then `cd apps/flare-backend && ./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).
- **Web app on a phone or behind ngrok:** add `apps/flare-webapp/.env.local` with the line `VITE_API_BASE_URL=` (empty), then `npm run dev -- --host`. The dev server forwards `/api` to the back end, so one address serves both.

### If something doesn't work

| Symptom | Fix |
| --- | --- |
| Every message ends up "Unclassified" | The LLM isn't used: check step 3 is running, and that `FLARE_LLM_ENABLED=true` is in `apps/flare-backend/.env`. Restart the back end after changing it |
| `port is already allocated` on 8080 | Another back end is running (IntelliJ, or an old container). Stop it, or `docker compose down` |
| `Cannot connect to the Docker daemon` | Start Docker Desktop and wait until it says it's running |
| `Could not start llama-server` | llama.cpp isn't installed or isn't on PATH: see step 1 |
| Back end log: llama-server `is not reachable` (Linux only) | Docker on Linux can't reach `127.0.0.1` on the host. Start the LLM with `LLAMA_HOST=0.0.0.0 FLARE_LLM_API_KEY=<a long random key> node scripts/llm/start-llama-server.mjs`, and put the same key in `FLARE_LLM_API_KEY` in `apps/flare-backend/.env` |
| Messages are accepted but never reach staff | You hit a rate limit (3 messages per 10 minutes per device): see [operations](docs/architecture/operations.md) |
| The Staff app can't connect | `EXPO_PUBLIC_API_BASE_URL` must be your laptop's IP, not `localhost`, and the phone must be on the same Wi-Fi. Restart with `npx expo start --clear` after editing `.env` |

### Tests

- **Back end:** `cd apps/flare-backend && ./mvnw test` (no database or LLM needed). The LLM evaluation is `./mvnw test -Dgroups=eval` and needs step 3 running ([results](docs/eval-results.md)).
- **Web app:** `cd apps/flare-webapp && npm test`.
- **Staff app:** `cd apps/flare-mobile && npx tsc --noEmit && npm run lint`.

## Team

Team of three, hackathon project.

## License

Not yet decided.
