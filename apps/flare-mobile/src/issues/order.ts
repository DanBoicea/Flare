import type { IssueCard } from '@/api/types';

/**
 * The Active list's order: Critical Issues always on top, then the most recent Report first, in both groups.
 * The server still sends its priority order; this re-sorts it so new activity is seen first.
 */
export function activeOrder(issues: IssueCard[]): IssueCard[] {
  return [...issues].sort((a, b) => {
    const critical = Number(b.severity === 'CRITICAL') - Number(a.severity === 'CRITICAL');
    if (critical !== 0) return critical;
    return Date.parse(b.lastReportedAt) - Date.parse(a.lastReportedAt);
  });
}
