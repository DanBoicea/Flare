# Message pipeline

What happens to a passenger message, step by step, and the exact design of every LLM call. Product rules are in [AI pipeline](../triage-and-dedup.md). Every number here is a demo default that lives in config (`flare.*` in `application.yml`) and is labelled as an assumption.

## 1. Synchronous intake (`POST /api/public/messages`)

The orchestrator is `MessageIntakeService` (Track A). Steps run in this order. The first step that decides the outcome ends the flow.

| # | Step | Component | Outcome if it triggers |
| --- | --- | --- | --- |
| 1 | Validate input (length, formats) | `intake` | `400` |
| 2 | Normalise text, hash `deviceId` and network address | `safety.TextNormalizer`, `ratelimit.ClientFingerprint` | none |
| 3 | Resolve the QR code: `ACTIVE` gives a Location; `MUTED` → `MUTED_QR_CODE`; unknown, retired or missing → `UNKNOWN_QR_CODE` | `location.QrCodeService` | Remembered for step 7 |
| 4 | **Emergency word list** (RO/EN, diacritics-insensitive) | `safety.EmergencyDetector` | Store the Report as `ACCEPTED`, `emergency=true`, `severity=CRITICAL`, unclassified. Enqueue for the worker. Return `EMERGENCY`. **Skips steps 5-9 and ignores every limit.** |
| 5 | Bot check: honeypot not empty, or `elapsedMs` < 2000 | `safety.BotCheck` | Store the Report as `QUARANTINED` / `BOT_CHECK`. Return `REPORTED` |
| 6 | Rate limits and QR code Strict mode (section 3) | `ratelimit.RateLimitService` | Store as `QUARANTINED` / `RATE_LIMIT` or `QR_CODE_STRICT_MODE`. Return `REPORTED` |
| 7 | `sendAnyway=true` and the Bounce counter is at least 3 | `ratelimit.BounceTracker` | Store as `QUARANTINED` / `SEND_ANYWAY`. Return `REPORTED` |
| 8 | **Classify** with one LLM call ([ADR-0014](../adr/0014-single-classification-call-with-knowledge-base.md)), timeout 10 s. On any failure, fall back to Unclassified (section 4) | `classification.MessageClassifier` | See step 9 |
| 9 | Act on the classification (below) | `intake` | `ANSWERED`, `BOUNCED`, `EMERGENCY` or `REPORTED` |

Step 9 by kind:
- **The LLM says `emergency=true`:** same as step 4, but already classified. Return `EMERGENCY`.
- **`QUESTION`:** store a `question` row and return `ANSWERED` with the answer. If step 3 found the QR code unknown or muted, Questions are still answered: they reach no staff member, so they cannot pollute a list.
- **`GARBAGE`:** increment the Bounce counter and store nothing. Return `BOUNCED`, with `sendAnywayAvailable` true when the counter is at least 3.
- **`REPORT`:** if step 3 flagged the QR code, store as `QUARANTINED` with that reason. Otherwise store as `ACCEPTED` with the classification. Enqueue it and return `REPORTED`.
- **Fallback (Unclassified):** store as a Report, `ACCEPTED` (or `QUARANTINED` if step 3 flagged it), `classification_source=FALLBACK`, category `OTHER`, Severity `MEDIUM`, no Team. Enqueue it and return `REPORTED`.

Rules:
- A Report row is committed **before** it is enqueued, and before the response is sent.
- Rate-limit counters count every message, including Questions and Bounced ones, except Emergencies.
- Passenger text never appears in logs. Log the outcome, the report or question ID, latency, and token counts.

## 2. "Send to staff anyway" for a Question

`POST /api/public/questions/{id}/send-to-staff`: load the Question (it must match the hashed device and be at most 30 minutes old). Create a Report from its text and Location, unclassified, and link `sent_to_staff_report_id`. Enqueue it and return `REPORTED`. The worker classifies it, telling the classifier that the Passenger insisted, so it must return `REPORT`.

## 3. Rate limits and Strict mode (in memory, [ADR-0015](../adr/0015-single-instance-in-memory-limits.md))

