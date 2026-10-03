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

function retryable(response: Response): boolean {
  if (response.status === 408 || response.status === 409 || response.status === 429 || response.status >= 500) {
    return response.body?.error?.details?.error_code !== "enforced_spend_limit_reached";
  }
  return false;
}

function failure(response: Response, attempts: number): CallFailed {
  return new CallFailed(response.status, response.body?.error?.type ?? "unknown", attempts, response.headers["request-id"] ?? null);
}

function retryAfter(response: Response): number {
  const seconds = Number.parseFloat(response.headers["retry-after"] ?? "");
  return Number.isFinite(seconds) && seconds > 0 ? seconds : 0;
}

export function callWithRetry(send: () => Response, sleep: (seconds: number) => void, options: Options = {}): Response {
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
      if (attempt >= maxAttempts) throw new CallFailed(0, "connection_error", attempt);
    }
    sleep(Math.max(jitter(Math.min(cap, baseDelay * 2 ** (attempt - 1))), wait));
  }
}
