import { test } from "node:test";
import assert from "node:assert/strict";
import { GOOD_CRITERION, VAGUE_CRITERION, lintCriterion, lintExamples, trust } from "./criteriaLint.ts";

test("a criterion that names no pattern is linted and a concrete one is not", () => {
  assert.deepEqual(lintCriterion(VAGUE_CRITERION), ["vague-report", "no-skip", "no-high-example", "no-low-example"]);
  assert.deepEqual(lintCriterion(GOOD_CRITERION), []);
  assert.deepEqual(lintCriterion({ ...GOOD_CRITERION, skip: "Use your judgment." }), ["vague-skip"]);
});

test("a set of examples needs two to four both verdicts and a reason each", () => {
  const report = { verdict: "report", reason: "r" };
  const skip = { verdict: "skip", reason: "s" };
  assert.deepEqual(lintExamples([report, skip]), []);
  assert.deepEqual(lintExamples([report]), ["two-to-four", "both-verdicts"]);
  assert.deepEqual(lintExamples([report, skip, report, skip, report]), ["two-to-four"]);
  assert.deepEqual(lintExamples([report, { verdict: "report", reason: "" }]), ["both-verdicts", "reason-missing"]);
});

test("a category is switched off only with enough reviews and a low share accepted", () => {
  const verdicts: Array<[string, string]> = [...Array(9).fill(["bug", "accepted"]), ["bug", "dismissed"], ...Array(2).fill(["style", "accepted"]), ...Array(6).fill(["style", "dismissed"]), ...Array(3).fill(["naming", "dismissed"])];
  const table = trust(verdicts);
  assert.deepEqual(table.bug, { reviewed: 10, accepted: 9, precision: 0.9, off: false });
  assert.deepEqual(table.style, { reviewed: 8, accepted: 2, precision: 0.25, off: true });
  assert.ok(table.naming.off === false && table.naming.precision === 0);
});