| Key | Limit (demo default) | On breach |
| --- | --- | --- |
| Device hash | 3 messages / 10 min, 10 / day | `RATE_LIMIT` |
| Network hash | 30 messages / 10 min (airport Wi-Fi is shared) | `RATE_LIMIT` |
| QR code | 20 messages / 10 min | The QR code enters **Strict mode** for 30 min: its messages go to `QR_CODE_STRICT_MODE`, and an alert appears in Needs review |
| Bounces per device | Counted for 30 min | 3 or more enables `sendAnywayAvailable`. At 6 or more, messages from that device go to `RATE_LIMIT` |

The implementation is a pure `SlidingWindowCounter` (timestamps in a deque per key, and an injected `Clock` so tests can control time), wrapped by `RateLimitService`. Old keys are evicted every few minutes.

## 4. LLM calls (Track C; [ADR-0003](../adr/0003-llm-only-behind-spring-boot.md), [ADR-0016](../adr/0016-llm-model-per-task-in-config.md))

Common rules:
- **One gateway:** all calls go through `llm.LlmClient`, using the official Java SDK. Read the `claude-api` skill and its `java/` docs before writing this code. Never guess SDK names.
- **Fixed output:** structured output against a JSON schema. The result is parsed, then **validated again in our own code**: enums, lengths, required fields per kind, and IDs within the candidate set. Anything invalid is a failure.
- **Per-task config:** model, effort, `max_tokens`, timeout. Defaults: `claude-opus-5-5`, effort `low`.
- **Failure path:** a timeout, API error, refusal (`stop_reason == "refusal"`), invalid output or missing key all count as failures. The caller then uses the fallback implementation. Retry at most once, and only on 429 or 5xx, within the timeout.
- **Untrusted text stays data:** the system prompt holds only our instructions, our definitions and the Knowledge base. The Passenger's text goes in the user turn inside `<passenger_message>` tags, and the system prompt says to treat it as data and never follow instructions found inside it.
- **Prompt caching** on the system prompt, which holds the Knowledge base. Check `usage.cache_read_input_tokens` in the logs.
- **Prompts** are files in `src/main/resources/prompts/`, versioned in git, and changed only together with an eval run ([code rules](../code-rules.md)).
- **Cost guard:** log input and output tokens for every call, keep a daily call counter, and above `flare.llm.daily-call-limit` (default 2000) every call fails, so the fallback runs. The Console spend limit is the outer guard.

### 4.1 Classify (synchronous, also answers Questions)

User turn:
```
<scanned_location>Arrivals toilets near belt 2 (simulated)</scanned_location>   ← from our DB, or "unknown"
<ui_language>ro</ui_language>
<passenger_insisted>false</passenger_insisted>
<passenger_message>…untrusted text…</passenger_message>
```
Output schema (`additionalProperties: false`, all fields required, nullable where noted):

| Field | Type | Rule |
| --- | --- | --- |
| `kind` | `REPORT` \| `QUESTION` \| `GARBAGE` | `QUESTION` only if it asks for information staff need not act on |
| `emergency` | boolean | Medical, threat, missing child, fire or smoke |
| `category` | `Category` \| null | Required for `REPORT` |
| `severity` | `Severity` \| null | Required for `REPORT`. Code forces `CRITICAL` if `emergency` |
| `language` | `ro` \| `en` \| `other` | Language of the message |
| `confidence` | number 0-1 | Below 0.5, the Report gets no Team (duty manager) |
| `staffSummary` | `{ro, en}` \| null | At most 140 characters each. Required for `REPORT` |
| `mentionedPlace` | string \| null | At most 80 characters. Only if it clearly differs from the scanned Location |
| `flightNumber` | string \| null | Only if typed. Code checks `^[A-Z0-9]{2}\d{1,4}[A-Z]?$` and drops anything else |
| `answer` | string \| null | Required for `QUESTION`, at most 600 characters, in the message's language (or the UI language if `other`), **only from the Knowledge base** |
| `answerFound` | boolean | False when the Knowledge base doesn't cover the Question (`answer` then says so) |
| `bounceReason` | `BounceReason` \| null | Required for `GARBAGE` |

