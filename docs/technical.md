# Technical guide

**Status: placeholder.** The technology stack is undecided and no technical planning has started. The team will say when to begin. Do not choose, add or install any framework, language, database, service or library before then.

## What the product needs from a stack (requirements only)

These come from the other docs and are not choices:

- A mobile-first web form for passengers, in Romanian and English, that works on slow connections and small screens.
- A staff dashboard that updates live without refreshing.
- A back end that accepts reports, applies rate limits, keeps a quarantine queue, groups duplicates and stores issues.
- An LLM for the quality gate and for triage, with fixed-structure output, no ability to take actions, and a rule-based fallback when it is unavailable.
- A bot check for the form.
- A unique ID per QR code, with unknown and muted IDs flagged.
- Escaping of all passenger text on display.
- Optionally later: camera access for scanning tickets, and photo upload.
- Work that three people can do in parallel with clear interfaces between parts.

## Decisions still to make at planning time

- Stack, hosting, and how the demo runs (live vs recorded fallback).
- LLM provider, and how its use is paid for and kept secret (never commit secrets).
- Data storage, retention implementation, and live-update mechanism.
- How code IDs are issued and validated.
- How staff sign in for the demo.
- The split of work between three people, and the task files for it.

## Planning approach (agreed, not started)

- An architecture plan, then tasks or issues written as markdown files in the project, so three people can work simultaneously.
- Do not start until the team says so.
