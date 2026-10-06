// Reading a trace: where did it fail, in the integration or in the model, and what should happen next?
//
// The Claude documentation on API errors and on stop reasons (read on 2026-10-03) lists the error types and says that a stop reason is part of
// a successful response ("Response contains valid content") while an error is a 4xx or 5xx status. It also says that adding text right after
// a tool result can make Claude end its turn with an empty reply. This file reads three hand-written traces, each a list of events, and names
// the first failure, its origin and the next action. The traces are scripted and carry no live output.
import { logger } from "./logger.ts";
const log = logger("read_a_trace");

type Event = Record<string, any>;

const ORIGIN: Record<string, string> = {
  invalid_request_error: "integration", authentication_error: "account", rate_limit_error: "service", api_error: "service",
  overloaded_error: "service", timeout_error: "service",
};
const NEXT: Record<string, string> = {
  invalid_request_error: "fix the request, do not retry", authentication_error: "fix the credential", rate_limit_error: "wait, then retry",
  api_error: "retry with back-off", overloaded_error: "retry with back-off", timeout_error: "stream the request",
};

export const TRACES: Record<string, Event[]> = {
  "A: a tool loop that ends in silence": [
    { kind: "request", last_user_blocks: ["text"] },
    { kind: "response", status: 200, stop_reason: "tool_use", content: [{ type: "tool_use" }] },
    { kind: "request", last_user_blocks: ["tool_result", "text"] },
    { kind: "response", status: 200, stop_reason: "end_turn", content: [] },
  ],
  "B: a busy service and a retry": [
    { kind: "request", last_user_blocks: ["text"] },
    { kind: "error", status: 529, error_type: "overloaded_error" },
    { kind: "request", last_user_blocks: ["text"] },
    { kind: "response", status: 200, stop_reason: "end_turn", content: [{ type: "text" }] },
  ],
  "C: JSON in a code fence": [
    { kind: "request", last_user_blocks: ["text"] },
    { kind: "response", status: 200, stop_reason: "end_turn", content: [{ type: "text" }] },
    { kind: "parse", ok: false, text: '```json\n{"label": "spam"}\n```' },
  ],
};

/** [index, what, origin, next action] of the first failing event, or null. */
export function firstFailure(trace: Event[]): [number, string, string, string] | null {
  let lastBlocks: string[] = [];
  for (let i = 0; i < trace.length; i++) {
    const e = trace[i];
    if (e.kind === "request") {
      lastBlocks = e.last_user_blocks;
    } else if (e.kind === "error") {
      return [i, e.error_type, ORIGIN[e.error_type], NEXT[e.error_type]];
    } else if (e.kind === "response" && e.stop_reason === "end_turn" && e.content.length === 0) {
      const at = lastBlocks.indexOf("tool_result");
      if (at >= 0 && lastBlocks.slice(at).includes("text")) return [i, "empty reply", "integration", "send the tool result alone, with no text after it"];
      return [i, "empty reply", "model", "add a new user message that asks it to continue"];
    } else if (e.kind === "parse" && !e.ok) {
      const start = e.text.indexOf("{");
      const end = e.text.lastIndexOf("}");
      try {
        JSON.parse(e.text.slice(start, end + 1));
        return [i, "parse failure", "integration", "extract the JSON object before parsing"];
      } catch {
        return [i, "parse failure", "model", "validate the output and retry"];
      }
    }
  }
  return null;
}

function main() {
  for (const [name, trace] of Object.entries(TRACES)) {
    const found = firstFailure(trace)!;
    const recovered = trace.slice(found[0] + 1).some((e) => e.kind === "response" && e.stop_reason === "end_turn" && e.content.length > 0);
    console.log(name);
    console.log(`  first failure: event ${found[0]}, ${found[1]}; origin: ${found[2]}; next: ${found[3]}; recovered later: ${recovered ? "True" : "False"}`);
  }
}

if (import.meta.main) main();
