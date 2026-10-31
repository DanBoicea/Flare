# Data model

Postgres in Supabase, schema `flare` (not exposed through Supabase's Data API; row-level security enabled on every table with no policies, [ADR-0010](../adr/0010-supabase-postgres-and-auth-only.md)). Created only by Flyway migrations in `apps/flare-backend/src/main/resources/db/migration/`. All times are `timestamptz` in UTC. IDs are `uuid` (`gen_random_uuid()`) unless stated.

## Enums (stored as `text` with a `check` constraint; the same values in the contract)

| Enum | Values |
| --- | --- |
| `Category` | `CLEANING_OR_SPILL`, `TOILET`, `LIFT_OR_ESCALATOR`, `BROKEN_OR_MAINTENANCE`, `SIGN_OR_INFORMATION`, `ASSISTANCE_NEEDED`, `LOST_ITEM_OR_PERSON`, `SAFETY_OR_SECURITY`, `OTHER` |
| `Severity` | `CRITICAL`, `HIGH`, `MEDIUM`, `LOW` |
| `TeamId` | `CLEANING`, `MAINTENANCE`, `SECURITY`, `PASSENGER_ASSISTANCE` |
| `StaffRole` | `STAFF`, `DUTY_MANAGER` (the IDs of the `role` rows) |
| `IssueStatus` | `NEW`, `ACKNOWLEDGED`, `IN_PROGRESS`, `RESOLVED` |
| `ReportStatus` | `ACCEPTED`, `QUARANTINED`, `DISMISSED` |
| `QuarantineReason` | `RATE_LIMIT`, `QR_CODE_STRICT_MODE`, `UNKNOWN_QR_CODE`, `MUTED_QR_CODE`, `BOT_CHECK`, `SEND_ANYWAY` |
| `QrCodeStatus` | `ACTIVE`, `MUTED`, `RETIRED` |
| `ClassificationSource` | `LLM`, `FALLBACK` (Unclassified) |
| `Language` | `ro`, `en`, `other` |

Categories and Severities are tentative. Changing them means one migration, the contract, the prompts, the category-to-Team table in config, and both apps' string files.

## Tables

### `area`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | text PK | e.g. `ARR` (simulated) |
| `name_ro`, `name_en` | text | |

### `location`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | uuid PK | |
| `area_id` | text FK → area | |
| `label` | text unique | Location label printed under the QR code, e.g. `ARR-WC-02` |
| `name_ro`, `name_en` | text | e.g. "Toalete sosiri, lângă banda 2" (simulated) |
| `criticality` | smallint 0-2 | Raises priority for critical Locations (0 normal, 2 most critical) |

### `qr_code`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | text PK | Opaque 10-character ID ([ADR-0017](../adr/0017-qr-code-url-format.md)) |
| `location_id` | uuid FK → location | |
| `status` | text `QrCodeStatus` | `MUTED` sends its messages to Quarantined; `RETIRED` is treated as unknown |
| `created_at` | timestamptz | |

### `team`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | text PK `TeamId` | Seeded by migration (real config, not demo data) |
| `name_ro`, `name_en` | text | |

### `staff_member`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | uuid PK | Equals the Supabase Auth user ID (`auth.users.id`, the JWT `sub`) |
| `display_name` | text | Shown in "I'm on it" and the history |
| `active` | boolean | Inactive means 403 on every staff endpoint |
| `created_at` | timestamptz | |

### `staff_member_team`
| Column | Type | Notes |
| --- | --- | --- |
| `staff_member_id` | uuid FK | PK (both columns) |
| `team_id` | text FK | A staff member can be in several Teams |

### `role`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | text PK `StaffRole` | Seeded by migration (real config, not demo data) |
| `name_ro`, `name_en` | text | |

### `staff_member_role`
| Column | Type | Notes |
| --- | --- | --- |
| `staff_member_id` | uuid FK | PK (both columns) |
| `role_id` | text FK | A staff member can hold several roles. A member with no `DUTY_MANAGER` row gets ordinary staff access |

### `question`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | uuid PK | Returned to the web app as `questionId` |
| `scanned_qr_code_id` | text null | Raw scanned value (validated format, max 32 characters); no FK, since it may be unknown |
| `location_id` | uuid null FK | Resolved Location |
| `text` | text | Passenger text (max 1000 characters) |
| `language` | text `Language` | |
| `answer_text` | text null | Knowledge base answer shown |
| `answer_found` | boolean | False means the Knowledge base did not cover it |
| `device_hash` | text | HMAC ([ADR-0018](../adr/0018-hashed-device-and-network-keys.md)) |
| `sent_to_staff_report_id` | uuid null FK → report | Set by "Send to staff anyway" |
| `created_at` | timestamptz | |

### `issue`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | uuid PK | |
| `location_id` | uuid null FK | Location of the first Report (null when unknown) |
| `category` | text `Category` | |
| `severity` | text `Severity` | The highest Severity of its Reports unless overridden |
| `category_overridden`, `severity_overridden` | boolean | Overrides stick; later Reports don't change them |
| `team_id` | text null FK | Null means unrouted, visible to the duty manager only |
| `status` | text `IssueStatus` | |
| `taken_by` | uuid null FK → staff_member | "I'm on it" |
| `summary_ro`, `summary_en` | text | |
| `summary_source` | text `ClassificationSource` | |
| `mentioned_place` | text null | From the latest Report that has one |
| `emergency` | boolean | Any Report flagged as an Emergency |
| `unclassified` | boolean | True while every Report is a fallback |
| `report_count` | int | Accepted Reports |
| `reporter_count` | int | Distinct `device_hash` among accepted Reports (independent Passengers) |
| `first_reported_at`, `last_reported_at` | timestamptz | |
| `acknowledged_at`, `resolved_at` | timestamptz null | |
| `reopened_count` | int | |
| `version` | bigint | Optimistic locking (`@Version`); staff actions send it |
| `created_at`, `updated_at` | timestamptz | |

### `report`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | uuid PK | |
| `issue_id` | uuid null FK | Null until grouped (or while Quarantined) |
| `scanned_qr_code_id` | text null | Raw scanned value, as in `question` |
| `location_id` | uuid null FK | |
| `text` | text | max 1000 characters |
| `language` | text `Language` | |
| `contact` | text null | Optional, max 120 characters; erased by retention |
| `device_hash`, `network_hash` | text | HMAC ([ADR-0018](../adr/0018-hashed-device-and-network-keys.md)) |
| `status` | text `ReportStatus` | |
| `quarantine_reason` | text null `QuarantineReason` | |
| `team_override` | text null FK → team | Set when the duty manager releases a Report to a chosen Team. The worker then creates a new Issue for that Team without matching |
| `classification_source` | text null | Null means not classified yet (Emergency path) |
| `category` | text null | |
| `severity` | text null | |
| `emergency` | boolean | Word list match or the LLM said so |
| `confidence` | numeric(3,2) null | |
| `mentioned_place` | text null | |
| `flight_number` | text null | Only if the Passenger typed one; format-checked |
| `staff_summary_ro`, `staff_summary_en` | text null | One-line summary from classification |
| `created_at` | timestamptz | |
| `grouped_at` | timestamptz null | |

### `audit_event`
| Column | Type | Notes |
| --- | --- | --- |
| `id` | uuid PK | |
| `issue_id` | uuid null FK | |
| `report_id` | uuid null FK | |
| `staff_member_id` | uuid null FK | Null means the system did it |
| `action` | text | `ISSUE_CREATED`, `REPORT_ADDED`, `ACKNOWLEDGED`, `TAKEN`, `RESOLVED`, `REOPENED`, `CATEGORY_CHANGED`, `SEVERITY_CHANGED`, `HANDED_OVER`, `REPORT_RELEASED`, `REPORT_DISMISSED` |
| `details` | jsonb | e.g. `{ "from": "MEDIUM", "to": "HIGH" }`. Never contains Passenger text |
| `created_at` | timestamptz | |

Indexes: `issue(status, team_id)`, `issue(location_id, status)`, `report(issue_id)`, `report(status) where status = 'QUARANTINED'`, `report(issue_id) where issue_id is null and status = 'ACCEPTED'` (worker recovery), `question(created_at)`.

## Issue state machine

```
          take            resolve
  NEW ─────────────▶ IN_PROGRESS ─────────▶ RESOLVED
   │  acknowledge         ▲                  │
   └──────▶ ACKNOWLEDGED ─┘ take             │ reopen (staff), or a matching Report
                                             │ within 30 min of resolving
   NEW ◀─────────────────────────────────────┘ (reopened_count + 1, taken_by cleared)
```

- **Resolve** is allowed from any state except `RESOLVED`, so a quick fix needs one tap.
- **Take** from `NEW` acknowledges implicitly.
- **Hand over** keeps the status but clears `taken_by`, sets the new `team_id`, and records `HANDED_OVER`.

## Report lifecycle

```
message ──(Question)──▶ question row ──(send to staff)──▶ report ACCEPTED
   │
   ├─(Bounced)──▶ not stored (only a counter in memory)
   │
   └─(Report)──▶ report ACCEPTED ──▶ worker ──▶ issue_id set, grouped_at set
                  or QUARANTINED ──(duty manager)──▶ release: ACCEPTED → worker
                                                └─▶ dismiss: DISMISSED
```

## Retention (job in A10, runs daily)

| What | When it is erased | Status |
| --- | --- | --- |
| `report.contact` | 30 days after its Issue is resolved | Decided |
| `question` rows | 30 days after creation | Proposed assumption |
| `report` rows, `issue` rows, their `audit_event`s | 90 days after the Issue is resolved | Proposed assumption |
| Dismissed Reports | 7 days after dismissal | Proposed assumption |
| In-memory counters | On restart | By design |

## Seed data

- **Real configuration** (`team` and `role` rows) goes in a normal versioned migration.
- **Simulated demo data** goes in `db/seed/R__demo_seed.sql`, loaded only by the `local` and `demo` profiles through Flyway's `locations` setting. That covers Areas, Locations, QR codes, and staff members (with their Teams and roles) linked to Supabase users by email. It is produced by D01 from `data/airport/`, and every name is visibly simulated.
- Staff accounts are created by hand in the Supabase dashboard (S03). The seed then links them: `insert into flare.staff_member (id, …) select id, … from auth.users where email = …`.
