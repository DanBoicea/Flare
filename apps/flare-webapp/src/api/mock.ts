// Mock mode (VITE_MOCK=true): answers by keyword so every screen can be reached without a back end.
// Also the offline demo fallback. All data is simulated.
import type { MessageRequest, MessageResponse, SendToStaffResponse } from './types'
import { mockResponses } from '../fixtures/responses'

const DELAY_MS = 1500
let bounceCount = 0

const wait = () => new Promise((resolve) => setTimeout(resolve, DELAY_MS))

export async function mockSendMessage(request: MessageRequest): Promise<MessageResponse> {
  await wait()
  const text = request.text.toLowerCase()

  if (/(fire|foc|incendiu|112)/.test(text)) return mockResponses.emergency()
  // "Send anyway" always ends as a (Quarantined) Report on the real server.
  if (request.sendAnyway) return mockResponses.reported()
  if (/(joke|glum|spam|test)/.test(text)) {
    bounceCount += 1
    return mockResponses.bounced(bounceCount >= 3)
  }
  if (text.includes('?')) {
    return text.includes('wifi')
      ? mockResponses.answeredNotFound(request.uiLanguage)
      : mockResponses.answered(request.uiLanguage)
  }
  return mockResponses.reported()
}

export async function mockSendQuestionToStaff(): Promise<SendToStaffResponse> {
  await wait()
  return { outcome: 'REPORTED' }
}
