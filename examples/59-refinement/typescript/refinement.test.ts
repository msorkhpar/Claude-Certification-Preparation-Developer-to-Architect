import { test } from "node:test";
import assert from "node:assert/strict";
import { chooseMode, failureReport, groupFeedback } from "./refinement.ts";

const task = (over = {}) => ({ diff_in_one_sentence: false, files: 1, architectural: false, approaches: 1, ...over });

test("a change you can say in one sentence is done directly", () => {
  assert.deepEqual(chooseMode(task({ diff_in_one_sentence: true })), ["implement"]);
});

test("large architectural or ambiguous changes are planned first", () => {
  assert.deepEqual(chooseMode(task({ architectural: true })), ["explore", "plan", "implement"]);
  assert.deepEqual(chooseMode(task({ files: 45 })), ["explore", "plan", "implement"]);
  assert.deepEqual(chooseMode(task({ approaches: 2 })), ["explore", "plan", "implement"]);
  assert.deepEqual(chooseMode(task({ diff_in_one_sentence: true, architectural: true })), ["explore", "plan", "implement"]);
});

test("interacting problems travel together and independent ones go one at a time", () => {
  const issues = [{ id: "a", interacts_with: ["b"] }, { id: "b", interacts_with: [] }, { id: "c", interacts_with: [] }, { id: "d", interacts_with: ["c"] }, { id: "e" }];
  assert.deepEqual(groupFeedback(issues), [["a", "b"], ["c", "d"], ["e"]]);
  assert.deepEqual(groupFeedback([]), []);
});

test("a failure report names each failing test with input and expected output", () => {
  const results = [{ name: "ok", input: 1, expected: 2, actual: 2 }, { name: "empty", input: [], expected: [], actual: null }];
  assert.equal(failureReport(results), "1 of 2 tests fail:\n- empty: input [], expected [], got null");
  assert.equal(failureReport(results.slice(0, 1)), "All tests pass.");
});
