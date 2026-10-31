-- Flare schema (docs/architecture/data-model.md). Flyway runs this in the "flare" schema.
-- Enums are text columns with check constraints, using the same values as the contract.
-- Row-level security is enabled on every table with no policies (ADR-0010): only the back end,
-- connecting as the table owner, can read or write. Supabase's Data API never sees this schema.

-- ---------- Places ----------

create table area (
    id      text primary key,
    name_ro text not null,
    name_en text not null
);

create table location (
    id          uuid primary key default gen_random_uuid(),
    area_id     text not null references area (id),
    label       text not null unique,
    name_ro     text not null,
    name_en     text not null,
    criticality smallint not null default 0 check (criticality between 0 and 2)
);

create table qr_code (
    id          text primary key,
    location_id uuid not null references location (id),
    status      text not null default 'ACTIVE' check (status in ('ACTIVE', 'MUTED', 'RETIRED')),
    created_at  timestamptz not null default now()
);

-- ---------- Staff ----------

create table team (
    id      text primary key check (id in ('CLEANING', 'MAINTENANCE', 'SECURITY', 'PASSENGER_ASSISTANCE')),
    name_ro text not null,
    name_en text not null
);

create table role (
    id      text primary key check (id in ('STAFF', 'DUTY_MANAGER')),
    name_ro text not null,
    name_en text not null
);

-- id equals the Supabase Auth user ID (the JWT "sub"). No foreign key to auth.users, so the schema
-- doesn't depend on Supabase internals; the demo seed links rows by email.
create table staff_member (
    id           uuid primary key,
    display_name text not null,
    active       boolean not null default true,
    created_at   timestamptz not null default now()
);

create table staff_member_team (
    staff_member_id uuid not null references staff_member (id) on delete cascade,
    team_id         text not null references team (id),
    primary key (staff_member_id, team_id)
);

create table staff_member_role (
    staff_member_id uuid not null references staff_member (id) on delete cascade,
    role_id         text not null references role (id),
    primary key (staff_member_id, role_id)
);

-- ---------- Issues and Reports ----------

create table issue (
    id                  uuid primary key default gen_random_uuid(),
    location_id         uuid references location (id),
    category            text not null check (category in ('CLEANING_OR_SPILL', 'TOILET', 'LIFT_OR_ESCALATOR',
                            'BROKEN_OR_MAINTENANCE', 'SIGN_OR_INFORMATION', 'ASSISTANCE_NEEDED',
                            'LOST_ITEM_OR_PERSON', 'SAFETY_OR_SECURITY', 'OTHER')),
    severity            text not null check (severity in ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    category_overridden boolean not null default false,
    severity_overridden boolean not null default false,
    team_id             text references team (id),
    status              text not null default 'NEW' check (status in ('NEW', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED')),
    taken_by            uuid references staff_member (id),
    summary_ro          text not null,
    summary_en          text not null,
    summary_source      text not null check (summary_source in ('LLM', 'FALLBACK')),
    mentioned_place     text,
    emergency           boolean not null default false,
    unclassified        boolean not null default false,
    report_count        integer not null default 0,
    reporter_count      integer not null default 0,
    first_reported_at   timestamptz not null,
    last_reported_at    timestamptz not null,
    acknowledged_at     timestamptz,
    resolved_at         timestamptz,
    reopened_count      integer not null default 0,
    version             bigint not null default 0,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

create table report (
    id                    uuid primary key default gen_random_uuid(),
    issue_id              uuid references issue (id),
    scanned_qr_code_id    text check (char_length(scanned_qr_code_id) <= 32),
    location_id           uuid references location (id),
    text                  text not null check (char_length(text) between 1 and 1000),
    language              text not null check (language in ('ro', 'en', 'other')),
    contact               text check (char_length(contact) <= 120),
    device_hash           text not null,
    network_hash          text not null,
    status                text not null check (status in ('ACCEPTED', 'QUARANTINED', 'DISMISSED')),
    quarantine_reason     text check (quarantine_reason in ('RATE_LIMIT', 'QR_CODE_STRICT_MODE', 'UNKNOWN_QR_CODE',
                              'MUTED_QR_CODE', 'BOT_CHECK', 'SEND_ANYWAY')),
    team_override         text references team (id),
    classification_source text check (classification_source in ('LLM', 'FALLBACK')),
    category              text check (category in ('CLEANING_OR_SPILL', 'TOILET', 'LIFT_OR_ESCALATOR',
                              'BROKEN_OR_MAINTENANCE', 'SIGN_OR_INFORMATION', 'ASSISTANCE_NEEDED',
                              'LOST_ITEM_OR_PERSON', 'SAFETY_OR_SECURITY', 'OTHER')),
    severity              text check (severity in ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    emergency             boolean not null default false,
    confidence            numeric(3, 2) check (confidence between 0 and 1),
    mentioned_place       text,
    flight_number         text,
    staff_summary_ro      text,
    staff_summary_en      text,
    created_at            timestamptz not null default now(),
    grouped_at            timestamptz
);

create table question (
    id                      uuid primary key default gen_random_uuid(),
    scanned_qr_code_id      text check (char_length(scanned_qr_code_id) <= 32),
    location_id             uuid references location (id),
    text                    text not null check (char_length(text) between 1 and 1000),
    language                text not null check (language in ('ro', 'en', 'other')),
    answer_text             text,
    answer_found            boolean not null default false,
    device_hash             text not null,
    -- Retention erases Questions before Reports, but if a Report goes first the link just clears.
    sent_to_staff_report_id uuid references report (id) on delete set null,
    created_at              timestamptz not null default now()
);

-- Never holds Passenger text (code rules). Deleted together with its Issue or Report by retention.
create table audit_event (
    id              uuid primary key default gen_random_uuid(),
    issue_id        uuid references issue (id) on delete cascade,
    report_id       uuid references report (id) on delete cascade,
    staff_member_id uuid references staff_member (id),
    action          text not null check (action in ('ISSUE_CREATED', 'REPORT_ADDED', 'ACKNOWLEDGED', 'TAKEN',
                        'RESOLVED', 'REOPENED', 'CATEGORY_CHANGED', 'SEVERITY_CHANGED', 'HANDED_OVER',
                        'REPORT_RELEASED', 'REPORT_DISMISSED')),
    details         jsonb not null default '{}'::jsonb,
    created_at      timestamptz not null default now()
);

-- ---------- Indexes ----------

create index issue_status_team_idx on issue (status, team_id);
create index issue_location_status_idx on issue (location_id, status);
create index report_issue_idx on report (issue_id);
create index report_quarantined_idx on report (created_at) where status = 'QUARANTINED';
create index report_ungrouped_idx on report (created_at) where issue_id is null and status = 'ACCEPTED';
create index question_created_at_idx on question (created_at);
create index audit_event_issue_idx on audit_event (issue_id);

-- ---------- Row-level security: enabled, no policies ----------

alter table area enable row level security;
alter table location enable row level security;
alter table qr_code enable row level security;
alter table team enable row level security;
alter table role enable row level security;
alter table staff_member enable row level security;
alter table staff_member_team enable row level security;
alter table staff_member_role enable row level security;
alter table issue enable row level security;
alter table report enable row level security;
alter table question enable row level security;
alter table audit_event enable row level security;
