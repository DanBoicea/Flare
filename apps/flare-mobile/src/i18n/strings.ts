import { getLocales } from 'expo-localization';

import type { UiLanguage } from '@/api/types';
import en from './en.json';
import ro from './ro.json';

/** Every UI string key. Romanian is the source of truth for the key list. */
export type StringKey = keyof typeof ro;

// Compile-time parity check: English must have exactly the Romanian keys.
const strings: Record<UiLanguage, Record<StringKey, string>> = { ro, en: en satisfies Record<StringKey, string> };

export type Vars = Record<string, string | number>;

export function translate(language: UiLanguage, key: StringKey, vars?: Vars): string {
  const template = strings[language][key] ?? strings.ro[key] ?? key;
  if (!vars) return template;
  return template.replace(/\{(\w+)\}/g, (match, name: string) => (name in vars ? String(vars[name]) : match));
}

/** Romanian by default; English only when the device's preferred language is English. */
export function detectLanguage(): UiLanguage {
  return getLocales()[0]?.languageCode === 'en' ? 'en' : 'ro';
}

/** True when `key` exists, for codes that come from the server (error codes). */
export function hasKey(key: string): key is StringKey {
  return key in strings.ro;
}
