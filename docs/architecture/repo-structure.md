# Repository structure

One repo, independent apps ([ADR-0009](../adr/0009-monorepo-independent-apps.md)). The three app skeletons already exist in `apps/`. Folders marked *(new)* are created by task S01 or the skeleton tasks.

```
Flare/
├── CLAUDE.md                  project instructions (read first)
├── README.md
├── GLOSSARY.md                domain vocabulary
├── .gitignore                 root ignores; extended in S01 before any install
├── .claude/settings.json      shared Claude Code plugins
│
├── contract/                  (new) the API contract, single source of truth
│   └── openapi.yaml
│
├── apps/
│   ├── flare-backend/         Spring Boot 4.1.2, Java 21, Maven wrapper (skeleton exists)
│   │   ├── pom.xml            group com.fsociety, artifact flare-backend
│   │   ├── mvnw, mvnw.cmd, .mvn/, .gitignore, .gitattributes
│   │   ├── .env.example       names of every back-end env var, fake values
│   │   ├── Dockerfile         (A11) container for hosting
│   │   └── src/
│   │       ├── main/java/com/fsociety/flarebackend/…   FlareBackendApplication + packages, see backend.md
│   │       ├── main/resources/
│   │       │   ├── application.yml            replaces the generated application.properties (A01)
│   │       │   ├── application-local.yml      local profile
│   │       │   ├── application-demo.yml       demo profile (enables the demo seed)
│   │       │   ├── prompts/                   classify / match / summarize .system.md (C02-C04)
│   │       │   ├── safety/emergency-terms.yml Emergency word list RO/EN (A07)
│   │       │   └── db/migration/ (A02), db/seed/R__demo_seed.sql (D01; local/demo only)
│   │       └── test/java/com/fsociety/flarebackend/…   unit tests; eval tests tagged "eval"
│   │
│   ├── flare-webapp/          passenger web app: React + Vite + TypeScript (skeleton exists)
│   │   ├── package.json, vite.config.ts, tsconfig*.json, eslint.config.js, index.html, .gitignore
│   │   ├── .env.example
│   │   └── src/
│   │       ├── main.tsx, App.tsx          screen state machine
│   │       ├── api/                       schema.d.ts (generated), client.ts, mock.ts
│   │       ├── screens/                   Compose, Sending, Answer, Bounced, Confirmation, Emergency
│   │       ├── components/                EmergencyStrip, LanguageButton, MessageField, …
│   │       ├── i18n/                      ro.json, en.json, i18n.ts
│   │       ├── lib/                       deviceId.ts, qrCode.ts, elapsed.ts
│   │       ├── fixtures/                  mock responses (D05)
│   │       └── styles/                    tokens.css, global.css (replace the template's App.css / index.css)
│   │
│   └── flare-mobile/          Staff app: Expo 58 + Expo Router + TypeScript (skeleton exists)
│       ├── package.json, app.json, tsconfig.json, .gitignore, AGENTS.md
│       ├── .env.example
│       └── src/
│           ├── app/                       Expo Router routes (replace the template's index/explore)
│           │   ├── _layout.tsx            session gate
│           │   ├── sign-in.tsx
│           │   ├── (tabs)/_layout.tsx     Active · Resolved · Needs review (duty manager)
│           │   ├── (tabs)/index.tsx       Active
│           │   ├── (tabs)/resolved.tsx
│           │   ├── (tabs)/review.tsx
│           │   └── issue/[id].tsx         Issue detail
│           ├── api/                       schema.d.ts (generated), client.ts, mock.ts
│           ├── auth/                      supabase.ts, session.tsx
│           ├── hooks/                     useIssueFeed.ts, useMe.ts, useIssue.ts (next to the template's theme hooks)
│           ├── components/                IssueCard, SeverityBadge, ActionBar, ReportList, TeamPicker, …
│           ├── constants/theme.ts         design tokens (extends the template's theme file)
│           ├── i18n/                      ro.json, en.json, i18n.ts
│           └── fixtures/                  mock data (D05)
│
├── data/                      committed, simulated, labelled
│   ├── airport/               Areas, Locations, QR codes, Teams, demo staff (D01)
│   ├── knowledge-base/        simulated airport facts, Markdown, RO + EN (D03)
│   ├── test-set/              labelled RO/EN messages + grouping pairs (D02)
│   └── demo-scenarios/        scripted demo message sequences (D06)
│
├── scripts/                   Node 22 built-ins only
│   ├── check-i18n.mjs         RO/EN key parity for both apps (D04)
│   ├── demo/run-scenario.mjs  plays a scenario against an API base URL (D06)
│   └── posters/               printable poster generator (D07)
│
├── docs/
│   ├── architecture/          this folder
│   ├── adr/                   decision records
│   ├── tasks/                 task files and the board
│   ├── research/              (new) e.g. competitor check (D09)
│   ├── eval-results.md        (new) evaluation results (C05)
│   └── product.md, principles.md, …
│
└── recordings/                ignored by git (demo fallback video)
```

## Conventions

- **Generated files are committed** (`schema.d.ts` in both apps), so nobody needs the generator just to build. Regenerate with `npm run gen:api` whenever `contract/openapi.yaml` changes.
- **Every app has its own `.env.example`.** Real `.env` files are ignored. Root `.gitignore` additions for Node, Expo and Maven come in S01 ([CLAUDE.md](../../CLAUDE.md) "Never commit").
- **The back end reads `data/knowledge-base/`** through Maven: the `pom.xml` adds `../../data/knowledge-base` as an extra resource folder (target path `knowledge-base/`), so the Knowledge base ships inside the JAR.
- **Eval tests read `data/test-set/`** by relative path from `apps/flare-backend/` (`../../data/test-set`).
- **Java package root `com.fsociety.flarebackend`** (from the existing skeleton).
