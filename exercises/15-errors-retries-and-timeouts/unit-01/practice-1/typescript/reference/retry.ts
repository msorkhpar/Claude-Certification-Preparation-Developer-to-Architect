// A retry policy for API calls. See ../../statement.md for the contract.
import { logger } from "../logger.ts";
const log = logger("retry");

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

function statusRetryable(status: number): boolean {
  return status === 408 || status === 409 || status === 429 || status >= 500;
}

function spendCap(response: Response): boolean {
  return response.body?.error?.details?.error_code === "enforced_spend_limit_reached";
}

function retryable(response: Response): boolean {
  return statusRetryable(response.status) && !spendCap(response);
}

function errorType(response: Response): string {
  return response.body?.error?.type ?? "unknown";
}

function requestId(response: Response): string | null {
  return response.headers["request-id"] ?? null;
}

function failure(response: Response, attempts: number): CallFailed {
  return new CallFailed(response.status, errorType(response), attempts, requestId(response));
}

function connectionFailure(attempts: number): CallFailed {
  return new CallFailed(0, "connection_error", attempts);
}

function retryAfter(response: Response): number {
  const seconds = Number.parseFloat(response.headers["retry-after"] ?? "");
  return Number.isFinite(seconds) && seconds > 0 ? seconds : 0;
}

function delayFor(attempt: number, baseDelay: number, cap: number, jitter: (delay: number) => number): number {
  return jitter(Math.min(cap, baseDelay * 2 ** (attempt - 1)));
}

export function callWithRetry(send: () => Response, sleep: (seconds: number) => void, options: Options = {}): Response {
  log.debug("callWithRetry input", options);
  const { maxAttempts = 4, baseDelay = 0.5, cap = 8, jitter = (delay: number) => delay } = options;
  for (let attempt = 1; ; attempt++) {
    let wait = 0;
    try {
      const response = send();
      if (response.status < 400) return response;
      if (!retryable(response) || attempt >= maxAttempts) throw failure(response, attempt);
      wait = retryAfter(response);
    } catch (err) {
      if (!(err instanceof TransportError)) throw err;
      if (attempt >= maxAttempts) throw connectionFailure(attempt);
    }
    sleep(Math.max(delayFor(attempt, baseDelay, cap, jitter), wait));
  }
}
