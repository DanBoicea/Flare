# A10: Retention job, log hygiene check

Status: TODO
Owner:
Track: A · Phase 3 · Estimate: 30 min · Agent-friendly: yes
Depends on: A08

## Goal
Show, and actually do, GDPR-minded retention ([data model: retention](../architecture/data-model.md#retention-job-in-a10-runs-daily)).

## Do
1. **`retention/RetentionJob`:** `@Scheduled(cron = "0 30 3 * * *")`, with each rule as a separate, logged step and a deleted-row count:
   - null `report.contact` 30 days after the Issue is resolved
   - delete `question` rows older than 30 days
   - delete dismissed Reports after 7 days
   - delete resolved Issues with their Reports and audit events after 90 days
   
   All periods come from `flare.retention`.
2. **Test it:** a unit test of the date arithmetic with a fixed `Clock`, plus an admin-only manual trigger for the demo. The simplest is a `local`/`demo`-profile-only `@Bean CommandLineRunner` behind a flag, since there is no admin UI.
3. **Log hygiene:** run the demo scenarios (D06) and grep the back-end log for a few phrases from the scenario texts. Expect zero hits. Fix any log line that prints text, contact details or raw IPs.

## Done when
- [ ] The job runs, with its rules unit tested.
- [ ] The grep shows no Passenger text in the logs.
- [ ] The README or pitch can truthfully say "contact details are deleted 30 days after resolution".
