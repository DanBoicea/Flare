# A07: Emergency words, bot check, rate limits, Strict mode, Bounce tracker

Status: TODO
Owner:
Track: A · Phase 2 · Estimate: 1.5 h · Agent-friendly: yes for the pure classes with their tests (OpenCode), wiring by the main session
Depends on: A06
Unblocks: A09

## Goal
The deterministic safety net ([decisions round 4](../decisions.md)) and the mentors' request limiting, in plain, tested Java.

## Read first
[pipeline sections 1 and 3](../architecture/pipeline.md) · [ADR-0015](../adr/0015-single-instance-in-memory-limits.md) · [ADR-0018](../adr/0018-hashed-device-and-network-keys.md) · [ADR-0019](../adr/0019-bot-check-without-third-party.md).

## Do (all pure classes **test first**, with an injected `Clock`)
1. **`safety/TextNormalizer`:** lower case, strip diacritics (ă→a, â→a, î→i, ș/ş→s, ț/ţ→t), collapse whitespace and punctuation.
2. **`safety/EmergencyDetector`:**
   - Reads `safety/emergency-terms.yml` with `ro:` and `en:` phrase lists, and matches whole phrases on normalised text.
   - Start with about 20 phrases per language. Examples: "nu respira", "a lesinat", "atac de cord", "sangereaza", "copil pierdut", "mi-am pierdut copilul", "incendiu", "foc", "fum", "bomba", "arma", "someone collapsed", "not breathing", "heart attack", "bleeding", "lost child", "my child is missing", "fire", "smoke", "gun", "bomb".
   - Tests include false-positive guards, such as "the fire exit sign is broken" and "fire extinguisher missing", which must **not** match. Prefer specific phrases ("there is a fire", "e foc", "miroase a fum") over single words where a single word causes false positives. Document each choice in a YAML comment. A false positive costs little, since staff can lower the Severity; a false negative is still caught by the LLM.
3. **`safety/BotCheck`:** a non-empty honeypot, or `elapsedMs` < `min-elapsed-ms`, means it's a bot.
4. **`ratelimit/SlidingWindowCounter`:** per-key timestamp deques, `tryAcquire(key, limit, window)`, eviction of idle keys.
5. **`ratelimit/RateLimitService`:** device (10 min and daily), network and QR code limits from `flare.limits`. Returns `OK`, `RATE_LIMIT` or `QR_CODE_STRICT_MODE`.
6. **`ratelimit/QrCodeFloodGuard`:** a QR code over its limit enters Strict mode for `strict-mode-duration`. It exposes the active alerts (QR code ID, since when, message count) for A09.
7. **`ratelimit/BounceTracker`:** counts Bounces per device hash in a window. `sendAnywayAvailable` at 3 or more; at 6 or more, messages count as `RATE_LIMIT`.
8. **Wire them** into `MessageIntakeService` in place of A06's no-ops, in the exact step order. **Emergencies skip every limit.**
9. **Network address:** use `request.getRemoteAddr()`. Behind a proxy it is correct only with `server.forward-headers-strategy=framework`, which is enabled only in the `demo` profile and documented in A11.

## Seams from A06
A06 already calls these interfaces in the pipeline order: `safety.BotCheck`, `ratelimit.RateLimitService` (returns `RATE_LIMIT` or `QR_CODE_STRICT_MODE`, counting every non-Emergency message) and `ratelimit.BounceTracker`. Replace the placeholder classes `NoBotCheck`, `NoRateLimits` and `NoBounceTracker` with the real implementations; `MessageIntakeService` needs no change.

## Notes from D06 (for the A07 owner)
Found while writing the demo scenarios. Nothing in A07 is changed; these are for you to decide.
1. **Running every demo scenario from one laptop trips the network limit.** Every simulated Passenger shares the laptop's network: 30 messages per 10 minutes, while `run-scenario.mjs all` sends about 45. D06 works around it (spam wave last, restart the back end before a full run). Suggestion: raise only the network limit in `application-local.yml`, for example `flare.limits.network.per-10-min: 200`. The device and QR code limits stay real, so the spam wave still shows Quarantine and Strict mode, and the `demo` profile keeps 30.
2. **"Three Bounces, then Send anyway" ends as `RATE_LIMIT`, not `SEND_ANYWAY`.** Bounced messages count towards the device limit (3 per 10 minutes), and the rate-limit step (6) runs before the send-anyway step (7). So the fourth message is always over the limit. The Passenger sees the same result, but Needs review shows the wrong reason. Options: don't count Bounced messages towards the device limit, check send-anyway before the rate limits, or accept it.

## Done when
- [ ] Unit tests cover every class, including the window edges and the emergency false-positive guards.
- [ ] A loop of 25 messages from one device to one QR code shows: the first 3 accepted, then `RATE_LIMIT` Quarantined, then Strict mode on that QR code, and an Emergency phrase still returns `EMERGENCY`.
