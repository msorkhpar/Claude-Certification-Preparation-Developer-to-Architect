import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { chooseApi, mergePasses, resubmissionPlan, reviewPlan, submissionInterval } = await import(pathToFileURL(resolve(dir, "batchReview.ts")).href);

const got = (v: any) => {
  assert.ok(v !== null && v !== undefined, "the function returned nothing");
  return v;
};
const finding = (over: Record<string, unknown> = {}): any => ({ file: "a.py", line: 10, severity: "medium", issue: "unchecked input", confidence: 90, ...over });

test("m1 the interval between submissions leaves room for the window and the handling", () => {
  assert.equal(submissionInterval(30), 4);
  assert.doesNotThrow(() => submissionInterval(27));
  assert.equal(submissionInterval(27), 1);
  assert.equal(submissionInterval(48, 24, 4), 20);
});

test("e1 an sla without room for a batch is refused", () => {
  assert.throws(() => submissionInterval(26));
  assert.throws(() => submissionInterval(20));
});

test("e2 a blocking check or a tool loop needs the synchronous api", () => {
  assert.equal(chooseApi(true), "synchronous");
  assert.equal(chooseApi(false), "batch");
  assert.equal(chooseApi(false, true), "synchronous");
});

test("e3 only the items that did not succeed are resubmitted by custom id", () => {
  const results = [["a1", "succeeded"], ["a2", "expired"], ["a3", "succeeded"], ["a4", "canceled"], ["a5", "server_error"]];
  assert.deepEqual(resubmissionPlan(results, {}, 1000), [["a2", "resubmit"], ["a4", "resubmit"], ["a5", "resubmit"]]);
  assert.deepEqual(resubmissionPlan([["a1", "succeeded"]], {}, 1000), []);
});

test("e4 an item over the limit is chunked and a rejected request is fixed first", () => {
  const results = [["big", "invalid_request"], ["bad", "invalid_request"], ["late", "expired"], ["bigexp", "expired"]];
  const sizes = { big: 5000, bad: 100, late: 100, bigexp: 5000 };
  assert.deepEqual(resubmissionPlan(results, sizes, 1000), [["big", "chunk"], ["bad", "fix"], ["late", "resubmit"], ["bigexp", "chunk"]]);
  assert.deepEqual(resubmissionPlan([["exact", "expired"]], { exact: 1000 }, 1000), [["exact", "resubmit"]]);
});

test("e5 a multi file review gets a local pass per file and one integration pass", () => {
  const plan = got(reviewPlan(["a.py", "b.py", "c.py"]));
  assert.deepEqual(plan.map((p: any) => p.name), ["local:a.py", "local:b.py", "local:c.py", "integration"]);
  assert.deepEqual(plan[0].files, ["a.py"]);
  assert.deepEqual(plan[3].files, ["a.py", "b.py", "c.py"]);
  assert.deepEqual(got(reviewPlan(["a.py"])).map((p: any) => p.name), ["local:a.py"]);
});

test("e6 the same finding from two passes is one finding with the highest severity and the lowest confidence", () => {
  const merged = got(mergePasses([[finding({ severity: "low", confidence: 95 })], [finding({ severity: "high", confidence: 85 }), finding({ file: "b.py", line: 3, issue: "race" })]]));
  assert.equal(merged.length, 2);
  const first = merged[0];
  assert.deepEqual([first.file, first.line, first.severity, first.passes, first.confidence], ["a.py", 10, "high", 2, 85]);
  assert.equal(merged[1].passes, 1);
});

test("e7 a finding is accepted only when two independent passes agree with confidence", () => {
  const twiceSamePass = got(mergePasses([[finding(), finding()], []]));
  assert.ok(twiceSamePass[0].passes === 1 && twiceSamePass[0].route === "verify");
  assert.equal(got(mergePasses([[finding({ confidence: 99 })]]))[0].route, "verify");
  assert.equal(got(mergePasses([[finding({ confidence: 60 })], [finding({ confidence: 90 })]]))[0].route, "verify");
  assert.equal(got(mergePasses([[finding({ confidence: 80 })], [finding({ confidence: 90 })]]))[0].route, "accept");
});
