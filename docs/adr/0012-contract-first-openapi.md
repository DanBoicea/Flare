---
status: accepted
date: 2026-10-31
---

# The API is contract-first: one OpenAPI file, generated TypeScript types, hand-written Java DTOs

`contract/openapi.yaml` (OpenAPI 3.1) defines every endpoint and data shape between the apps and Spring Boot, and is written before the code. Both apps generate their TypeScript types from it with `openapi-typescript`, a development-only tool that generates types and nothing else, and commit the result. Each app has a small hand-written `fetch` wrapper using those types. Spring Boot implements the same shapes as Java records written by hand: Java code generators are heavy, and there are only about 15 endpoints. Writing the contract first lets the three of us work in parallel from the first hour: the apps can run against mock data that has exactly the contract's shape before the back end exists. A contract change is its own small PR, announced to the team.
