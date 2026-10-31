import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { UiLanguage } from '../api/types'
import { I18nContext, type I18n } from './context'
import { detectLanguage, translate, type StringKey, type Vars } from './strings'

const STORAGE_KEY = 'flare.language'

function initialLanguage(): UiLanguage {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved === 'ro' || saved === 'en') return saved
  } catch {
    // Storage blocked: fall back to detection.
  }
  return detectLanguage()
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<UiLanguage>(initialLanguage)

  const setLanguage = useCallback((next: UiLanguage) => {
    setLanguageState(next)
    try {
      localStorage.setItem(STORAGE_KEY, next)
    } catch {
      // Not saved; the choice still applies for this visit.
    }
  }, [])

  // Screen readers and hyphenation follow the document language.
  useEffect(() => {
    document.documentElement.lang = language
    document.title = translate(language, 'app.title')
  }, [language])

  const value = useMemo<I18n>(
    () => ({ language, setLanguage, t: (key: StringKey, vars?: Vars) => translate(language, key, vars) }),
    [language, setLanguage],
  )

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}