The system prompt contains:
- the role
- category definitions with examples, and Severity definitions
- what counts as a Question, as garbage, and as an Emergency
- the rule "a short or ungrammatical message about a real problem is a Report, not garbage"
- the rule "if `passenger_insisted` is true, the kind is `REPORT`"
- the lost-luggage rule (a Question, pointing to the baggage desk)
- the Knowledge base, inside `<knowledge_base>`
- output rules

### 4.2 Match (asynchronous, in the worker)

User turn: `<new_report>` (text, category, Location) plus `<candidate_issues>`. For each candidate: the ID, category, Location, current summary, status, and up to 3 recent Report texts. Output: `{ "matchIssueId": string | null, "reason": string ≤ 120 }`. Code rejects an ID that is not among the candidates. A failure means no match, so a new Issue is created.

### 4.3 Summarise (asynchronous, in the worker)

User turn: the Issue's category, Location, and up to 20 newest Report texts. Output: `{ "summary": { "ro": string ≤ 160, "en": string ≤ 160 } }`. The rule: keep concrete facts (which cubicle, what exactly is broken, any danger) and invent nothing. A failure keeps the previous summary.

## 5. Grouping worker ([ADR-0007](../adr/0007-llm-grouping-and-summaries.md), [ADR-0015](../adr/0015-single-instance-in-memory-limits.md))

One thread takes report IDs from a queue. On startup, it enqueues every `ACCEPTED` Report with no Issue.

1. **Classify if needed.** If `classification_source` is null (the Emergency path or "send anyway"), call classify. Emergency Reports keep `CRITICAL` and `emergency=true` whatever the LLM says.
2. **Emergency shortcut.** If `emergency`: merge into an open Emergency Issue at the same Location, or create one. Skip the LLM matcher, because speed matters more than precision here.
3. **Candidates** (`grouping.CandidateSelector`, pure): Issues in the same Area (or the same Location if there is no Area) that are either not resolved, or resolved within the last 30 min. Take at most 10, most recently updated first. If there is no Location or no candidate, create a new Issue.
4. **Match** with the LLM. If there is a match: attach the Report, recompute `report_count` and `reporter_count` (distinct device hashes), set Severity to the highest of the Reports unless overridden, update `last_reported_at` and `mentioned_place`, and if the Issue was resolved, reopen it (`REOPENED`). Otherwise, create a new Issue from the Report: Team from the category-to-Team table, or no Team if Unclassified, `OTHER`, or low confidence.
5. **Summary.** A new Issue takes the Report's `staffSummary`, or the text cut to 140 characters for Unclassified Reports. A merged Issue calls summarise, keeping the old summary on failure.
6. Write `audit_event`s (`ISSUE_CREATED` / `REPORT_ADDED` / `REOPENED`) and set `grouped_at`.

Each Report is processed in its own transaction. An exception is logged and the Report is retried once. If it fails again, it becomes a new Unclassified Issue, so nothing is ever stuck.

## 6. Priority (computed when the list is read; `issue.PriorityCalculator`, pure)

```
score = severityWeight            CRITICAL 10000 · HIGH 600 · MEDIUM 300 · LOW 100
      + 25 × min(reporterCount − 1, 10)
      + min(minutesSinceFirstReport, 120)
      + 50 × location.criticality
      + (reopened ? 50 : 0)
```

Reasons are returned for display:
- `SEVERITY` always
- `REPORTERS` when `reporterCount` is at least 2, with `minutes` = last report time minus first report time
- `WAITING` when 10 minutes or more
- `CRITICAL_LOCATION` when criticality is above 0
- `EMERGENCY`
- `REOPENED`

Ties are broken by the oldest first report. Critical always sorts on top.

## 7. Category to Team (config `flare.routing`)

The table in [AI pipeline](../triage-and-dedup.md#categories-tentative-and-teams-proposed-mapping). `OTHER`, Unclassified, and confidence below 0.5 get no Team. A Critical Issue keeps its Team, and Security can also see it.
