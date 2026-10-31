---
status: accepted
date: 2026-10-31
---

# QR codes open `/q/{qrCodeId}`, where the ID is random and opaque

Every QR code encodes `https://<official domain>/q/<qrCodeId>`. The `qrCodeId` is 10 random characters from an alphabet without look-alike characters (for example `k7f3m9q2xd`). It is generated once per printed QR code and stored in the `qr_code` table with its Location. The URL never contains the Location name, so editing the URL cannot pick a Location, and an ID we never issued is detected as unknown and Quarantined. This settles the open part of [ADR-0006](0006-location-specific-qr-codes.md). The short URL printed beneath the QR code is the same address. The Location label printed beneath it (for example `ARR-WC-02`) is for staff and is not part of the URL.
