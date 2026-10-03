// Tool errors that an agent can act on: a structured error, bounded retries, an unknown outcome and the next action. See ../../statement.md.

/** What a tool throws. `kind` is transient, validation, permission, business or timeout (no answer was received, so the effect is unknown). */
export class ToolError extends Error {
  kind: string;
  retryAfterMs: number | null;
  explanation: string | null;
  constructor(kind: string, message: string, retryAfterMs: number | null = null, explanation: string | null = null) {
    super(message);
    this.kind = kind;
    this.retryAfterMs = retryAfterMs;
    this.explanation = explanation;
  }
}

export function makeError(kind: string, message: string, explanation: string | null = null, attempts = 1, attempted: Record<string, unknown> | null = null): any {
  // TODO: the structured result of a failed call.
  return null;
}

export function toToolResult(toolUseId: string, result: Record<string, any>): any {
  // TODO: the tool_result block for the API.
  return null;
}

export function runTool(tool: (args: Record<string, any>) => any, args: Record<string, any>, policy: Record<string, any>, sleep: (ms: number) => void): any {
  // TODO: call the tool, retry only what is safe to retry, return a result or a structured error.
  return null;
}

export function nextAction(result: Record<string, any>): any {
  // TODO: what the loop does next with a result.
  return null;
}
