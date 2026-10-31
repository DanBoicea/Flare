# Flare

Passenger issue reporting and help for airports. A passenger scans a QR code and types a problem or a question into one simple field. Questions get an instant answer from an airport knowledge base. Problems go to the right staff team's mobile app, grouped with other reports of the same problem and sorted by severity.

## Status

Hackathon prototype. The architecture and the workplan are done, and the build is starting. The documents are:
- the product brief: [CLAUDE.md](CLAUDE.md)
- the vocabulary: [GLOSSARY.md](GLOSSARY.md)
- the system design: [docs/architecture](docs/architecture/README.md)
- decisions: [docs/adr](docs/adr/)
- the task board: [docs/tasks](docs/tasks/README.md)

## How it works

1. A passenger scans a QR code. Each QR code belongs to one specific place in the airport, so staff know exactly where the problem is.
2. They type a problem or a question in one text field, in Romanian or English. Contact details are optional.
3. An AI assistant on the server sorts the message:
   - a **question** gets an answer from the airport knowledge base, and the passenger can still send it to staff
   - **garbage, a joke or spam** is sent back with a request to rewrite it
   - an **emergency** shows emergency instructions (112) immediately
   - a **real problem** becomes a report
4. Reports of the same problem are grouped into one issue ("3 people reported missing toilet paper").
5. Staff see their own team's issues live in a mobile app, with an AI summary and the original reports, and acknowledge, take or resolve them in one tap.
6. The passenger gets a confirmation that explains what happens next. Flare works like a hotline: staff close issues, and the passenger does not need to follow up.

## Stack

- **Passenger web app:** React
- **Staff app:** React Native
- **Back end:** Spring Boot
- **Data:** Supabase
- **AI:** Claude, called only by the back end

## Scope

- **Target setting:** Craiova International Airport, for the hackathon demo only.
- **Data:** all demo data is simulated and fictional. It is labelled as simulated wherever it appears.
- **Languages:** Romanian and English at minimum.
- **Time:** about one day plus a few hours. The pitch is on Sunday.

### Out of scope

- Real deployment, or integration with real airport or airline systems, or flight data
- A native passenger app, payments, passenger accounts
- Real emergency dispatch

This is not an emergency channel. The passenger web app shows the emergency number on every screen, and messages that suggest a medical emergency, security threat or missing child are flagged to staff as top priority.

## Security and privacy

- Software cannot stop a fake sticker over a real QR code. Flare relies on signage next to each code, printed short URLs, unique code IDs and flagging of unknown codes.
- The passenger web app never asks for payments, passwords or sensitive personal data.
- Spam is limited by throttling, a bot check and a quarantine queue that keeps suspicious reports out of staff lists.
- Garbage, joke and spam messages are caught by a quality check. The passenger is told why and asked to rewrite the message.
- Passenger text is treated as untrusted data, and both apps display it as plain text only.
- Staff have individual accounts and see only their own team's issues. There is an audit log.
- Data is collected minimally and is anonymous by default. Contact details are deleted 30 days after the issue is resolved. No tracking.

## Limits

- The AI makes mistakes. Staff can always override category and severity, and the original reports are always visible.
- Accuracy will be measured on a small labelled test set in Romanian and English, and reported honestly.
- Competing products already exist. This project does not claim novelty.

## Setup

Not written yet. The skeleton tasks (A01, B01, W01) fill this section in. The planned commands and environment variables are in [docs/architecture/operations.md](docs/architecture/operations.md).

## Team

Team of three, hackathon project.

## License

Not yet decided.
