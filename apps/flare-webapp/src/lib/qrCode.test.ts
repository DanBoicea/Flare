import { describe, expect, it } from 'vitest'
import { readQrCodeId } from './qrCode'

describe('readQrCodeId', () => {
  it('reads the ID from /q/{id}, with or without a trailing slash', () => {
    expect(readQrCodeId('/q/k7m2x9')).toBe('k7m2x9')
    expect(readQrCodeId('/q/k7m2x9/')).toBe('k7m2x9')
  })

  it('lowercases hand-typed IDs', () => {
    expect(readQrCodeId('/q/K7M2X9')).toBe('k7m2x9')
  })

  it('returns null when the page was not opened from a QR code', () => {
    expect(readQrCodeId('/')).toBeNull()
    expect(readQrCodeId('/q/')).toBeNull()
    expect(readQrCodeId('/q/a/b')).toBeNull()
  })

  it('keeps a malformed escape as-is (the server flags it), and caps the length at 32', () => {
    expect(readQrCodeId('/q/%E0%A4%A')).toBe('%e0%a4%a')
    expect(readQrCodeId(`/q/${'a'.repeat(40)}`)).toHaveLength(32)
  })
})
