// A raw Messages API client over an injected transport. See ../../statement.md for the contract.
export const URL = "https://api.anthropic.com/v1/messages";

export type Request = { method: string; url: string; headers: Record<string, string>; body: string };
export type Response = { status: number; headers: Record<string, string>; body: string }; // header names in lower case
export type Transport = (request: Request) => Response;

/** A non-2xx reply: status, error type, message and the request id (null when there is none). */
export class ApiError extends Error {
  status: number;
  errorType: string;
  detail: string;
  requestId: string | null;
  constructor(status: number, errorType: string, message: string, requestId: string | null = null) {
    super(`${status} ${errorType}: ${message}`);
    this.status = status;
    this.errorType = errorType;
    this.detail = message;
    this.requestId = requestId;
  }
}

function present(value: unknown): value is string {
  return typeof value === "string" && value.trim() !== "";
}

export function buildRequest(apiKey: string, model: string, messages: object[], maxTokens: number, system?: string | null): Request {
  if (!present(apiKey)) throw new Error("apiKey is required");
  if (!messages || messages.length === 0) throw new Error("messages must not be empty");
  if (!Number.isInteger(maxTokens) || maxTokens < 1) throw new Error("maxTokens must be at least 1");
  const body: Record<string, unknown> = { model, max_tokens: maxTokens, messages };
  if (present(system)) body.system = system;
  const headers = { "x-api-key": apiKey, "anthropic-version": "2023-06-01", "content-type": "application/json" };
  return { method: "POST", url: URL, headers, body: JSON.stringify(body) };
}

function errorFrom(response: Response, apiKey: string): ApiError {
  let requestId: string | null = response.headers["request-id"] ?? null;
  let kind = "unknown";
  let message = response.body.trim().slice(0, 200);
  try {
    const parsed = JSON.parse(response.body);
    if (parsed && typeof parsed === "object" && parsed.error && typeof parsed.error === "object") {
      kind = parsed.error.type ?? "unknown";
      message = String(parsed.error.message ?? "");
      requestId = parsed.request_id ?? requestId ?? null;
    }
  } catch {
    // not JSON: keep the raw text
  }
  return new ApiError(response.status, kind, message.split(apiKey).join("[redacted]"), requestId);
}

export function sendMessages(transport: Transport, apiKey: string, model: string, messages: object[], maxTokens: number, system?: string | null): any {
  const request = buildRequest(apiKey, model, messages, maxTokens, system);
  const response = transport(request);
  if (response.status >= 200 && response.status < 300) return JSON.parse(response.body);
  throw errorFrom(response, apiKey);
}

export function textOf(message: { content?: Array<{ type: string; text?: string }> }): string {
  return (message.content ?? []).filter((b) => b.type === "text").map((b) => b.text ?? "").join("");
}
