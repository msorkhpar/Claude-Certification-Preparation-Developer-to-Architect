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
  // TODO 1 of 8 (finish this to pass e2): refuse bad input before anything is sent.
  // Receives the key, the messages and maxTokens. Throws an Error when the key is blank (use present), the messages are empty or
  // maxTokens is not a whole number of at least 1; otherwise returns nothing.
  // Example: validate("  ", [], 8) -> Error, validate("sk-x", [{ role: "user", content: "hi" }], 8) -> undefined
}

function headersFor(apiKey: string): Record<string, string> {
  // TODO 2 of 8 (finish this to pass m1): the three request headers.
  // Receives the API key. Returns an object with exactly `x-api-key` (the key), `anthropic-version` (2023-06-01) and `content-type`
  // (application/json). Example: headersFor("k")["anthropic-version"] -> "2023-06-01"
  return {};
}

function bodyFor(model: string, messages: object[], maxTokens: number, system?: string | null): Record<string, unknown> {
  // TODO 3 of 8 (finish this to pass m1 and e1): the request body as an object.
  // Receives the model, the messages, maxTokens and the system text (may be null or undefined). Returns an object with `model`,
  // `max_tokens` and `messages`, plus `system` only when it is present and not blank (use present).
  // Example: bodyFor("m", [], 8, "  ") -> { model: "m", max_tokens: 8, messages: [] }
  return {};
}

export function buildRequest(apiKey: string, model: string, messages: object[], maxTokens: number, system?: string | null): Request {
  log.debug("buildRequest input", model, messages, maxTokens, system);
  validate(apiKey, messages, maxTokens);
  return { method: "POST", url: URL, headers: headersFor(apiKey), body: JSON.stringify(bodyFor(model, messages, maxTokens, system)) };
}

function errorParts(parsed: any, text: string): [string, string] {
  // TODO 4 of 8 (finish this to pass e4 and e5): the error type and message of an error reply.
  // Receives the parsed JSON body (null when it is not JSON) and the raw body text. For a body shaped like
  // { error: { type, message } } returns that type and message as text; for anything else returns "unknown" and the first 200
  // characters of the text, trimmed.
  // Example: errorParts({ error: { type: "x", message: "m" } }, "") -> ["x", "m"], errorParts(null, " <html> ") -> ["unknown", "<html>"]
  return ["", ""];
}

function pickRequestId(headerId: string | null, bodyId: string | null): string | null {
  // TODO 5 of 8 (finish this to pass e4): which request id the error carries.
  // Receives the id from the `request-id` header and the id from the body, either may be null. Returns the header's when there is one,
  // else the body's, else null. Example: pickRequestId("h", "b") -> "h", pickRequestId(null, "b") -> "b"
  return null;
}

function redact(message: string, apiKey: string): string {
  // TODO 6 of 8 (finish this to pass e6): keep the key out of the error.
  // Receives the message and the API key. Returns the message with every occurrence of the key replaced by [redacted].
  // Example: redact("bad key sk-1", "sk-1") -> "bad key [redacted]"
  return message;
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
  // TODO 7 of 8 (finish this to pass e3): the message of a good reply.
  // Receives the response body text. Returns the parsed JSON object. Example: parseMessage('{"stop_reason":"end_turn"}') -> { stop_reason: "end_turn" }
  return {};
}

export function sendMessages(transport: Transport, apiKey: string, model: string, messages: object[], maxTokens: number, system?: string | null): any {
  const request = buildRequest(apiKey, model, messages, maxTokens, system);
  const response = transport(request);
  if (response.status >= 200 && response.status < 300) return parseMessage(response.body);
  throw errorFrom(response, apiKey);
}

export function textOf(message: { content?: Array<{ type: string; text?: string }> }): string {
  // TODO 8 of 8 (finish this to pass e3): the text of a message.
  // Receives a message with a `content` array of blocks. Returns the `text` of the blocks whose `type` is "text", joined with nothing
  // between them; every other block type is ignored.
  // Example: textOf({ content: [{ type: "text", text: "a" }, { type: "tool_use" }, { type: "text", text: "b" }] }) -> "ab"
  return "";
}
