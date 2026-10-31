# API

The source for `contract/openapi.yaml` ([ADR-0012](../adr/0012-contract-first-openapi.md)). If this file and the YAML disagree, the YAML wins and this file gets fixed. JSON only, field names in camelCase, enums as in [data model](data-model.md), times in ISO 8601 UTC.

## Conventions

- **Base path** `/api`. `/api/public/**` needs no authentication. `/api/staff/**` needs `Authorization: Bearer <Supabase access token>`.
- **Errors** use RFC 9457 Problem Details (`application/problem+json`): `{ "type", "title", "status", "detail", "code" }`. `code` is a stable machine value that the apps translate, for example `TEXT_EMPTY`, `TEXT_TOO_LONG`, `NOT_FOUND`, `FORBIDDEN`, `VERSION_CONFLICT`, `INVALID_TRANSITION`, `NOT_A_STAFF_MEMBER`.
- **Localised text** from the server is `LocalizedText = { "ro": string, "en": string }`.
- **No 429s.** Over-limit Passenger messages are Quarantined silently and answered like normal Reports ([security and abuse](../security-and-abuse.md)).

## Public endpoints (passenger web app)

### `POST /api/public/messages`

Request:
```json
{
  "text": "Nu mai e hârtie în toaleta de lângă banda 2",
  "uiLanguage": "ro",
  "qrCodeId": "k7f3m9q2xd",
  "deviceId": "3f0e8f8e-2c4b-4d6c-9a51-6f1f3f0c2b7a",
  "contact": null,
  "sendAnyway": false,
  "website": "",
  "elapsedMs": 8400
}
```
| Field | Rules |
| --- | --- |
| `text` | Required, 1-1000 characters after trimming |
| `uiLanguage` | `ro` or `en` |
| `qrCodeId` | Optional, at most 32 characters of `[a-z0-9]`. Any other value is treated as unknown |
| `deviceId` | Required, a UUID string |
| `contact` | Optional, at most 120 characters (phone or email, free text) |
| `sendAnyway` | Optional. Honoured only if the server's Bounce counter for this device allows it |
| `website` | Honeypot. Must be empty ([ADR-0019](../adr/0019-bot-check-without-third-party.md)) |
| `elapsedMs` | Milliseconds since the page opened ([ADR-0019](../adr/0019-bot-check-without-third-party.md)) |

Response `200`:
```json
{
  "outcome": "REPORTED | ANSWERED | BOUNCED | EMERGENCY",
  "questionId": "uuid or null",
  "answer": { "text": "string", "found": true } ,
  "bounce": { "reason": "NOT_AIRPORT_RELATED | JOKE_OR_NONSENSE | SPAM_OR_ADVERTISING | ABUSIVE | TOO_UNCLEAR", "sendAnywayAvailable": false }
}
```
- **`REPORTED`:** stored as a Report. It is also returned for silently Quarantined, bot-check or rate-limited messages, and for Unclassified fallbacks. `answer` and `bounce` are null.
- **`ANSWERED`:** a Question. `questionId` and `answer` are set. When `answer.found` is false, the app offers "Send to staff" as the main action.
- **`BOUNCED`:** nothing stored. `bounce` is set. `sendAnywayAvailable` becomes true from the third Bounce in 30 minutes for this device.
- **`EMERGENCY`:** stored as a Critical Report. The app shows the emergency screen, whose text is fixed in the app, not sent by the server.
- **`400`:** validation errors (`TEXT_EMPTY`, `TEXT_TOO_LONG`, `INVALID_REQUEST`).

### `POST /api/public/questions/{questionId}/send-to-staff`

Request `{ "deviceId": "uuid" }`. Response `200 { "outcome": "REPORTED" }`. This creates a Report from the Question's text and Location, unclassified, and the worker classifies it. It is idempotent: a second call returns `REPORTED` without creating another Report. Returns `404 NOT_FOUND` if the ID is unknown, older than 30 minutes, or from a different device.

## Staff endpoints (Staff app)

### `GET /api/staff/me`
`200 { "id": "uuid", "displayName": "Ana (simulat)", "role": "STAFF | DUTY_MANAGER", "teams": ["CLEANING", "PASSENGER_ASSISTANCE"] }`. Returns `403 NOT_A_STAFF_MEMBER` if the JWT is valid but there is no active `staff_member` row.

### `GET /api/staff/teams`
`200 [ { "id": "CLEANING", "name": { "ro": "Curățenie", "en": "Cleaning" } }, … ]`. Used by the hand-over picker.

