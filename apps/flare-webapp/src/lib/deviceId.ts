// Random device ID for abuse protection only (ADR-0018); never used for tracking.
const STORAGE_KEY = 'flare.deviceId'

let cached: string | null = null

// crypto.randomUUID is missing on plain-HTTP LAN testing, so build a v4 UUID from getRandomValues.
function randomUuid(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

export function getDeviceId(): string {
  if (cached) return cached
  try {
    cached = localStorage.getItem(STORAGE_KEY)
    if (!cached) {
      cached = randomUuid()
      localStorage.setItem(STORAGE_KEY, cached)
    }
  } catch {
    // Storage blocked (private mode): keep one ID for this page load, so per-device limits still apply.
    cached = randomUuid()
  }
  return cached
}
