// A retry policy for API calls. See ../../statement.md for the contract.
export type Response = { status: number; headers: Record<string, string>; body: any }; // header names in lower case; body is parsed JSON or {}

/** The connection failed or timed out: no reply was received. */
export class TransportError extends Error {}

/** The call did not succeed. errorType is the API's error type, "connection_error" or "unknown". */
export class CallFailed extends Error {
  status: number;
  errorType: string;
  attempts: number;
  requestId: string | null;
  constructor(status: number, errorType: string, attempts: number, requestId: string | null = null) {
    super(`${status} ${errorType} after ${attempts} attempt(s)`);
    this.status = status;
    this.errorType = errorType;
    this.attempts = attempts;
    this.requestId = requestId;
  }
}

export type Options = { maxAttempts?: number; baseDelay?: number; cap?: number; jitter?: (delay: number) => number };

export function callWithRetry(send: () => Response, sleep: (seconds: number) => void, options: Options = {}): Response {
  // TODO: call send() up to maxAttempts times; sleep(delay) between attempts; return the first good Response.
  return undefined as unknown as Response;
}
