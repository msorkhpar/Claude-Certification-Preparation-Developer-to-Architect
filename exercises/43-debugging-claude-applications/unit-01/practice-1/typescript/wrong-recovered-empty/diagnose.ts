// Diagnose a failure from a trace. See ../../statement.md.
export type Event = Record<string, any>;
export type Diagnosis = { index: number; type: string; origin: string; recovery: string; recovered: boolean };
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

function http(event: Event): Triple {
  const status: number = event.status ?? 0;
  if (status === 429) {
    if (event.headers && "retry-after" in event.headers) return ["rate_limit", "service", "wait_retry_after"];
    if (event.error_code === "enforced_spend_limit_reached") return ["spend_cap", "account", "wait_for_reset"];
    return ["rate_limit", "service", "retry_backoff"];
  }
  if (status === 400 && String(event.message ?? "").toLowerCase().includes("spend limit")) return ["spend_limit", "account", "raise_limit"];
  if (status in HTTP) return HTTP[status];
  return status >= 500 ? HTTP[500] : HTTP[400];
}

function hasJsonObject(text: string): boolean {
  const start = text.indexOf("{");
  const end = text.lastIndexOf("}");
  if (start < 0 || end < start) return false;
  try {
    const value = JSON.parse(text.slice(start, end + 1));
    return typeof value === "object" && value !== null && !Array.isArray(value);
  } catch {
    return false;
  }
}

function classify(event: Event, tools: string[] | null, lastBlocks: string[]): Triple | null {
  const kind = event.kind;
  if (kind === "error") return http(event);
  if (kind === "network_error") return ["network", "service", "retry_backoff"];
  if (kind === "response" && event.status === 200) {
    const reason: string = event.stop_reason;
    if (reason in STOP) return STOP[reason];
    if (reason === "end_turn" && !(event.content && event.content.length)) {
      const at = lastBlocks.indexOf("tool_result");
      if (at >= 0 && lastBlocks.slice(at).includes("text")) return ["empty_response", "integration", "remove_text_after_tool_result"];
      return ["empty_response", "model", "add_continue_prompt"];
    }
  }
  if (kind === "tool_call" && tools !== null && !tools.includes(event.name)) return ["unknown_tool", "model", "return_error_result"];
  if (kind === "tool_result" && event.exception) return ["tool_exception", "integration", "fix_tool_code"];
  if (kind === "parse" && event.ok === false) {
    return hasJsonObject(String(event.text ?? "")) ? ["parse_failure", "integration", "extract_json"] : ["parse_failure", "model", "validate_and_retry"];
  }
  return null;
}

/** The first failure in the trace: its index, type, origin, recovery, and whether a later response recovered. */
export function diagnose(trace: Event[]): Diagnosis {
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
      const recovered = trace.slice(i + 1).some((e) => e.kind === "response" && e.status === 200 && e.stop_reason === "end_turn");
      return { index: i, type: found[0], origin: found[1], recovery: found[2], recovered };
    }
  }
  return { index: -1, type: "ok", origin: "none", recovery: "none", recovered: false };
}
