import AsyncStorage from '@react-native-async-storage/async-storage';
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';

import type { UiLanguage } from '@/api/types';
import { I18nContext, type I18n } from './context';
import { detectLanguage, translate } from './strings';

const STORAGE_KEY = 'flare.language';

/** Starts in the device language, then switches to the saved choice once it has loaded. */
export function I18nProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<UiLanguage>(detectLanguage);

  useEffect(() => {
    AsyncStorage.getItem(STORAGE_KEY)
      .then((saved) => {
        if (saved === 'ro' || saved === 'en') setLanguageState(saved);
      })
      .catch(() => {
        // No saved choice available; keep the device language.
      });
  }, []);

  const setLanguage = useCallback((next: UiLanguage) => {
    setLanguageState(next);
    AsyncStorage.setItem(STORAGE_KEY, next).catch(() => {
      // Not saved; the choice still applies until the app restarts.
    });
  }, []);

  const value = useMemo<I18n>(
    () => ({ language, setLanguage, t: (key, vars) => translate(language, key, vars) }),
    [language, setLanguage],
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}
