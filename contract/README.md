# contract

The API contract between the back end and both apps, and its single source of truth ([ADR-0012](../docs/adr/0012-contract-first-openapi.md)).

- `openapi.yaml`: the OpenAPI spec (task S02).

Change the contract here first, then regenerate each app's `schema.d.ts` with `npm run gen:api`.
