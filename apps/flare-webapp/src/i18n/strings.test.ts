import { describe, expect, it } from 'vitest'
import en from './en.json'
import ro from './ro.json'
import { hasKey, translate } from './strings'

// Every reason and error code the contract can send (src/api/schema.d.ts). A new one must get strings.
const BOUNCE_REASONS = ['NOT_AIRPORT_RELATED', 'JOKE_OR_NONSENSE', 'SPAM_OR_ADVERTISING', 'ABUSIVE', 'TOO_UNCLEAR']

describe('string files', () => {
  it('have exactly the same keys in Romanian and English', () => {
    expect(Object.keys(en).sort()).toEqual(Object.keys(ro).sort())
  })

  it('have no empty strings', () => {
    for (const [lang, file] of Object.entries({ ro, en })) {
      for (const [key, value] of Object.entries(file)) {
        expect(value.trim(), `${lang}: ${key}`).not.toBe('')
      }
    }
  })

  it('use the same {placeholders} in both languages', () => {
    const vars = (s: string) => (s.match(/\{\w+\}/g) ?? []).sort()
    for (const key of Object.keys(ro) as (keyof typeof ro)[]) {
      expect(vars(en[key]), key).toEqual(vars(ro[key]))
    }
  })

  it('have a title and a body for every Bounce reason', () => {
    for (const reason of BOUNCE_REASONS) {
      expect(hasKey(`bounce.${reason}`), reason).toBe(true)
      expect(hasKey(`bounce.title.${reason}`), reason).toBe(true)
    }
  })

  it('keep English free of Romanian letters, except Romanian names', () => {
    const allowed = new Set(['app.airport', 'language.switch'])
    for (const [key, value] of Object.entries(en)) {
      if (allowed.has(key)) continue
      expect(value, key).not.toMatch(/[ăâîșțĂÂÎȘȚ]/)
    }
  })

  it('use Romanian comma-below ș ț, never the cedilla ş ţ', () => {
    for (const [key, value] of Object.entries(ro)) {
      expect(value, key).not.toMatch(/[şţŞŢ]/)
    }
  })

  it('never start a Romanian sentence with "Ia" (reads as "la" in Instrument Sans)', () => {
    for (const [key, value] of Object.entries(ro)) {
      expect(value, key).not.toMatch(/(^|[.!?]\s+)Ia\b/)
    }
  })
})

describe('translate', () => {
  it('fills in variables', () => {
    expect(translate('en', 'compose.counter', { count: 850, max: 1000 })).toBe('850 of 1000 characters')
    expect(translate('ro', 'compose.counter', { count: 850, max: 1000 })).toBe('850 din 1000 caractere')
  })

  it('leaves unknown variables visible instead of dropping them', () => {
    expect(translate('en', 'compose.counter', { count: 1 })).toBe('1 of {max} characters')
  })
})
