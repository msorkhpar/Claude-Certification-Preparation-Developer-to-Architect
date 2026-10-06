// A raw Messages API client over an injected transport. See ../../statement.md for the contract.
import { logger } from "./logger.ts";
const log = logger("rawClient");
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

function validate(apiKey: string, messages: object[], maxTokens: number): void {
  if (!present(apiKey)) throw new Error("apiKey is required");
  if (!messages || messages.length === 0) throw new Error("messages must not be empty");
  if (!Number.isInteger(maxTokens) || maxTokens < 1) throw new Error("maxTokens must be at least 1");
}

function headersFor(apiKey: string): Record<string, string> {
  return { "x-api-key": apiKey, "anthropic-version": "2023-06-01", "content-type": "application/json" };
}

function bodyFor(model: string, messages: object[], maxTokens: number, system?: string | null): Record<string, unknown> {
  const body: Record<string, unknown> = { model, max_tokens: maxTokens, messages };
  if (present(system)) body.system = system;
  return body;
}

export function buildRequest(apiKey: string, model: string, messages: object[], maxTokens: number, system?: string | null): Request {
  log.debug("buildRequest input", model, messages, maxTokens, system);
  validate(apiKey, messages, maxTokens);
  return { method: "POST", url: URL, headers: headersFor(apiKey), body: JSON.stringify(bodyFor(model, messages, maxTokens, system)) };
}

function errorParts(parsed: any, text: string): [string, string] {
  if (parsed && typeof parsed === "object" && parsed.error && typeof parsed.error === "object") {
    return [parsed.error.type ?? "unknown", String(parsed.error.message ?? "")];
  }
  return ["unknown", text.trim().slice(0, 200)];
}

function pickRequestId(headerId: string | null, bodyId: string | null): string | null {
  return headerId ?? bodyId ?? null;
}

function redact(message: string, apiKey: string): string {
  return message.split(apiKey).join("[redacted]");
}

function errorFrom(response: Response, apiKey: string): ApiError {
  let parsed: any = null;
  try {
    parsed = JSON.parse(response.body);
  } catch {
    // not JSON: keep the raw text
  }
  const [kind, message] = errorParts(parsed, response.body);
  const bodyId = parsed && typeof parsed === "object" ? (parsed.request_id ?? null) : null;
  return new ApiError(response.status, kind, redact(message, apiKey), pickRequestId(response.headers["request-id"] ?? null, bodyId));
}

function parseMessage(text: string): any {
  return JSON.parse(text);
}

export function sendMessages(transport: Transport, apiKey: string, model: string, messages: object[], maxTokens: number, system?: string | null): any {
  const request = buildRequest(apiKey, model, messages, maxTokens, system);
  const response = transport(request);
  if (response.status >= 200 && response.status < 300) return parseMessage(response.body);
  throw errorFrom(response, apiKey);
}

export function textOf(message: { content?: Array<{ type: string; text?: string }> }): string {
  return (message.content ?? []).filter((b) => b.type === "text").map((b) => b.text ?? "").join("");
}
