import { createContext, useContext } from 'react';

import type { UiLanguage } from '@/api/types';
import type { StringKey, Vars } from './strings';

export interface I18n {
  language: UiLanguage;
  setLanguage: (language: UiLanguage) => void;
  t: (key: StringKey, vars?: Vars) => string;
}

export const I18nContext = createContext<I18n | null>(null);

export function useI18n(): I18n {
  const value = useContext(I18nContext);
  if (!value) throw new Error('useI18n must be used inside <I18nProvider>');
  return value;
}
