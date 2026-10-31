---
status: accepted
date: 2026-10-31
---

# One back-end instance: in-memory rate limits and a single-threaded grouping worker

The back end runs as exactly one instance for the hackathon. That lets us keep three things in memory, as plain Java with no Redis and no rate-limit library:
- rate-limit counters
- Bounce counts
- QR code Strict mode

All three reset on restart, which is acceptable for a demo. Grouping Reports into Issues runs on a single background thread that takes Reports from a queue one at a time. Two Reports of the same problem arriving together can therefore never create two Issues, with no locking needed. On startup, the worker re-queues accepted Reports that have no Issue yet, so nothing is lost by a restart. Scaling to several instances would mean moving the counters to the database or Redis, and grouping to a database-backed queue.
