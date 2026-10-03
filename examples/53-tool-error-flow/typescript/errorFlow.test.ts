import { test } from "node:test";
import assert from "node:assert/strict";
import { ToolError, nextStep, run, scenarios, scripted } from "./errorFlow.ts";

test("a transient failure is retried with doubling waits until it works", () => {
  const [result, waits] = run(scripted(new ToolError("transient", "busy."), new ToolError("transient", "busy."), "ok"), {});
  assert.ok(result.content === "ok" && result.attempts === 3);
  assert.deepEqual(waits, [100, 200]);
});

test("a timeout without a key is not repeated and names the check to make", () => {
  const tool = scripted(new ToolError("timeout", "No answer."), "created");
  const [result, waits] = run(tool, {});
  assert.ok(result.kind === "outcome_unknown" && tool.state.n === 1 && waits.length === 0);
  assert.equal(nextStep(result), "check the state first");
});

test("the same key goes out with every retry", () => {
  const tool = scripted(new ToolError("timeout", "No answer."), "created");
  const [result] = run(tool, {}, { key: "k-1" });
  assert.equal(result.is_error, false);
  assert.deepEqual(tool.state.seen, ["k-1", "k-1"]);
});

test("an empty result is accepted and every scenario has a next step", () => {
  const [result] = run(scripted([]), {}, { read_only: true });
  assert.equal(nextStep(result), "accept the empty result");
  for (const [, tool, options] of scenarios()) assert.ok(nextStep(run(tool, {}, options)[0]));
});
