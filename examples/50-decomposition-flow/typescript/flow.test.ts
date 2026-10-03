import { test } from "node:test";
import assert from "node:assert/strict";
import { CHANGE, SUMMARIES, plan, runAdaptive, work } from "./flow.ts";

test("the adaptive run ends when the planner says done and keeps every step", () => {
  const [status, steps, summary] = runAdaptive("goal");
  assert.equal(status, "done");
  assert.deepEqual(steps.map(([s]) => s), ["list the test files", "run the failing test", "read the module under test"]);
  assert.equal(summary, "the failure is in parse()");
});

test("the step limit ends a run that the planner does not", () => {
  const [status, steps] = runAdaptive("goal", 2);
  assert.ok(status === "step_limit" && steps.length === 2);
});

test("the planner choice depends on the last result", () => {
  assert.equal(plan("g", [["list the test files", "3 files"], ["run the failing test", "all passed"]]).done, true);
  assert.equal(plan("g", [["list the test files", "3 files"], ["run the failing test", "1 failure in test_parse"]]).next, "read the module under test");
});

test("every file has a summary and a worker answer exists for each subtask", () => {
  assert.deepEqual(Object.keys(SUMMARIES).sort(), Object.keys(CHANGE).sort());
  assert.equal(work("list the test files"), "3 files");
});
