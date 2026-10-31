# S01: Repo folders, `.gitignore` for all stacks, `.env.example` files

Status: DONE
Owner: Florin Cristi
Track: S (lead: A) · Phase 0 · Estimate: 30 min · Agent-friendly: yes (OpenCode can do it; a human reviews)
Depends on: none
Unblocks: A01, B01, W01

## Goal
Prepare the repo so the three skeletons can be created without anyone committing `node_modules`, build output or secrets.

## Read first
[repo structure](../architecture/repo-structure.md) · [operations](../architecture/operations.md) (env vars) · `CLAUDE.md` "Never commit".

## Do
1. **Extend the root `.gitignore`.** Each app skeleton already has its own `.gitignore` from its template; the root rules are a safety net. Add these sections:
   - **Node:** `node_modules/`, `dist/`, `*.tsbuildinfo`, `npm-debug.log*`
   - **Vite:** `.vite/`
   - **Expo:** `.expo/`, `web-build/`, `expo-env.d.ts`, `.metro-health-check*`, `/apps/flare-mobile/ios/`, `/apps/flare-mobile/android/` (managed workflow, no native folders), `*.jks`, `*.p8`, `*.p12`, `*.key`, `*.mobileprovision`
   - **Maven/Java:** `apps/flare-backend/target/`, `*.class`, `hs_err_pid*`, `.mvn/wrapper/maven-wrapper.jar` only if the wrapper version uses a downloaded jar (checked: the wrapper is `distributionType=only-script`, so no jar and no rule; the backend `.gitignore` covers it anyway)
   - **IDEs:** `*.iml`, `.classpath`, `.project`, `.settings/`

   Keep the existing rules. **Do not** add broad patterns like `*.json`, `*.csv` or `data/`.
2. **Create the folders** with a short `README.md` each, saying what goes there:
   - `contract/`
   - `data/airport/`, `data/knowledge-base/`, `data/test-set/`, `data/demo-scenarios/`
   - `scripts/`
   - `docs/research/`
   
   Every `data/` README says "All content is simulated".
3. **Create `apps/flare-backend/.env.example`, `apps/flare-webapp/.env.example` and `apps/flare-mobile/.env.example`** with the exact contents from [operations](../architecture/operations.md). If the skeleton tasks run first, they create these instead.
4. **Check:** create a fake `apps/x/node_modules/a.js`, `apps/flare-backend/target/a.class` and `apps/flare-backend/.env`, and confirm `git status` ignores them. Delete them afterwards.

## Don't
Touch the app skeletons (they already exist; A01, B01 and W01 adapt them) or add dependencies.

## Files
`.gitignore`, `contract/README.md`, `data/*/README.md`, `scripts/README.md`, `docs/research/README.md`, the three `.env.example` files.

## Done when
- [x] Ignore rules are verified with `git check-ignore` for `node_modules`, `dist`, `target`, `.expo`, `.env`, `ios/`, `android/`.
- [x] `.env.example` files are tracked, and none holds a real value.
- [x] The folders exist, each with a README.
