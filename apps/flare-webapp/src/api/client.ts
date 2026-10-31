import type { ErrorCode, MessageRequest, MessageResponse, Problem, SendToStaffResponse } from './types'
import { mockSendMessage, mockSendQuestionToStaff } from './mock'

const BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')
const USE_MOCK = import.meta.env.VITE_MOCK === 'true'
const TIMEOUT_MS = 20_000

/** A failed request. `code` is a contract ErrorCode, or NETWORK when no usable answer arrived. */
export class ApiError extends Error {
  readonly status: number
  readonly code: ErrorCode | 'NETWORK'

  constructor(status: number, code: ErrorCode | 'NETWORK') {
    super(code)
    this.status = status
    this.code = code
  }
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const controller = new AbortController()
  // The timeout covers the whole exchange, including reading the body.
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS)
  try {
    let response: Response
    try {
      response = await fetch(`${BASE_URL}${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
        signal: controller.signal,
      })
    } catch {
      throw new ApiError(0, 'NETWORK')
    }

    if (!response.ok) {
      let code: ErrorCode = 'INTERNAL_ERROR'
      try {
        const problem = (await response.json()) as Partial<Problem>
        if (problem.code) code = problem.code
      } catch {
        // Not a Problem Details body; keep the generic code.
      }
      throw new ApiError(response.status, code)
    }

    try {
      return (await response.json()) as T
    } catch {
      // Body cut off, aborted mid-read, or not JSON.
      throw new ApiError(response.status, 'NETWORK')
    }
  } finally {
    clearTimeout(timer)
  }
}

export function sendMessage(request: MessageRequest): Promise<MessageResponse> {
  if (USE_MOCK) return mockSendMessage(request)
  return post<MessageResponse>('/api/public/messages', request)
}

export function sendQuestionToStaff(questionId: string, deviceId: string): Promise<SendToStaffResponse> {
  if (USE_MOCK) return mockSendQuestionToStaff()
  return post<SendToStaffResponse>(
    `/api/public/questions/${encodeURIComponent(questionId)}/send-to-staff`,
    { deviceId },
  )
}
