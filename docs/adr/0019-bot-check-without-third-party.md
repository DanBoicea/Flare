---
status: accepted
date: 2026-10-31
---

# The demo's bot check is a hidden honeypot field plus a minimum fill time, with no third-party challenge

The passenger web app contains a form field that people never see and bots tend to fill, and it reports how long the page was open before sending. A message with the honeypot filled, or sent less than 2 seconds after the page opened, is silently Quarantined and never reaches the LLM. This adds no third-party script, no tracking and no extra tap for elderly Passengers. The trade-off is that a determined attacker can get past it. The rate limits and QR code Strict mode are the real defence, and the pitch says so. A privacy-friendly challenge service, such as an invisible CAPTCHA, is the documented next step for a real deployment, and needs the team's agreement because it is a new external service.
