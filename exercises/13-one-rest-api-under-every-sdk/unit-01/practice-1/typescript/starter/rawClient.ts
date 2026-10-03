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

export function buildRequest(apiKey: string, model: string, messages: object[], maxTokens: number, system?: string | null): Request {
  // TODO: method, url, the three headers and the JSON body, as the statement says.
  return { method: "GET", url: "", headers: {}, body: "{}" };
}

export function sendMessages(transport: Transport, apiKey: string, model: string, messages: object[], maxTokens: number, system?: string | null): any {
  // TODO: build the request, call transport(request), return the parsed message or throw ApiError.
  return {};
}

export function textOf(message: { content?: Array<{ type: string; text?: string }> }): string {
  // TODO: the text of the text blocks, joined.
  return "";
}
