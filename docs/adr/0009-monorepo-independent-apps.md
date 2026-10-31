---
status: accepted
date: 2026-10-31
---

# One repository, three independent apps, one contract folder

Everything lives in this one GitHub repo:
- `apps/flare-backend/`: Spring Boot
- `apps/flare-webapp/`: React
- `apps/flare-mobile/`: React Native
- `contract/`: the API contract
- `data/`: simulated airport data, the Knowledge base and the test set
- `scripts/`: demo and checking scripts
- `docs/`: documentation

Each app has its own build and its own `package.json` or `pom.xml`. There are no npm workspaces and no monorepo tool, because Expo's bundler and workspaces need extra setup we have no time to debug. The only things shared between apps are the contract file and the `data/` folder. See [repo structure](../architecture/repo-structure.md).
