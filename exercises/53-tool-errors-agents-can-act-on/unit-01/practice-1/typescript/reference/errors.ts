// Tool errors that an agent can act on: a structured error, bounded retries, an unknown outcome and the next action. See ../../statement.md.
import { logger } from "../logger.ts";
const log = logger("errors");

const KINDS: Record<string, boolean> = { transient: true, validation: false, permission: false, business: false, outcome_unknown: false, internal: false };
const GENERIC = new Set(["", "error", "failed", "failure", "operation failed", "something went wrong", "unknown error"]);
const ACTIONS: Record<string, string> = { transient: "retry_later", validation: "repair_input", permission: "escalate", business: "explain", outcome_unknown: "verify_first", internal: "escalate" };

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

/** The structured result of a failed call. */
export function makeError(kind: string, message: string, explanation: string | null = null, attempts = 1, attempted: Record<string, unknown> | null = null): Record<string, any> {
  if (!(kind in KINDS)) throw new Error(`unknown error kind: ${kind}`);
  if (GENERIC.has(String(message ?? "").trim().toLowerCase().replace(/\.+$/, ""))) throw new Error("an error message must say what went wrong and what to do");
  const error: Record<string, any> = { is_error: true, category: kind, retryable: KINDS[kind], message: message.trim(), attempts };
  if (explanation) error.explanation = explanation;
  if (attempted !== null) error.attempted = attempted;
  return error;
}

/** The tool_result block for the API: an error carries is_error true and its category, retry flag and message as text. */
export function toToolResult(toolUseId: string, result: Record<string, any>): Record<string, any> {
  if (result.is_error) {
    let text = `${result.category} error (retryable: ${result.retryable ? "yes" : "no"}): ${result.message}`;
    if (result.explanation) text += ` Tell the customer: ${result.explanation}`;
    return { type: "tool_result", tool_use_id: toolUseId, content: text, is_error: true };
  }
  return { type: "tool_result", tool_use_id: toolUseId, content: String(result.content ?? ""), is_error: false };
}

function isEmpty(value: unknown): boolean {
  return value === null || value === undefined || value === "" || (Array.isArray(value) && value.length === 0) || (typeof value === "object" && !Array.isArray(value) && Object.keys(value as object).length === 0);
}

/** Call a tool, recover locally from what is safe to recover from, and return a result or a structured error. */
export function runTool(tool: (args: Record<string, any>) => any, args: Record<string, any>, policy: Record<string, any>, sleep: (ms: number) => void): Record<string, any> {
  log.debug("runTool input", args);
  const key = policy.idempotency_key;
  const maxRetries = policy.max_retries ?? 2;
  const base = policy.base_delay_ms ?? 100;
  const safeToRepeat = Boolean(policy.read_only) || Boolean(key);
  let attempts = 0;
  while (true) {
    attempts++;
    const callArgs: Record<string, any> = { ...args };
    if (key) callArgs.idempotency_key = key;
    try {
      const value = tool(callArgs);
      return { ok: true, content: value, empty: isEmpty(value), attempts };
    } catch (error: any) {
      if (!(error instanceof ToolError)) return makeError("internal", `unexpected failure in the tool: ${error?.message ?? error}`, null, attempts, { ...args });
      let kind = error.kind;
      if (kind === "timeout") {
        if (!safeToRepeat) return makeError("outcome_unknown", `${error.message} The call may have taken effect: check the current state before trying again.`, null, attempts, { ...args });
        kind = "transient";
      }
      if (kind !== "transient") return makeError(kind, error.message, error.explanation, attempts, { ...args });
      if (attempts > maxRetries) return makeError("transient", `${error.message} Gave up after ${attempts} attempts.`, null, attempts, { ...args });
      sleep(error.retryAfterMs !== null ? error.retryAfterMs : base * 2 ** (attempts - 1));
    }
  }
}

/** What the loop does next with a result. */
export function nextAction(result: Record<string, any>): string {
  if (!result.is_error) return result.empty ? "accept_empty" : "continue";
  return ACTIONS[result.category];
}