### `GET /api/staff/issues?view=ACTIVE|RESOLVED`
`200 { "serverTime": "…", "issues": [IssueCard, …] }`
- **`ACTIVE`:** every non-resolved Issue the caller can see ([auth and security](auth-and-security.md)), sorted by priority, highest first.
- **`RESOLVED`:** Issues resolved in the last 24 hours, newest first, at most 50.
- The Staff app polls `ACTIVE` every 5 seconds ([ADR-0011](../adr/0011-staff-app-polling.md)).

`IssueCard`:
```json
{
  "id": "uuid",
  "version": 3,
  "status": "NEW",
  "severity": "HIGH",
  "category": "TOILET",
  "team": "CLEANING",
  "location": { "id": "uuid", "label": "ARR-WC-02", "name": { "ro": "…", "en": "…" } },
  "mentionedPlace": null,
  "summary": { "ro": "…", "en": "…" },
  "summarySource": "LLM",
  "reportCount": 3,
  "reporterCount": 3,
  "firstReportedAt": "…",
  "lastReportedAt": "…",
  "takenBy": { "id": "uuid", "displayName": "Ana (simulat)" },
  "emergency": false,
  "unclassified": false,
  "reopened": false,
  "priority": { "score": 742, "reasons": [ { "code": "REPORTERS", "count": 3, "minutes": 8 }, { "code": "WAITING", "minutes": 25 } ] }
}
```
- `location` and `takenBy` may be null.
- `team` is null when the Issue is unrouted.
- Priority reason codes, rendered by the app's string files: `SEVERITY` (`severity`), `REPORTERS` (`count`, `minutes`), `WAITING` (`minutes`), `CRITICAL_LOCATION`, `EMERGENCY`, `REOPENED`.

### `GET /api/staff/issues/{id}`
`200 IssueDetail` = `IssueCard` plus:
```json
{
  "reports": [ { "id": "uuid", "text": "…", "language": "ro", "createdAt": "…", "contact": null, "flightNumber": null, "mentionedPlace": null, "classificationSource": "LLM" } ],
  "history": [ { "action": "TAKEN", "staffDisplayName": "Ana (simulat)", "at": "…", "details": {} } ]
}
```
Reports are newest first, at most 50. Returns `404` if the Issue doesn't exist, and also if the caller can't see it, so that IDs are never confirmed.

### Issue actions
Every action takes `{ "version": <number from the card> }` plus the extra fields shown, and returns `200 IssueDetail`.

| Endpoint | Extra body | Effect |
| --- | --- | --- |
| `POST /api/staff/issues/{id}/acknowledge` | none | `NEW` → `ACKNOWLEDGED` |
| `POST /api/staff/issues/{id}/take` | none | becomes `IN_PROGRESS`, `takenBy` is the caller |
| `POST /api/staff/issues/{id}/resolve` | none | becomes `RESOLVED` |
| `POST /api/staff/issues/{id}/reopen` | none | `RESOLVED` → `NEW`, reopened |
| `PATCH /api/staff/issues/{id}` | `category?`, `severity?` | Override, sets the overridden flag |
| `POST /api/staff/issues/{id}/hand-over` | `team` (`TeamId`) | Moves the Issue to another Team, clears `takenBy` |

Errors:
- `409 VERSION_CONFLICT`: someone else changed the Issue. The app reloads and shows who did what.
- `409 INVALID_TRANSITION`: for example, reopening an Issue that is not resolved.
- `404`: the caller can't see the Issue.

### Needs review (duty manager only; `403 FORBIDDEN` for other roles)

`GET /api/staff/review` returns `200`:
```json
{
  "items": [ { "reportId": "uuid", "text": "…", "createdAt": "…", "reason": "UNKNOWN_QR_CODE", "scannedQrCodeId": "zz99…", "location": null } ],
  "alerts": [ { "qrCodeId": "k7f3m9q2xd", "location": { … }, "kind": "STRICT_MODE", "since": "…", "messageCount": 37 } ]
}
```

| Endpoint | Body | Effect |
| --- | --- | --- |
| `POST /api/staff/review/{reportId}/release` | `{ "team": TeamId \| null }` | Becomes `ACCEPTED`, goes to the worker. A given Team overrides routing |
| `POST /api/staff/review/{reportId}/dismiss` | none | Becomes `DISMISSED` |

## Health
`GET /actuator/health` returns `{ "status": "UP" }`, without authentication. No other actuator endpoint is exposed.

## CORS
`/api/public/**` allows only the passenger web app's origins (`FLARE_ALLOWED_ORIGINS`). The Staff app makes native requests, which CORS does not apply to.
