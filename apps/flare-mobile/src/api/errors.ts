import type { ErrorCode } from './types';

/** A failed request. `code` is a contract ErrorCode, or NETWORK when no usable answer arrived. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: ErrorCode | 'NETWORK';

  constructor(status: number, code: ErrorCode | 'NETWORK') {
    super(code);
    this.status = status;
    this.code = code;
  }
}
