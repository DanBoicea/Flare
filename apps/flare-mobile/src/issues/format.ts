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
 * The card's "who reported, and since when" line, as in the V1 mock-up: "Reported by 3 people, 14 min ago".
 * The time is said once, counted from the first Report.
 */
export function reportersText(t: T, issue: IssueCard, nowMs: number): string {
  const age = ageText(t, minutesSince(issue.firstReportedAt, nowMs));
  return issue.reporterCount > 1
    ? t('card.reportedMany', { count: issue.reporterCount, age })
    : t('card.reportedOne', { age });
}

/** What is happening with the Issue right now, from its status (and who took it). */
export function statusText(t: T, issue: IssueCard): string {
  switch (issue.status) {
    case 'NEW':
      return t('card.new');
    case 'ACKNOWLEDGED':
      return t('card.acknowledged');
    case 'IN_PROGRESS':
      return issue.takenBy ? t('card.takenBy', { name: issue.takenBy.displayName }) : t('card.inProgress');
    case 'RESOLVED':
      return t('card.resolved');
  }
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
