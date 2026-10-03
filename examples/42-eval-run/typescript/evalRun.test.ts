import { test } from "node:test";
import assert from "node:assert/strict";
import { CASES, CRITERIA, PROMPT_V1, PROMPT_V2, compare, gate, grade, run } from "./evalRun.ts";

test("exact grading ignores case and spacing only", () => {
  assert.ok(grade({ expect: "neutral" }, " Neutral\n"));
  assert.ok(!grade({ expect: "neutral" }, "neutral."));
  assert.ok(!grade({ expect: "neutral" }, "positive"));
});

test("the first prompt passes four of six and fails the gate twice", () => {
  const report = run(CASES, PROMPT_V1);
  assert.deepEqual(report.results.filter((r) => !r.passed).map((r) => r.id), ["sarcasm-1", "mixed-1"]);
  assert.equal(report.pass_rate.toFixed(3), "0.667");
  assert.deepEqual(report.by_tag.edge, [1, 3]);
  assert.deepEqual(gate(report, CRITERIA), ["overall", "tag:edge"]);
});

test("the second prompt has a better average and one regression", () => {
  const v1 = run(CASES, PROMPT_V1);
  const v2 = run(CASES, PROMPT_V2);
  assert.ok(v2.pass_rate > v1.pass_rate);
  assert.deepEqual(compare(v1, v2), { regressions: ["empty-1"], fixed: ["sarcasm-1", "mixed-1"] });
  assert.deepEqual(gate(v2, CRITERIA), ["tag:edge"]);
});

test("a run that changes nothing has no regressions", () => {
  const v1 = run(CASES, PROMPT_V1);
  assert.deepEqual(compare(v1, v1), { regressions: [], fixed: [] });
});
