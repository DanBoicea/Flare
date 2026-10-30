# Flare

Passenger issue reporting for airports. A passenger scans a QR code, describes a problem in a short form, and staff see it on a live dashboard that sorts by severity, groups duplicates, and routes each issue to the right team.

## Status

Hackathon prototype, in the planning stage. The technology stack is not chosen yet, and there is no runnable code. The product brief and open questions are in [CLAUDE.md](CLAUDE.md).

## What it does

1. A passenger scans a QR code at a location. The form opens with the location already filled in.
2. They choose a category or write free text, optionally add a photo, and optionally leave contact details. Nothing is required except the problem itself.
3. The report is checked for spam and abuse, then triaged (category, severity, short summary, language, responsible team). An LLM assists with triage, and a basic fallback always runs without it.
4. Reports about the same problem in the same place within a short time are grouped into one issue with a count.
5. Staff see a live dashboard filtered to their role and can acknowledge, assign, or resolve an issue in one tap.
6. The passenger sees honest status updates: received, seen, on its way, resolved.

## Scope

- **Target setting:** Craiova International Airport, for the hackathon demo only.
- **Data:** all demo data is simulated and fictional. It is labelled as simulated wherever it appears.
- **Languages:** Romanian and English at minimum.
- **Users:** passengers (no account, no app install), staff by role (cleaning, maintenance, security, passenger assistance, duty manager), and admins who manage QR codes, locations, teams, and rules.

### Out of scope

- Real deployment, or integration with real airport systems or flight data
- Native mobile apps, payments, passenger accounts
- Real emergency dispatch

This is not an emergency channel. The passenger form shows the emergency number prominently, and reports that suggest a medical emergency, security threat, or missing child are flagged to staff as top priority.

## Security and privacy

- Fake QR codes (stickers placed over real ones) are treated as a real threat. Mitigations include an official domain only, the short URL printed under every code, a unique ID per code with an admin inventory, and anomaly alerts for unknown IDs.
- The form never asks for payments, passwords, or sensitive personal data.
- Spam is limited by throttling, a bot challenge, daily caps, and a quarantine queue that keeps suspicious reports off the main dashboard.
- Passenger text is treated as untrusted data, and the staff interface escapes all displayed content.
- Uploaded photos are size-limited and have their metadata stripped.
- Staff access is by role, with an audit log.
- Data is collected minimally, anonymous by default, and kept for a limited time. No tracking.

The location in a report comes from the QR code, not the device. Device location can be spoofed and is unreliable indoors, so it is only a weak signal.

## Limits

- LLM triage makes mistakes. Staff can always override the category and severity.
- Accuracy will be measured on a small labelled test set in Romanian and English, and reported honestly.
- Competing products already exist. This project does not claim novelty.

## Setup

Not yet defined. Setup steps will be added once the stack is decided.

## Team

Team of three, hackathon project.

## License

Not yet decided.
