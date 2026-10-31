# data/knowledge-base

**SIMULATED, for the hackathon demo. Not real information about Craiova International Airport.**

The places named here come from the simulated airport layout in [`data/airport/airport.json`](../airport/airport.json) (task D01). Details that do not come from that file, such as the Wi-Fi name or where the baggage services desk is, are invented for the demo and marked "(simulat)" / "(simulated)". There are no opening hours, prices, phone numbers or company names.

## What this folder is

The Knowledge base the LLM may use to answer Questions ([ADR-0005](../../docs/adr/0005-knowledge-base-answers-questions.md), [ADR-0014](../../docs/adr/0014-single-classification-call-with-knowledge-base.md)). The back end loads every `NN-topic.md` file, in file-name order, into the classification prompt. This README is skipped by the loader.

## Format

- One topic per file, named `NN-topic.md`.
- Each file has a `# Title`, then a `## RO` and a `## EN` section of 2-6 plain sentences.
- Plain words, written to be quoted directly as an answer to a Passenger.

## Deliberately not covered

So the demo can show an honest "I don't know": flight status and delays, shops, restaurants and cafés' menus, currency exchange, pharmacy, lounges, prayer room, car rental, pets, visas and customs rules.
