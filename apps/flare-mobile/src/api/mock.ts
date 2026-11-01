// Mock mode (EXPO_PUBLIC_MOCK=true): the same functions as client.ts, served from src/fixtures/
// with actions changing the fixtures in memory. Visibility and the allowed actions follow the server's
// StaffAccessPolicy and IssueTransitions, so the app behaves the same against the real back end.
import { fixtureIssues } from '@/fixtures/issues';
import { fixtureReview } from '@/fixtures/review';
import { fixtureStaff, fixtureTeams } from '@/fixtures/staff';
import { canDo } from '@/issues/transitions';
import { ApiError } from './errors';
import type {
  HistoryEntry, IssueAction, IssueCard, IssueDetail, IssueList, IssueOverride, IssueView, Me, ReviewList, Team, TeamId,
} from './types';

const DELAY_MS = 300;

const issues: IssueDetail[] = structuredClone(fixtureIssues);
const review: ReviewList = structuredClone(fixtureReview);
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

/** Visibility, version, then the transition: the same order and errors as IssueActionService. */
function load(id: string, version: number, action: Parameters<typeof canDo>[0]): IssueDetail {
  const issue = find(id);
  if (issue.version !== version) fail(409, 'VERSION_CONFLICT');
  if (!canDo(action, issue)) fail(409, 'INVALID_TRANSITION');
  return issue;
}

function record(issue: IssueDetail, action: HistoryEntry['action'], details: HistoryEntry['details'] = {}) {
  issue.history.push({ action, staffDisplayName: current.displayName, at: new Date().toISOString(), details });
  issue.version += 1;
}

function requireDutyManager() {
  if (!current.roles.includes('DUTY_MANAGER')) fail(403, 'FORBIDDEN');
}

function takeFromReview(reportId: string) {
  const index = review.items.findIndex((item) => item.reportId === reportId);
  if (index < 0) fail(404, 'NOT_FOUND');
  return review.items.splice(index, 1)[0];
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
    const issue = load(id, version, action);
    switch (action) {
      case 'acknowledge':
        issue.status = 'ACKNOWLEDGED';
        record(issue, 'ACKNOWLEDGED');
        break;
      case 'take':
        issue.status = 'IN_PROGRESS';
        issue.takenBy = { id: current.id, displayName: current.displayName };
        record(issue, 'TAKEN');
        break;
      case 'resolve':
        issue.status = 'RESOLVED';
        record(issue, 'RESOLVED');
        break;
      case 'reopen':
        issue.status = 'NEW';
        issue.takenBy = null;
        issue.reopened = true;
        record(issue, 'REOPENED');
        break;
    }
    return later(issue);
  },

  overrideIssue: (id: string, version: number, change: IssueOverride): Promise<IssueDetail> => {
    if (!change.category && !change.severity) fail(400, 'INVALID_REQUEST');
    const issue = load(id, version, 'override');
    if (change.category && change.category !== issue.category) {
      record(issue, 'CATEGORY_CHANGED', { from: issue.category, to: change.category });
      issue.category = change.category;
    }
    if (change.severity && change.severity !== issue.severity) {
      record(issue, 'SEVERITY_CHANGED', { from: issue.severity, to: change.severity });
      issue.severity = change.severity;
    }
    return later(issue);
  },

  handOverIssue: (id: string, version: number, team: TeamId): Promise<IssueDetail> => {
    const issue = load(id, version, 'handOver');
    if (issue.team === team) fail(409, 'INVALID_TRANSITION');
    record(issue, 'HANDED_OVER', { from: issue.team, to: team });
    issue.team = team;
    issue.takenBy = null;
    return later(issue);
  },

  getReview: (): Promise<ReviewList> => {
    requireDutyManager();
    return later(review);
  },

  releaseReport: (reportId: string, team: TeamId | null): Promise<void> => {
    requireDutyManager();
    const item = takeFromReview(reportId);
    const now = new Date().toISOString();
    // Like the worker: a released Report becomes a new Issue (for the chosen Team, or unrouted).
    issues.push({
      id: `40000000-0000-4000-8000-${String(Date.now()).slice(-12).padStart(12, '0')}`,
      version: 0,
      status: 'NEW',
      severity: 'MEDIUM',
      category: 'OTHER',
      team,
      location: item.location,
      mentionedPlace: null,
      summary: { ro: item.text, en: item.text },
      summarySource: 'FALLBACK',
      reportCount: 1,
      reporterCount: 1,
      firstReportedAt: item.createdAt,
      lastReportedAt: item.createdAt,
      takenBy: null,
      emergency: false,
      unclassified: true,
      reopened: false,
      priority: { score: 300, reasons: [{ code: 'SEVERITY', severity: 'MEDIUM' }] },
      reports: [{ id: item.reportId, text: item.text, language: 'ro', createdAt: item.createdAt, contact: null, flightNumber: null, mentionedPlace: null, classificationSource: 'FALLBACK' }],
      history: [{ action: 'ISSUE_CREATED', staffDisplayName: null, at: now, details: {} }],
    });
    return later(undefined);
  },

  dismissReport: (reportId: string): Promise<void> => {
    requireDutyManager();
    takeFromReview(reportId);
    return later(undefined);
  },
};
