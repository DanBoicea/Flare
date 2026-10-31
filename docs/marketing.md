# Marketing and adoption

The mentors listed "Marketing" as a point to address. We read it as three things, and the team confirmed the emphasis is on **passenger adoption and convenience** and on **pitch positioning**. If the mentors meant something narrower, update this file.

## 1. Passenger adoption and convenience (main focus)

Passengers use Flare only if scanning is easier than walking to a desk, and if they trust the QR code.

- **One field for everything.** "Report an issue in the airport or ask for help..." Passengers don't have to work out whether they have a problem or a question, or which category it falls in ([ADR-0004](adr/0004-single-free-text-field.md)).
- **Instant answers.** Simple Questions get an answer straight away from the Knowledge base, with no queue and no desk. This is a reason to scan even when nothing is broken ([ADR-0005](adr/0005-knowledge-base-answers-questions.md)).
- **No need to explain where you are.** The QR code already knows the Location ([ADR-0006](adr/0006-location-specific-qr-codes.md)).
- **Placement:** put QR codes where problems are noticed and people wait, such as near lifts, toilets, seating areas and information points. Exact Locations are an assumption, and we claim nothing about the real Craiova layout.
- **Convenience:** no app, no account, works one-handed and on a slow connection, and the phone keyboard's dictation works too.
- **Trust:** the short URL printed beneath the QR code, AI labelled honestly, and a clear promise of what happens next.

### Poster contents (draft)

Every poster carries:

- the QR code
- a one-line invitation in Romanian and English
- the short URL
- the "never asks for payment, passwords or personal data" line
- the not-an-emergency line with 112
- the small Location label, for staff, so posters go up in the right place

Draft text, to review with a native speaker before any use:

- RO: "Ai o problemă sau o întrebare? Scanează și spune-ne. Nu cerem niciodată plăți, parole sau date personale. Nu este o linie de urgență: pentru urgențe sună la 112."
- EN: "A problem or a question? Scan and tell us. This page never asks for payment, passwords or personal data. This is not an emergency line: in an emergency call 112."

## 2. Airport buy-in

- **A cheap pilot:** printed posters and a web page for Passengers, plus a mobile app for staff. No hardware.
- **What the airport gets:**
  - each Team sees only its own Issues, with the exact Location
  - duplicates are merged into prioritised Issues
  - simple Questions never reach a desk
  - fewer problems are missed
- Costs, ownership and who maintains the posters are unknown. Mark them as assumptions and do not quote numbers.

## 3. Pitch positioning

- Designed for non-technical staff and stressed Passengers, with Romanian first.
- **Honest about QR code impersonation:** signage plus detection, not a false promise.
- **Spam and quality handled:** rate limits, quarantine, and a quality check that tells Passengers what to fix.
- **Questions answered instantly** instead of becoming staff work.
- **Grouping turns noise into priority:** "3 people reported missing toilet paper".
- **Transparent:** AI is labelled, and nothing pretends to be a person.
- **No novelty claims.** Competing products exist, for example AeroSimple, Sensfix, AirVote, Zello QR Assist and Fixalert. That list is unverified. Before any comparison in the pitch, check their actual features from primary sources (a research task for later).
- The entry-page description is in [decisions](decisions.md) and is out of date. Keep it consistent with this positioning.
