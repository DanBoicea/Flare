# Principles

Priority order when principles conflict. The safety boundary in [CLAUDE.md](../CLAUDE.md) overrides everything here.

1. **Usability.** Passengers: done in about 30 seconds, one text field, large touch targets, plain language. Designed first for elderly and non-technical people. Staff: zero training, glanceable, a mobile app. Accessible (contrast, text size, screen readers). Romanian and English at minimum. The UI is clean and clearly human-made, not the generic AI-generated look.
2. **Trust and transparency.** Tell the passenger what happens to their report and roughly when. Label AI involvement honestly. Never pretend to be a human. Set realistic expectations. When a report is bounced, say so plainly and explain how to fix it.
3. **Security and abuse resistance.** The back end must not be flooded with spam, and fake QR codes are treated as a real threat. See [security and abuse](security-and-abuse.md).
4. **Signal over noise.** Severity, deduplication and the quality gate so staff see what matters first. Avoid alert fatigue.
5. **Simplicity.** A small feature set done well beats many half-finished features. Prefer simple, explainable logic over clever logic.

## Privacy (GDPR-minded)

- Collect minimal data. Anonymous by default. Contact details optional.
- Limited retention. The period is not decided yet (open question).
- Careful handling of photos that may contain faces. No tracking.
- Tell people what is stored and why, in plain language, where they enter it.

## Honesty about limits

- Location can be spoofed. The code ID is the primary location signal.
- LLMs make mistakes, including false rejections by the quality gate. We measure and report this.
- Demo data is simulated and always labelled so.
- Nothing about the airport is invented. Unknown facts are marked as assumptions.
- Competing products exist. We do not claim novelty, and we verify their features before comparing.
