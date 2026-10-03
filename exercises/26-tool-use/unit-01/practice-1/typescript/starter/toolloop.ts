// A tool loop against a scripted model. See ../../statement.md.
export type Block = Record<string, any>;
export type Tool = { name: string; description: string; input_schema: Record<string, any>; handler: (input: Record<string, any>) => unknown };
export type Reply = { content: Block[]; stop_reason: string };
export type Ask = (request: Record<string, any>) => Reply;
export type Outcome = { status: "done" | "refused" | "truncated" | "max_turns"; text: string; turns: number; messages: any[] };

/** A tool refuses or fails; its message goes back to the model as an error result. */
export class ToolError extends Error {}

/** The request would be rejected with a 400. `field` names the offending part. */
export class RequestError extends Error {
  field: string;
  reason: string;
  constructor(field: string, reason: string) {
    super(`${field}: ${reason}`);
    this.field = field;
    this.reason = reason;
  }
}

export function runAgent(ask: Ask, tools: Tool[], userText: string, model = "claude-sonnet-5-5", maxTurns = 8, toolChoice?: Record<string, any>): Outcome {
  // TODO: call the model, run every tool it asks for, send the results back, until it ends its turn or a limit is hit.
  return undefined as unknown as Outcome;
}
