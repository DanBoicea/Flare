---
status: accepted
date: 2026-10-30
---

# Garbage, joke and spam messages are bounced back to the Passenger

The team decided that messages the LLM judges to be garbage, jokes or spam are not sent to staff. The Passenger is told clearly why, and is asked to rewrite the message so it is relevant to the airport or their flight. Once the LLM accepts the rewritten message, it continues as a normal Report. This deliberately departs from the earlier principle that a report is never blocked: staff attention is the scarcest resource, and the hackathon mentors flagged request quality. Some guardrails are still proposed, not decided: the emergency bypass, behaviour during an LLM outage, retry limits and a "send anyway" option. They are described in [triage-and-dedup.md](../triage-and-dedup.md).
