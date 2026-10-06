// Tool errors that an agent can act on: a structured error, bounded retries, an unknown outcome and the next action. See ../../statement.md.
import { logger } from "./logger.ts";
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
  // TODO 2 of 9 (finish this to pass e1): the refusals of make_error. Refuse with an error when the kind is not one of
  //   KINDS, and when the message, trimmed, lower-cased and without final periods, is in GENERIC. Example:
  //   make_error("transient", "Operation failed.") -> raises.
  // TODO 1 of 9 (finish this to pass m1): the structured error. Receives the kind, the message and the attempts. Build
  //   the map: is_error true, category the kind, retryable the flag KINDS holds for that kind, message trimmed, attempts.
  //   Example: make_error("transient", "Service busy, retry later") -> is_error true, retryable true.
  const error: Record<string, any> = { is_error: false, category: kind, retryable: false, message, attempts };
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
  // TODO 5 of 9 (finish this to pass e4): the empty check. Return true for none, an empty text, an empty list and an
  //   empty map, and false for anything else (0 and false are real values). Example: [] -> true, 0 -> false.
  return false;
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
    // TODO 7 of 9 (finish this to pass e5): the idempotency key. When the policy holds a key, add it to `call_args`, the
    //   copy of the arguments that this attempt sends (never to the caller's own map). Example: key "k-1" -> every attempt
    //   receives idempotency_key "k-1".
    try {
      const value = tool(callArgs);
      return { ok: true, content: value, empty: isEmpty(value), attempts };
    } catch (error: any) {
      // TODO 9 of 9 (finish this to pass e7): the unexpected exception. When the tool raises something that is not a
      //   ToolError, return make_error("internal", "unexpected failure in the tool: " + its message, attempts, a copy of
      //   the arguments) instead of letting it end the run. Example: a tool that raises ValueError("boom") -> category
      //   internal.
      if (!(error instanceof ToolError)) return makeError("transient", `unexpected failure in the tool: ${error?.message ?? error}`, null, attempts, { ...args });
      let kind = error.kind;
      // TODO 6 of 9 (finish this to pass e5): the timeout. When the kind is timeout: if the call is not safe to repeat,
      //   return make_error("outcome_unknown", the message plus "The call may have taken effect: check the current state
      //   before trying again.", attempts, a copy of the arguments); otherwise treat it as transient. Example: timeout on
      //   a write with no key -> outcome_unknown.
      if (kind === "timeout") {
        kind = "transient";
      }
      // TODO 3 of 9 (finish this to pass e2): the kinds that are not retried. When the kind is not transient, return the
      //   structured error at once (message and explanation of the tool error, the attempts so far, a copy of the
      //   arguments). Example: a validation error on the first attempt -> make_error(validation, ..., attempts=1) and no
      //   sleep.
      if (attempts > maxRetries) return makeError("transient", `${error.message} Gave up after ${attempts} attempts.`, null, attempts, { ...args });
      // TODO 4 of 9 (finish this to pass e2, e3): the wait before the next attempt, in milliseconds. Receives the tool
      //   error's retry-after value (or none), the base delay and the attempt number from 1. Sleep the value the service
      //   asked for when there is one, otherwise base times 2 to the power of attempts - 1. Example: no retry-after, base
      //   100 -> waits 100, 200, 400.
      sleep(base);
    }
  }
}

/** What the loop does next with a result. */
export function nextAction(result: Record<string, any>): string {
  // TODO 8 of 9 (finish this to pass e6): the next action of the loop. Receives a result. For an error return the action
  //   ACTIONS holds for its category; for a success return accept_empty when it is empty, otherwise continue. Example: a
  //   permission error -> escalate.
  return "continue";
}
