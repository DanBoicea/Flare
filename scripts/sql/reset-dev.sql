-- ============================================================================================
-- WARNING: DELETES EVERY REPORT, ISSUE, QUESTION AND AUDIT EVENT.
-- For the shared flare-dev database only, to clear test traffic during the build day.
-- NEVER run this on the demo database after rehearsals start.
-- Run it from the Supabase SQL editor (task D01).
--
-- Keeps the configuration and the simulated seed: Areas, Locations, QR codes, Teams, roles and
-- staff members are not touched.
-- ============================================================================================

truncate table flare.audit_event, flare.question, flare.report, flare.issue cascade;
