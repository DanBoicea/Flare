// Short names for the generated contract types (src/api/schema.d.ts, `npm run gen:api`).
import type { components } from './schema';

type Schemas = components['schemas'];

export type Me = Schemas['Me'];
export type Team = Schemas['Team'];
export type TeamId = Schemas['TeamId'];
export type StaffRole = Schemas['StaffRole'];
export type IssueView = Schemas['IssueView'];
export type IssueList = Schemas['IssueList'];
export type IssueCard = Schemas['IssueCard'];
export type IssueDetail = Schemas['IssueDetail'];
export type IssueStatus = Schemas['IssueStatus'];
export type Severity = Schemas['Severity'];
export type Category = Schemas['Category'];
export type ErrorCode = Schemas['ErrorCode'];
export type UiLanguage = Schemas['UiLanguage'];
export type Problem = Schemas['Problem'];

/** The one-tap status actions; each sends the version the staff member saw. */
export type IssueAction = 'acknowledge' | 'take' | 'resolve' | 'reopen';
