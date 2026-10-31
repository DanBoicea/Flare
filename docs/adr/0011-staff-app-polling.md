---
status: accepted
date: 2026-10-31
---

# The Staff app gets live updates by polling every 5 seconds

While the Staff app is in the foreground, it asks Spring Boot for its Issue list every 5 seconds, and stops while in the background. At demo scale a full list is a few kilobytes. It works on every phone, network and host, and needs no extra server code or library. "Live without refreshing" is met, with up to 5 seconds of delay. All polling sits behind one hook in the app (`useIssueFeed`), so it can later be replaced by WebSocket or server-sent events without touching the screens.

## Considered options

- **WebSocket or server-sent events from Spring Boot.** More moving parts: authenticating the connection, reconnecting, and support from the host. Server-sent events also need a library in React Native.
- **Supabase Realtime.** Rejected with [ADR-0010](0010-supabase-postgres-and-auth-only.md).
- **Push notifications.** Stretch, for Critical Issues only.
