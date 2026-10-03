// Extract structured data from a document with validation and a bounded re-prompt. See ../../statement.md.
export type Problem = { path: string; message: string };
export type Reply = { content: Array<{ type: string; text?: string }>; stop_reason?: string };
export type Message = { role: "user" | "assistant"; content: string };
export type Ask = (messages: Message[]) => Reply;
export type Result = { status: "ok" | "failed" | "refused" | "truncated"; value: any; attempts: number; errors: Problem[] };

/** No JSON object could be read from the model's text. */
export class ParseError extends Error {}

export function parseJson(text: string): any {
  // TODO: the JSON value in a reply: a code fence's body, else the span from the first { to the last }.
  return undefined;
}

export function validate(schema: any, value: any, path = "$"): Problem[] {
  // TODO: one problem per way value breaks schema; an empty list when it conforms.
  return undefined as unknown as Problem[];
}

export function extract(ask: Ask, document: string, schema: any, maxAttempts = 3, evidenceFields: string[] = []): Result {
  // TODO: ask, parse, validate and, on a problem, re-prompt with the errors, at most maxAttempts calls.
  return undefined as unknown as Result;
}
