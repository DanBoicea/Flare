import type { IssueCard } from '@/api/types';

/**
 * Which actions make sense for an Issue now. The same rules as the back end's IssueTransitions
 * (docs/architecture/data-model.md), so the app only shows buttons that will work; the server still decides.
 */
export type IssueActionKind = 'acknowledge' | 'take' | 'resolve' | 'reopen' | 'override' | 'handOver';

export function canDo(action: IssueActionKind, issue: Pick<IssueCard, 'status' | 'takenBy'>): boolean {
  switch (action) {
    case 'acknowledge':
      return issue.status === 'NEW';
    case 'take':
      // An Issue handed over keeps IN_PROGRESS but nobody holds it, so it can be taken again.
      return issue.status === 'NEW' || issue.status === 'ACKNOWLEDGED' || (issue.status === 'IN_PROGRESS' && !issue.takenBy);
    case 'resolve':
    case 'handOver':
      return issue.status !== 'RESOLVED';
    case 'reopen':
      return issue.status === 'RESOLVED';
    case 'override':
      return true;
  }
}

/** The card's one quick action: "I'm on it" when it can be taken, "Resolve" when I hold it, otherwise none. */
export function mainAction(issue: Pick<IssueCard, 'status' | 'takenBy'>, myId: string): 'take' | 'resolve' | null {
  if (canDo('take', issue)) return 'take';
  if (issue.status === 'IN_PROGRESS' && issue.takenBy?.id === myId) return 'resolve';
  return null;
}
