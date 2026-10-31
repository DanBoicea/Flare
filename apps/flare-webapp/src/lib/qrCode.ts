/** The QR code ID from a `/q/{qrCodeId}` URL (ADR-0017), or null when the page was opened another way. */
export function readQrCodeId(pathname: string = window.location.pathname): string | null {
  const match = /^\/q\/([^/]+)\/?$/.exec(pathname)
  if (!match) return null
  let id: string
  try {
    id = decodeURIComponent(match[1])
  } catch {
    // Malformed escape (hand-typed or tampered URL): send the raw value, the server flags it as unknown.
    id = match[1]
  }
  // IDs are lowercase; normalising helps people who type the printed short URL by hand.
  // The server decides whether the ID is known; anything odd is Quarantined there, never here.
  return id.toLowerCase().slice(0, 32)
}
