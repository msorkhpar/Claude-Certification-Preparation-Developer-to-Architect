// Diagnose a failure from a trace. See ../../statement.md.
export type Event = Record<string, any>;
export type Diagnosis = { index: number; type: string; origin: string; recovery: string; recovered: boolean };
import { logger } from "./logger.ts";
const log = logger("diagnose");
type Triple = [string, string, string];

const HTTP: Record<number, Triple> = {
  400: ["invalid_request", "integration", "fix_request"],
  401: ["authentication", "account", "fix_credentials"],
  402: ["billing", "account", "fix_billing"],
  403: ["permission", "account", "fix_access"],
  404: ["not_found", "integration", "fix_request"],
  409: ["conflict", "integration", "resolve_then_retry"],
  413: ["request_too_large", "integration", "shrink_request"],
  500: ["server_error", "service", "retry_backoff"],
  504: ["timeout", "service", "stream_or_batch"],
  529: ["overloaded", "service", "retry_backoff"],
};
const STOP: Record<string, Triple> = {
  max_tokens: ["truncated", "integration", "raise_max_tokens"],
  model_context_window_exceeded: ["context_exceeded", "integration", "trim_context"],
  refusal: ["refusal", "model", "fallback_model"],
  pause_turn: ["paused", "integration", "continue_turn"],
};

// TODO 1 of 7 (unlocks e1): the diagnosis of a 429.
// Receives the error event. Returns ["rate_limit", "service", "wait_retry_after"] when its `headers` have a `retry-after` key (this wins),
// else ["spend_cap", "account", "wait_for_reset"] when `error_code` is "enforced_spend_limit_reached", else ["rate_limit", "service",
// "retry_backoff"]. Example: a 429 with error_code enforced_spend_limit_reached and no headers -> the spend_cap triple
function rateLimit(event: Event): Triple {
  return ["rate_limit", "service", "retry_backoff"];
}

// TODO 6 of 7 (unlocks e1 and m1): the triple for a status.
// Receives the status number. Returns HTTP[status] for a documented status; any other status falls back by class: 500 and above use
// the 500 row, everything else the 400 row. Example: byStatus(502) -> HTTP[500], byStatus(418) -> HTTP[400]
function byStatus(status: number): Triple {
  return HTTP[400];
}

function http(event: Event): Triple {
  const status: number = event.status ?? 0;
  if (status === 429) return rateLimit(event);
  if (status === 400 && String(event.message ?? "").toLowerCase().includes("spend limit")) return ["spend_limit", "account", "raise_limit"];
  return byStatus(status);
}

// TODO 2 of 7 (unlocks e4): does the text hold a JSON object?
// Receives a text. Returns true when the span from the first `{` to the last `}` parses as JSON and is an object (not an array or a
// number); false for no braces, a broken span or another JSON type. Example: hasJsonObject('ok {"a": 1} done') -> true, hasJsonObject('see {nope}') -> false
function hasJsonObject(text: string): boolean {
  return false;
}

// TODO 3 of 7 (unlocks e3): who is to blame for an empty end turn?
// Receives the block types of the last user message, in order. Returns ["empty_response", "integration", "remove_text_after_tool_result"]
// when a `text` block comes after a `tool_result` block, else ["empty_response", "model", "add_continue_prompt"].
// Example: ["tool_result", "text"] -> integration; ["text", "tool_result"] -> model
function emptyOrigin(lastBlocks: string[]): Triple {
  return ["empty_response", "model", "add_continue_prompt"];
}

// TODO 4 of 7 (unlocks e5): is this tool event a failure, and whose?
// Receives an event and the tool names of the last request (null when unknown). Returns ["unknown_tool", "model", "return_error_result"]
// for a `tool_call` whose name is not in `tools`, ["tool_exception", "integration", "fix_tool_code"] for a `tool_result` that has an
// `exception`, and null otherwise (an `is_error` flag alone is not our failure).
// Example: { kind: "tool_call", name: "get_wether" } with tools ["get_weather"] -> the unknown_tool triple
function toolFailure(event: Event, tools: string[] | null): Triple | null {
  return null;
}

// TODO 5 of 7 (unlocks e6): did a later response recover from the failure at index i?
// Receives the trace and the index of the failure. Returns true when a later event is a response with status 200, stop_reason
// "end_turn" and a non-empty `content`; false otherwise. Example: a failure at 1 and a good end_turn at 5 -> true
function recoveredAfter(trace: Event[], i: number): boolean {
  return false;
}

// TODO 7 of 7 (unlocks e2): the triple for a response that succeeded but stopped for a bad reason.
// Receives the stop_reason. Returns STOP[reason] when the reason is in the STOP table, else null (end_turn, stop_sequence and tool_use are fine).
// Example: stopFailure("refusal") -> ["refusal", "model", "fallback_model"], stopFailure("end_turn") -> null
function stopFailure(reason: string): Triple | null {
  return null;
}

function classify(event: Event, tools: string[] | null, lastBlocks: string[]): Triple | null {
  const kind = event.kind;
  if (kind === "error") return http(event);
  if (kind === "network_error") return ["network", "service", "retry_backoff"];
  if (kind === "response" && event.status === 200) {
    const reason: string = event.stop_reason;
    const stopped = stopFailure(reason);
    if (stopped) return stopped;
    if (reason === "end_turn" && !(event.content && event.content.length)) return emptyOrigin(lastBlocks);
  }
  const failed = toolFailure(event, tools);
  if (failed) return failed;
  if (kind === "parse" && event.ok === false) {
    return hasJsonObject(String(event.text ?? "")) ? ["parse_failure", "integration", "extract_json"] : ["parse_failure", "model", "validate_and_retry"];
  }
  return null;
}

/** The first failure in the trace: its index, type, origin, recovery, and whether a later response recovered. */
export function diagnose(trace: Event[]): Diagnosis {
  log.debug("diagnose input", trace);
  let tools: string[] | null = null;
  let lastBlocks: string[] = [];
  for (let i = 0; i < trace.length; i++) {
    const event = trace[i];
    if (event.kind === "request") {
      tools = event.tools ?? null;
      lastBlocks = event.last_user_blocks ?? [];
      continue;
    }
    const found = classify(event, tools, lastBlocks);
    if (found) {
      const recovered = recoveredAfter(trace, i);
      return { index: i, type: found[0], origin: found[1], recovery: found[2], recovered };
    }
  }
  return { index: -1, type: "ok", origin: "none", recovery: "none", recovered: false };
}
