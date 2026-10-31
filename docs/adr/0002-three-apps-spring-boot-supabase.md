---
status: accepted
date: 2026-10-30
---

# Three apps: React passenger web app, React Native Staff app, Spring Boot back end with Supabase

Passengers use a React web app opened from a QR code, so they install nothing. Staff use a React Native mobile app. One Spring Boot back end holds all business logic, and Supabase provides the data platform. The team prefers this stack, and staff need a real mobile app, so we chose it over a single web app. This has three consequences:
- The apps are written in TypeScript or JavaScript and the back end in Java, so the API between them must be written down once as a shared contract.
- iOS builds of the Staff app need a Mac. The team has two.
- Exactly which jobs Supabase does and which Spring Boot does is still open.

## Considered options

- **One TypeScript web app for Passengers and staff, with a built-in back end.** It was fastest to build: one language, one deploy. It was rejected because staff should have a mobile app and the team prefers React Native and Spring Boot.
