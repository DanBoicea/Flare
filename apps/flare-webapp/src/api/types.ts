// Short names for the generated contract types (src/api/schema.d.ts, `npm run gen:api`).
import type { components } from './schema'

type Schemas = components['schemas']

export type MessageRequest = Schemas['MessageRequest']
export type MessageResponse = Schemas['MessageResponse']
export type Answer = Schemas['Answer']
export type Bounce = Schemas['Bounce']
export type SendToStaffResponse = Schemas['SendToStaffResponse']
export type Outcome = Schemas['Outcome']
export type BounceReason = Schemas['BounceReason']
export type ErrorCode = Schemas['ErrorCode']
export type UiLanguage = Schemas['UiLanguage']
export type Problem = Schemas['Problem']
