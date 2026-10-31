---
status: accepted
date: 2026-10-30
---

# Every QR code belongs to one Location, which only staff see

Each printed QR code is placed at one specific Location, for example one particular toilet. Every Report made through it carries that Location, so staff know exactly where to go, and the Passenger never needs to say or see where they are. Each poster has a small Location label beneath the QR code so staff put it in the right place, plus the short URL used for the anti-impersonation checks. When a message clearly concerns a different place than the scanned Location, the LLM flags it. One part is proposed and still to confirm: the URL carries an opaque QR code ID that the back end maps to a Location, not the Location name itself. That way editing the URL cannot fake a Location, and unknown IDs can be flagged.
