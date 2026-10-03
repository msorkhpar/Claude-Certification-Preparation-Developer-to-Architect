import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { diagnose } = await import(pathToFileURL(resolve(dir, "diagnose.ts")).href);

const REQ = { kind: "request", model: "claude-sonnet-5-5", max_tokens: 1024, tools: ["get_weather"], last_user_blocks: ["text"] };
const one = (...events: Record<string, unknown>[]) => {
  const r = diagnose([REQ, ...events]) ?? {};
  return `${r.type}|${r.origin}|${r.recovery}`;
};
const error = (status: number, extra: Record<string, unknown> = {}) => ({ kind: "error", status, error_type: "x_error", message: "m", ...extra });
const reply = (stop_reason: string, content: unknown[] = [{ type: "text", text: "hi" }]) => ({ kind: "response", status: 200, stop_reason, content });

test("m1 each documented http error maps to a type an origin and a recovery", () => {
  assert.equal(one(error(400)), "invalid_request|integration|fix_request");
  assert.equal(one(error(401)), "authentication|account|fix_credentials");
  assert.equal(one(error(402)), "billing|account|fix_billing");
  assert.equal(one(error(403)), "permission|account|fix_access");
  assert.equal(one(error(404)), "not_found|integration|fix_request");
  assert.equal(one(error(409)), "conflict|integration|resolve_then_retry");
  assert.equal(one(error(413)), "request_too_large|integration|shrink_request");
  assert.equal(one(error(500)), "server_error|service|retry_backoff");
  assert.equal(one(error(504)), "timeout|service|stream_or_batch");
  assert.equal(one(error(529)), "overloaded|service|retry_backoff");
});

test("e1 a 429 is a rate limit or a spend cap and other statuses fall back by class", () => {
  assert.equal(one(error(429, { headers: { "retry-after": "12" } })), "rate_limit|service|wait_retry_after");
  assert.equal(one(error(429, { error_code: "enforced_spend_limit_reached" })), "spend_cap|account|wait_for_reset");
  assert.equal(one(error(429, { headers: { "retry-after": "3" }, error_code: "enforced_spend_limit_reached" })), "rate_limit|service|wait_retry_after");
  assert.equal(one(error(429)), "rate_limit|service|retry_backoff");
  assert.equal(one(error(400, { message: "You have reached your workspace Spend Limit" })), "spend_limit|account|raise_limit");
  assert.equal(one(error(418)), "invalid_request|integration|fix_request");
  assert.equal(one(error(502)), "server_error|service|retry_backoff");
});

test("e2 a successful response can still fail by its stop reason", () => {
  assert.equal(one(reply("max_tokens")), "truncated|integration|raise_max_tokens");
  assert.equal(one(reply("model_context_window_exceeded")), "context_exceeded|integration|trim_context");
  assert.equal(one(reply("refusal")), "refusal|model|fallback_model");
  assert.equal(one(reply("pause_turn")), "paused|integration|continue_turn");
  for (const fine of ["end_turn", "stop_sequence", "tool_use"]) assert.equal(one(reply(fine)), "ok|none|none", fine);
});

test("e3 an empty end turn is the integration when text followed the tool result and the model otherwise", () => {
  const label = (blocks: string[]) => {
    const r = diagnose([{ ...REQ, last_user_blocks: blocks }, reply("end_turn", [])]) ?? {};
    return `${r.index}|${r.type}|${r.origin}|${r.recovery}`;
  };
  assert.equal(label(["tool_result", "text"]), "1|empty_response|integration|remove_text_after_tool_result");
  assert.equal(label(["tool_result"]), "1|empty_response|model|add_continue_prompt");
  assert.equal(label(["text"]), "1|empty_response|model|add_continue_prompt");
  assert.equal(label(["text", "tool_result"]), "1|empty_response|model|add_continue_prompt");
  assert.equal(label(["tool_result", "tool_result", "text"]), "1|empty_response|integration|remove_text_after_tool_result");
  assert.equal(one(reply("end_turn")), "ok|none|none");
});

test("e4 a parse failure is the integration when a json object is in the text and the model when not", () => {
  const parse = (text: string) => ({ kind: "parse", ok: false, text });
  assert.equal(one(parse('Here you go: {"label": "spam"} hope it helps')), "parse_failure|integration|extract_json");
  assert.equal(one(parse('```json\n{"a": 1}\n```')), "parse_failure|integration|extract_json");
  assert.equal(one(parse("I cannot decide")), "parse_failure|model|validate_and_retry");
  assert.equal(one(parse('{"label": "spam"')), "parse_failure|model|validate_and_retry");
  assert.equal(one(parse("see [1, 2] and {nope}")), "parse_failure|model|validate_and_retry");
  assert.equal(one({ kind: "parse", ok: true, text: '{"a": 1}' }), "ok|none|none");
});

test("e5 tool failures split into a model that called a missing tool and our tool that raised", () => {
  assert.equal(one({ kind: "tool_call", name: "get_weather", input: {} }), "ok|none|none");
  assert.equal(one({ kind: "tool_call", name: "get_wether", input: {} }), "unknown_tool|model|return_error_result");
  assert.equal(one({ kind: "tool_result", name: "get_weather", is_error: false }), "ok|none|none");
  assert.equal(one({ kind: "tool_result", name: "get_weather", is_error: true, exception: "KeyError: 'city'" }), "tool_exception|integration|fix_tool_code");
  assert.equal(one({ kind: "tool_result", name: "get_weather", is_error: true }), "ok|none|none");
  const other = diagnose([{ kind: "tool_call", name: "anything", input: {} }]) ?? {};
  assert.equal(other.type, "ok");
});

test("e6 the first failure names the cause and a later good response marks it recovered", () => {
  let r = diagnose([REQ, error(529), REQ, reply("max_tokens"), REQ, reply("end_turn")]) ?? {};
  assert.deepEqual([r.index, r.type, r.recovered], [1, "overloaded", true]);
  r = diagnose([REQ, error(529), REQ, error(529)]) ?? {};
  assert.deepEqual([r.index, r.recovered], [1, false]);
  r = diagnose([REQ, error(401), REQ, reply("end_turn", [])]) ?? {};
  assert.deepEqual([r.index, r.type, r.recovered], [1, "authentication", false]);
  const clean = { index: -1, type: "ok", origin: "none", recovery: "none", recovered: false };
  assert.deepEqual(diagnose([REQ, reply("end_turn")]), clean);
  assert.deepEqual(diagnose([]), clean);
});

test("e7 a dropped connection has no status and belongs to the service side", () => {
  let r = diagnose([REQ, reply("end_turn"), REQ, { kind: "network_error", message: "connection reset" }]) ?? {};
  assert.deepEqual([r.index, r.type, r.origin, r.recovery, r.recovered], [3, "network", "service", "retry_backoff", false]);
  r = diagnose([REQ, { kind: "network_error", message: "timed out" }, REQ, reply("end_turn")]) ?? {};
  assert.deepEqual([r.index, r.type, r.recovered], [1, "network", true]);
});
