/** Milliseconds since the page started loading, sent with each message for the bot check (ADR-0019). */
export function elapsedMs(): number {
  // performance.now() counts from navigation start, so no module-load timing is involved.
  return Math.round(performance.now())
}
