// Mock mode (EXPO_PUBLIC_MOCK=true): the same functions as client.ts, served from src/fixtures/
// with actions changing the fixtures in memory. Visibility follows the server's StaffAccessPolicy.
import { fixtureIssues } from '@/fixtures/issues';
import { fixtureStaff, fixtureTeams } from '@/fixtures/staff';
import { ApiError } from './errors';
import type { IssueAction, IssueCard, IssueDetail, IssueList, IssueView, Me, Team } from './types';

const DELAY_MS = 300;

const issues: IssueDetail[] = structuredClone(fixtureIssues);
let current: Me = fixtureStaff[3];

/** The mock sign-in picks which fixture staff member is "signed in" (B02's role picker). */
export function setMockStaff(id: string) {
  current = fixtureStaff.find((member) => member.id === id) ?? current;
}

function canSee(member: Me, issue: IssueCard): boolean {
  return member.roles.includes('DUTY_MANAGER')
    || (issue.team !== null && member.teams.includes(issue.team))
    || (issue.severity === 'CRITICAL' && member.teams.includes('SECURITY'));
}

function fail(status: number, code: ApiError['code']): never {
  throw new ApiError(status, code);
}

function later<T>(value: T): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(structuredClone(value)), DELAY_MS));
}

function find(id: string): IssueDetail {
  const issue = issues.find((candidate) => candidate.id === id);
  if (!issue || !canSee(current, issue)) fail(404, 'NOT_FOUND');
  return issue;
}

export const mockApi = {
  getMe: (): Promise<Me> => later(current),

  listTeams: (): Promise<Team[]> => later(fixtureTeams),

  listIssues: (view: IssueView): Promise<IssueList> => {
    const visible = issues
      .filter((issue) => canSee(current, issue))
      .filter((issue) => (view === 'RESOLVED') === (issue.status === 'RESOLVED'))
      .sort((a, b) => b.priority.score - a.priority.score);
    return later({ serverTime: new Date().toISOString(), issues: visible });
  },

  getIssue: (id: string): Promise<IssueDetail> => later(find(id)),

  issueAction: (id: string, action: IssueAction, version: number): Promise<IssueDetail> => {
    const issue = find(id);
    if (issue.version !== version) fail(409, 'VERSION_CONFLICT');
    const me = { id: current.id, displayName: current.displayName };
    switch (action) {
      case 'acknowledge':
        if (issue.status !== 'NEW') fail(409, 'INVALID_TRANSITION');
        issue.status = 'ACKNOWLEDGED';
        break;
      case 'take':
        if (issue.status === 'RESOLVED') fail(409, 'INVALID_TRANSITION');
        issue.status = 'IN_PROGRESS';
        issue.takenBy = me;
        break;
      case 'resolve':
        if (issue.status === 'RESOLVED') fail(409, 'INVALID_TRANSITION');
        issue.status = 'RESOLVED';
        break;
      case 'reopen':
        if (issue.status !== 'RESOLVED') fail(409, 'INVALID_TRANSITION');
        issue.status = 'NEW';
        issue.takenBy = null;
        issue.reopened = true;
        break;
    }
    issue.version += 1;
    return later(issue);
  },
};
