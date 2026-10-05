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
  // TODO 1 of 7 (finish this to pass m1 and e1): may a reply with this status be tried again?
  // Receives the HTTP status. Returns true for 408, 409, 429 and every status of 500 and above (529 included); false for every other one.
  // Example: statusRetryable(529) -> true, statusRetryable(404) -> false
  return false;
}

function spendCap(response: Response): boolean {
  // TODO 2 of 7 (finish this to pass e4): is this 429 a spend cap rather than a rate limit?
  // Receives the Response. Returns true when its body's error.details.error_code is "enforced_spend_limit_reached" (any level may be
  // missing); retrying cannot succeed then.
  // Example: a body { error: { details: { error_code: "enforced_spend_limit_reached" } } } -> true; {} -> false
  return false;
}

function retryable(response: Response): boolean {
  return statusRetryable(response.status) && !spendCap(response);
}

function errorType(response: Response): string {
  // TODO 3 of 7 (finish this to pass e1, e4 and e6): the API's error type of a reply.
  // Receives the Response. Returns body.error.type, or "unknown" when the body has none.
  // Example: a body { error: { type: "overloaded_error" } } -> "overloaded_error"; {} -> "unknown"
  return "";
}

function requestId(response: Response): string | null {
  // TODO 4 of 7 (finish this to pass e1, e4 and e6): the request id of a reply.
  // Receives the Response. Returns its `request-id` header (header names are lower case), or null when there is none.
  // Example: headers { "request-id": "req_1" } -> "req_1"; {} -> null
  return null;
}

function failure(response: Response, attempts: number): CallFailed {
  return new CallFailed(response.status, errorType(response), attempts, requestId(response));
}

function connectionFailure(attempts: number): CallFailed {
  // TODO 5 of 7 (finish this to pass e5): the failure when the last attempt lost its connection.
  // Receives the number of attempts made. Returns a CallFailed with status 0, error type "connection_error", that many attempts and no
  // request id. Example: connectionFailure(2) -> CallFailed(0, "connection_error", 2)
  return new CallFailed(0, "", attempts);
}

function retryAfter(response: Response): number {
  // TODO 6 of 7 (finish this to pass e2): the wait the server asked for.
  // Receives the Response. Returns its `retry-after` header as seconds (never below 0); 0 when the header is absent or not a number.
  // Example: headers { "retry-after": "3" } -> 3; {} -> 0; { "retry-after": "soon" } -> 0
  return 0;
}

function delayFor(attempt: number, baseDelay: number, cap: number, jitter: (delay: number) => number): number {
  // TODO 7 of 7 (finish this to pass m1, e3 and e5): the wait before the next attempt.
  // Receives the attempt that just failed (1 for the first), baseDelay, cap and the jitter function. Returns
  // jitter(min(cap, baseDelay * 2 ** (attempt - 1))): the jitter is applied last, after the cap.
  // Example: delayFor(3, 1, 5, (d) => d) -> 4, delayFor(4, 1, 5, (d) => d) -> 5
  return 0;
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
