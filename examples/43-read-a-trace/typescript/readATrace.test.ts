import { test } from "node:test";
import assert from "node:assert/strict";
import { TRACES, firstFailure } from "./readATrace.ts";

test("an empty reply after text following a tool result is our message structure", () => {
  const [index, what, origin] = firstFailure(TRACES["A: a tool loop that ends in silence"])!;
  assert.deepEqual([index, what, origin], [3, "empty reply", "integration"]);
});

test("the same empty reply without that text is the model", () => {
  const trace = [{ kind: "request", last_user_blocks: ["tool_result"] }, { kind: "response", status: 200, stop_reason: "end_turn", content: [] }];
  assert.deepEqual(firstFailure(trace)!.slice(0, 3), [1, "empty reply", "model"]);
});

test("a 529 is the service and the next action is a retry", () => {
  assert.deepEqual(firstFailure(TRACES["B: a busy service and a retry"]), [1, "overloaded_error", "service", "retry with back-off"]);
});

test("json inside a code fence is the parser and prose without json is the model", () => {
  assert.deepEqual(firstFailure(TRACES["C: JSON in a code fence"])!.slice(0, 3), [2, "parse failure", "integration"]);
  assert.deepEqual(firstFailure([{ kind: "parse", ok: false, text: "I am not sure" }])!.slice(0, 3), [0, "parse failure", "model"]);
});

test("a clean trace has no failure", () => {
  assert.equal(firstFailure([{ kind: "request", last_user_blocks: ["text"] }, { kind: "response", status: 200, stop_reason: "end_turn", content: [{ type: "text" }] }]), null);
});
