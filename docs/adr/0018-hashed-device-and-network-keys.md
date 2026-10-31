---
status: accepted
date: 2026-10-31
---

# Rate limits use a random device ID and the network address, both stored only as keyed hashes

The passenger web app creates a random device ID on first use and keeps it in the browser's local storage. Spring Boot combines it with the request's network address, taken from the hosting proxy's forwarding header and trusted only from that proxy. Both are run through an HMAC with a server secret before they are used or stored, so the database never holds a raw IP address or device ID, and the hashes mean nothing outside Flare. The device ID is used only for abuse protection, which we treat as strictly necessary. It is not used for tracking or analytics. This is a documented assumption to check with a privacy expert before any real deployment. The limits on network addresses are generous, because many Passengers on airport Wi-Fi share one public address.
