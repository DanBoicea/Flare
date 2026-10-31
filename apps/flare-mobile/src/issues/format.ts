import { ApiError } from '@/api/errors';
import type { IssueCard, UiLanguage } from '@/api/types';
import { hasKey, type StringKey, type Vars } from '@/i18n/strings';

type T = (key: StringKey, vars?: Vars) => string;

/** Whole minutes from `iso` to `nowMs`, never negative. */
export function minutesSince(iso: string, nowMs: number): number {
  return Math.max(0, Math.floor((nowMs - Date.parse(iso)) / 60_000));
}

/** "just now", "12 min ago", "3 h ago". */
export function ageText(t: T, minutes: number): string {
  if (minutes < 1) return t('age.now');
  if (minutes < 60) return t('age.minutes', { minutes });
  return t('age.hours', { hours: Math.floor(minutes / 60) });
}

/**
 * The card's "who reported" line. With several Passengers it comes from the REPORTERS priority reason
 * ("3 people reported · 8 min", first to last Report); with one, it gives the age.
 */
export function reportersText(t: T, issue: IssueCard, nowMs: number): string {
  const reporters = issue.priority.reasons.find((reason) => reason.code === 'REPORTERS');
  if (reporters?.count && reporters.count > 1) {
    return t('priority.REPORTERS', { count: reporters.count, minutes: reporters.minutes ?? 0 });
  }
  return t('card.reportedOne', { minutes: minutesSince(issue.firstReportedAt, nowMs) });
}

/** A clock time in the app's language, e.g. 14:05. */
export function clockTime(iso: string, language: UiLanguage): string {
  return new Date(iso).toLocaleTimeString(language === 'ro' ? 'ro-RO' : 'en-GB', { hour: '2-digit', minute: '2-digit' });
}

/** The translated message for a failed request. */
export function errorText(t: T, error: unknown): string {
  const key = error instanceof ApiError ? `error.${error.code}` : 'error.INTERNAL_ERROR';
  return hasKey(key) ? t(key) : t('error.INTERNAL_ERROR');
}
