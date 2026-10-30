# Triage, quality gate and deduplication

## Categories (tentative, will probably change)

Cleaning or spill, Toilet, Lift or escalator, Broken or maintenance, Sign or information, Assistance needed, Lost item or person, Safety or security concern, Other (free text).

## Severity scale (tentative, will probably change)

- **Critical:** emergency or safety hazard (medical, security threat, missing child, fire or smoke). Set by deterministic rules, never by the LLM alone.
- **High:** blocks passengers or poses a hazard (broken lift with no alternative, large spill, blocked exit).
- **Medium:** degraded service (dirty toilet, broken seat, missing assistance).
- **Low:** minor or informational (unclear sign, small litter, suggestion).

## Request quality gate (decided, details proposed)

**Decided by the team:**

- An LLM detects garbage, joke and spam reports.
- The passenger who sends one gets clear negative feedback. The report is not sent through.
- The passenger is told to change the request so that it is relevant to the airport and flights.
- When the rewritten report passes the LLM check, it goes to the back end and the employee app.

**Proposed guardrails (to confirm with the team):**

- **Emergency first.** Deterministic emergency rules run before the gate and bypass it. A report that looks like an emergency is never bounced. The passenger sees emergency instructions immediately and staff get a top-priority flag.
- **Fail open on LLM failure.** If the gate's LLM is unavailable, the report goes through marked "unchecked" (or into the quarantine queue). An outage must not block genuine reports, per the principle that a report is never lost.
- **Feedback is plain, polite and specific.** It says what looks wrong and what to change, keeps the passenger's text for editing, and does not reveal thresholds or accuse the passenger.
- **No thresholds revealed, retries limited.** Attempts are rate limited per device. This stops attackers from probing the gate and stops cost abuse.
- **Fixed output structure.** The gate returns a verdict, a reason code and a short message from a fixed set. Passenger text is untrusted data and never changes the instructions.
- **Beware false rejections.** Short or non-native-language reports such as "lift broken" or messages in a language other than Romanian or English must not be rejected for style. We measure the false-rejection rate and report it honestly.
- **Privacy for rejected text.** By default, do not store rejected text beyond what is needed to count attempts. Whether to keep samples for tuning is an open question.

**Open question:** if a genuine report is wrongly rejected repeatedly, should the passenger get a "send anyway for human review" option that routes to the quarantine queue? It protects real problems from being lost, but weakens the gate. Recommended: yes, after a few failed attempts.

## Triage rules

- Outputs: category, severity, short summary for staff, responsible team, confidence, spam or abuse likelihood, duplicate candidates.
- Deterministic rules override the LLM where the stakes are high (for example emergency keywords).
- Cheap checks run first, so obvious spam does not cost an LLM call.
- A basic rule-based fallback triage always exists.
- Staff can always override category and severity.
- In phase 1 the suggested team is shown but does not route the report.

## Measuring quality

Build a small labelled test set of realistic reports in Romanian and English. Measure:

- category accuracy
- severity accuracy
- spam, joke and garbage detection (catch rate)
- **false rejection rate** of genuine reports

Report the results honestly in the demo.

## Deduplication and prioritisation

- Same zone, same category, short time window and similar content become one issue with a count.
- Priority combines severity, number of independent reports, age and how critical the location is. The dashboard explains why (for example "12 reports in 10 minutes").
- Count independent reporters only. Allow staff to split or merge issues (stretch if time is short).
- Avoid over-merging distinct problems.
