# AI pipeline: classification, Knowledge base, grouping and summaries

Everything the LLM does in Flare. Every LLM call runs in the Spring Boot back end ([ADR-0003](adr/0003-llm-only-behind-spring-boot.md)). Terms are in [GLOSSARY.md](../GLOSSARY.md).

## What the LLM decides for each message

- **Kind:** Report, Question, or garbage, joke or spam (Bounced). Emergencies are flagged in every case.
- **For Reports:**
  - category
  - Severity
  - Team
  - short summary for staff
  - language
  - confidence
  - whether the message describes a different place than the scanned Location
  - proposed: any flight number the Passenger wrote
- **Output:** a fixed structure, validated before use (see [code rules](code-rules.md)). The LLM cannot take actions.

## Categories (tentative) and Teams (proposed mapping)

| Category | Team |
| --- | --- |
| Cleaning or spill | Cleaning |
| Toilet | Cleaning |
| Lift or escalator | Maintenance |
| Broken or maintenance | Maintenance |
| Sign or information | Passenger assistance |
| Assistance needed | Passenger assistance |
| Lost item or person | Passenger assistance (a missing child is an Emergency) |
| Safety or security concern | Security |
| Other | Duty manager |

The duty manager sees every Issue regardless of Team. The Team list is still to confirm.

## Severity scale (tentative, will probably change)

- **Critical:** emergency or safety hazard (medical, security threat, missing child, fire or smoke).
- **High:** blocks Passengers or poses a hazard (broken lift with no alternative, large spill, blocked exit).
- **Medium:** degraded service (dirty toilet, broken seat, missing assistance).
- **Low:** minor or informational (unclear sign, small litter, suggestion).

## Emergencies

- An Emergency is always Critical. The Passenger sees emergency instructions (112) at once, and the Issue goes to Security and the duty manager.
- No medical or security advice is given.
- Whether a small fixed word list catches emergencies before the LLM is answered is still open. See "Role of deterministic rules" below.

## Quality check that Bounces garbage ([ADR-0001](adr/0001-bounce-garbage-messages.md))

**Decided by the team:**

- The LLM detects garbage, joke and spam messages.
- The Passenger who sends one gets clear negative feedback. The message is not sent through.
- The Passenger is asked to change the message so it is relevant to the airport or their flight.
- When the rewritten message passes the LLM check, it goes on to the back end and the Staff app.

**Proposed guardrails (to confirm):**

- **Emergency first.** A message that looks like an Emergency is never Bounced.
- **LLM outage.** If the LLM is unavailable, the message goes through as "Unclassified" to the duty manager, so an outage never blocks genuine Reports.
- **Plain, polite, specific feedback.** It says what to change, keeps the Passenger's text, and never accuses the Passenger or reveals thresholds.
- **Retries limited.** Attempts are rate limited per device, which stops probing and cost abuse.
- **Watch false Bounces.** Short or non-native messages such as "lift broken" must not be Bounced for style. We measure the false-Bounce rate.
- **Bounced text is not stored** beyond what is needed to count attempts.
- **Open:** after a few Bounces, offer "Send anyway for staff review", which goes to Quarantined. Recommended: yes, after 3 Bounces.

## Knowledge base answers ([ADR-0005](adr/0005-knowledge-base-answers-questions.md))

- The Knowledge base is a curated set of airport facts. It is simulated for the demo and labelled as such, and never invented as if real.
- The LLM answers only from the Knowledge base. If the Knowledge base does not cover the Question, it says so and offers to send the message to staff.
- Answers are labelled as automatic. They never pretend to come from a person, and never give medical or security advice.
- "Send to staff anyway" is always available and turns the message into a Report.
- Where the Knowledge base lives and how big it may get are still open.

## Location handling ([ADR-0006](adr/0006-location-specific-qr-codes.md))

- The scanned Location (from the QR code) is the primary signal.
- Proposed: when the message clearly describes another place ("the lift near gate 3"), the LLM flags it, and staff see both "Scanned at" and "Mentioned".
- Some out-of-place messages are really Questions. For example, "my suitcase didn't arrive" sent from a toilet gets a Knowledge base answer.

## Role of deterministic rules (open)

The team asked whether a rule-based system on top of the LLM is redundant. Proposed answer: there is no second classifier. Only three small things stay outside the LLM, because they must work when the LLM is slow, wrong or down:

1. A short Romanian and English emergency word list that shows 112 instantly.
2. A default for when the LLM fails: the message is stored as Unclassified and sent to the duty manager.
3. Cheap checks before any LLM call: empty or repeated text, and rate limits.

## Grouping Reports into Issues ([ADR-0007](adr/0007-llm-grouping-and-summaries.md))

- The LLM decides whether a new Report describes an existing Issue. It only compares against candidates close in time and place. Proposed: open Issues from the last 30 minutes at the same or nearby Locations, plus Issues resolved within the last 30 minutes, which reopen with a "Reopened" flag. Details to set.
- Example: three "no toilet paper" Reports at one toilet become one Issue with "3 people reported". "The toilet is dirty" at the same toilet is a separate Issue.
- Count independent Passengers only. One device repeating itself does not raise the count.
- Avoid over-merging distinct problems. Staff split and merge is stretch.

## Priority

Priority combines Severity, the number of independent Reports, age, and how critical the Location is. The Staff app explains why an Issue ranks where it does (for example "5 reports in 8 minutes").

## Staff summaries ([ADR-0007](adr/0007-llm-grouping-and-summaries.md))

- The LLM writes the Issue summary and refreshes it as new Reports join.
- Summaries are labelled AI, and the original Reports are one tap away.
- A summary must keep the facts that matter for acting, such as which cubicle or what exactly is broken.

## Measuring quality

Build a small labelled test set of realistic Romanian and English messages. Measure:

- kind accuracy (Report, Question or garbage)
- category accuracy
- Severity accuracy
- Team routing accuracy
- garbage catch rate
- false-Bounce rate of genuine Reports
- grouping accuracy on pairs that should and should not be grouped

Report the results honestly in the demo.
