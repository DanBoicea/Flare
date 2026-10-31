import { ApiError } from './errors';
import { mockApi } from './mock';
import type { ErrorCode, IssueAction, IssueDetail, IssueList, IssueView, Me, Problem, Team } from './types';

export { ApiError } from './errors';

const BASE_URL = (process.env.EXPO_PUBLIC_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');
export const USE_MOCK = process.env.EXPO_PUBLIC_MOCK === 'true';
const TIMEOUT_MS = 15_000;

/** Supplies the Supabase access token for each request. Set by the session code (B02). */
let getAccessToken: () => Promise<string | null> = async () => null;

export function setAccessTokenProvider(provider: () => Promise<string | null>) {
  getAccessToken = provider;
}

async function request<T>(method: 'GET' | 'POST', path: string, body?: unknown): Promise<T> {
  const token = await getAccessToken();
  const controller = new AbortController();
  // The timeout covers the whole exchange, including reading the body.
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    let response: Response;
    try {
      response = await fetch(`${BASE_URL}${path}`, {
        method,
        headers: {
          Accept: 'application/json',
          ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: body !== undefined ? JSON.stringify(body) : undefined,
        signal: controller.signal,
      });
    } catch {
      throw new ApiError(0, 'NETWORK');
    }

    if (!response.ok) {
      let code: ErrorCode = 'INTERNAL_ERROR';
      try {
        const problem = (await response.json()) as Partial<Problem>;
        if (problem.code) code = problem.code;
      } catch {
        // Not a Problem Details body; keep the generic code.
      }
      throw new ApiError(response.status, code);
    }

    try {
      return (await response.json()) as T;
    } catch {
      // Body cut off, aborted mid-read, or not JSON.
      throw new ApiError(response.status, 'NETWORK');
    }
  } finally {
    clearTimeout(timer);
  }
}

export function getMe(): Promise<Me> {
  if (USE_MOCK) return mockApi.getMe();
  return request<Me>('GET', '/api/staff/me');
}

export function listTeams(): Promise<Team[]> {
  if (USE_MOCK) return mockApi.listTeams();
  return request<Team[]>('GET', '/api/staff/teams');
}

export function listIssues(view: IssueView): Promise<IssueList> {
  if (USE_MOCK) return mockApi.listIssues(view);
  return request<IssueList>('GET', `/api/staff/issues?view=${view}`);
}

export function getIssue(id: string): Promise<IssueDetail> {
  if (USE_MOCK) return mockApi.getIssue(id);
  return request<IssueDetail>('GET', `/api/staff/issues/${encodeURIComponent(id)}`);
}

export function issueAction(id: string, action: IssueAction, version: number): Promise<IssueDetail> {
  if (USE_MOCK) return mockApi.issueAction(id, action, version);
  return request<IssueDetail>('POST', `/api/staff/issues/${encodeURIComponent(id)}/${action}`, { version });
}
