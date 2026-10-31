# data/demo-scenarios

**All content is simulated.** The messages, places and QR code IDs come from the simulated airport in [`data/airport/airport.json`](../airport/airport.json). Nothing here is real Passenger data.

Scripted sequences of Passenger messages, played against the back end by `scripts/demo/run-scenario.mjs` (task D06). They make the demo and every rehearsal repeatable: one command sends exactly what real Passengers would send from the passenger web app, and the Staff app shows the result live.

## Before you run

1. **The back end is running** (see [operations](../../docs/architecture/operations.md#running-locally-for-the-readme-setup-section-filled-in-by-s01-and-the-skeleton-tasks)), with the `local` profile so the demo seed is loaded.
2. **Reset the dev database.** The runner creates real Reports and Issues. Run [`scripts/sql/reset-dev.sql`](../../scripts/sql/reset-dev.sql) in the Supabase SQL editor for `flare-dev`. It clears Reports, Issues, Questions and audit events, and keeps Locations, QR codes and staff.
3. **Restart the back end before a full run.** Rate limits are counted in memory ([ADR-0015](../../docs/adr/0015-single-instance-in-memory-limits.md)), so a restart resets them. This matters because every simulated Passenger is really your one laptop, which counts as one network: the default limit is 30 messages per 10 minutes per network, and `all` sends about 45. See "Rate limits" below.

## Run

```
node scripts/demo/run-scenario.mjs --list                  # show the scenarios
node scripts/demo/run-scenario.mjs toilet-paper-burst      # one scenario
node scripts/demo/run-scenario.mjs all                     # every scenario, in file order
node scripts/demo/run-scenario.mjs all --base-url https://<demo back end>
```

The default base URL is `http://localhost:8080`. The runner needs no keys and prints no secrets: the public endpoints have no sign-in.

For each step it prints the Location label, the text and the outcome (`REPORTED`, `ANSWERED`, `BOUNCED` or `EMERGENCY`). At the end of each scenario it prints the **Expect** line.

**Check the Expect line in the Staff app.** The runner only sees what a Passenger sees. A Quarantined message returns `REPORTED`, exactly like a normal Report, on purpose, so spammers cannot tell. Grouping, Team routing, Quarantine and Strict mode can only be checked by signing in to the Staff app as the simulated staff:

| Account | Who | Sees |
| --- | --- | --- |
| `cleaning@demo.flare.invalid` | Ana (simulat) | Cleaning |
| `maintenance@demo.flare.invalid` | Mihai (simulat) | Maintenance and Passenger assistance |
| `security@demo.flare.invalid` | Ioana (simulat) | Security, plus every Critical Issue |
| `manager@demo.flare.invalid` | Radu (simulat) | Everything, plus Needs review |

The exit code is 0 when every request was accepted, and 1 if any failed (network error or HTTP error). Until A06 is built, the message endpoint is a stub, so every step prints `HTTP 501 … (endpoint not built yet)`.

## Scenarios

| File | Shows |
| --- | --- |
| `01-single-report` | One Report classified and routed to Maintenance |
| `02-toilet-paper-burst` | Three Reports grouped into one Issue ("3 people reported"); "dirty" stays separate |
| `03-question-then-send-to-staff` | A Knowledge base answer, then "Send to staff anyway" |
| `04-joke-then-rewrite` | A joke Bounced, then a real Report accepted |
| `05-three-bounces-send-anyway` | Three Bounces, then "Send anyway" into Needs review |
| `06-unknown-and-muted-qr-code` | An unknown QR code ID and the muted QR code, both Quarantined |
| `07-mentioned-other-place` | Scanned at a toilet, about the departures lift: "Scanned at" and "Mentioned" |
| `08-emergency-ro-en` | Emergencies in RO and EN: the 112 screen and Critical Issues for Security |
| `09-cleaning-vs-security` | Ana and Ioana see different Issues; Radu sees both |
| `10-mixed-ro-en` | The same problem in RO and EN, grouped |
| `11-spam-wave` | 25 fake Reports at one QR code: Quarantine, then Strict mode; an Emergency still gets through. **Always last** |

## Rate limits

The defaults are in `apps/flare-backend/src/main/resources/application.yml` (`flare.limits`): 3 messages per device per 10 minutes, 30 per network per 10 minutes, and 20 per QR code per 10 minutes before Strict mode.

- Each scenario uses its own simulated phones: `d1` in one scenario is a different device from `d1` in another, so the per-device limit only applies within a scenario.
- The spam wave is numbered last so it cannot spoil the other scenarios. On demo day, run it on its own as the final act.
- If you run `all` twice without restarting the back end, the network limit Quarantines almost everything on the second run. Restart first.
- `05-three-bounces-send-anyway`: Bounced messages count towards the device limit, so the fourth message ("Send anyway") is over the limit and is Quarantined as `RATE_LIMIT` rather than `SEND_ANYWAY`. The Passenger sees the same result. This was raised with the A07 owner (see the D06 task file).

## File format

```json
{
  "name": "toilet-paper-burst",
  "simulated": true,
  "description": "…",
  "steps": [
    { "qrCodeId": "skp4fq3r5e", "location": "ARR-WC-02", "text": "Nu mai e hârtie igienică", "uiLanguage": "ro", "device": "d1", "waitMs": 1500 },
    { "sendToStaff": true, "device": "d1", "waitMs": 1500 },
    { "qrCodeId": "xw9fwv52ua", "location": "DEP-WC-02", "text": "Toaleta e murdara", "uiLanguage": "ro", "device": "bot-{n}", "waitMs": 200, "repeat": 20 }
  ],
  "expect": "What a person should see in the Staff app"
}
```

- `qrCodeId` must be a real ID from `airport.json`, except in the unknown-QR-code scenario. `location` is only there for humans; the runner never sends it, because the back end must find the Location from the QR code ID alone.
- `device` is a label. The runner turns each label into a random device ID, stable for the whole scenario.
- `sendAnyway: true` sets the "Send anyway" flag on that message.
- `sendToStaff: true` calls "Send to staff" with the `questionId` from the last Question answered for that device.
- `repeat: N` sends the step N times. `{n}` in `device` or `text` becomes 1 … N, so `bot-{n}` simulates N different phones.
- Every message is sent with `elapsedMs: 5000` and an empty honeypot field `website`, like a real browser ([ADR-0019](../../docs/adr/0019-bot-check-without-third-party.md)).
