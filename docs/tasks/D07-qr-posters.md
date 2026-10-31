# D07: Printable QR posters

Status: TODO
Owner:
Reviewer: any human (checks the printed result)
Track: D (OpenCode) · Phase 3 · Estimate: 45 min · Agent-friendly: yes, **but generating QR images needs the team's OK for the `qrcode` npm package** ([ADR-0013](../adr/0013-tooling-and-dependencies.md)). Without it, generate QR images with any trusted offline tool and save them as files.
Depends on: D01, the demo domain (S07)

## Goal
Posters that make the QR code trustworthy and correctly placed ([marketing: poster contents](../marketing.md#poster-contents-draft), [security: fake QR codes](../security-and-abuse.md#1-fake-qr-codes-quishing)).

## Do
1. **`scripts/posters/`:** a generator that reads `data/airport/airport.json` plus a `--base-url`, and writes `scripts/posters/out/<label>.html`, an A5 or A4 printable page. Add `scripts/posters/out/` to `.gitignore`. Each poster contains:
   - the invitation in RO and EN (from marketing.md)
   - a large QR code encoding `<base-url>/q/<qrCodeId>`
   - the **short URL printed in plain text** beneath it
   - "This page never asks for payment, passwords or personal data" in RO and EN
   - "Not an emergency line. Emergency: 112" in RO and EN
   - the **Location label** small in a corner, for staff
   - a footer: "Hackathon demo, simulated"
2. Print styles: black on white, large type, no background images.
3. Print 3 posters for the demo (S07), and test scanning from about 1.5 m.

## Done when
- [ ] Posters are generated for all Locations; 3 are printed and scan correctly with the iOS and Android cameras.
