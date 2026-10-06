import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const solution = await import(pathToFileURL(resolve(dir, "architectureReview.ts")).href);

const review = (design: unknown) => {
  const result = solution.review(design);
  assert.ok(Array.isArray(result), "review returned nothing");
  return result as Array<{ rule: string; severity: string }>;
};
const verdict = (findings: unknown) => {
  const result = solution.verdict(findings);
  assert.ok(typeof result === "string", "verdict returned nothing");
  return result as string;
};

const design = (over: Record<string, unknown> = {}) => ({
  name: "intake", pattern: "workflow", agents: 1, cost: 3, path_known: true, parallel_independent: false, shared_context: false, needs_audit: true, writes_without_approval: false,
  stages: { input: ["parse"], processing: ["classify", "route"], output: ["validate", "send"], feedback: ["review a sample"] }, ...over,
});
const stages = (over: Record<string, unknown> = {}) => ({ ...design().stages, ...over });
const rules = (d: unknown) => review(d).map((f) => f.rule);

test("m1 a sound design passes review with no findings", () => {
  assert.deepEqual(review(design()), []);
  assert.equal(verdict([]), "approve");
});

test("e1 a design without a feedback loop or a stage is rejected", () => {
  assert.deepEqual(review(design({ stages: stages({ feedback: [] }) })), [{ rule: "no-feedback", severity: "high" }]);
  const { feedback, ...withoutFeedback } = stages();
  assert.deepEqual(rules(design({ stages: withoutFeedback })), ["no-feedback"]);
  for (const stage of ["input", "processing"]) assert.deepEqual(rules(design({ stages: stages({ [stage]: [] }) })), [`missing-stage:${stage}`]);
  assert.deepEqual(rules(design({ stages: stages({ output: [] }) })), ["missing-stage:output"]);
});

test("e2 autonomy is flagged only when the path is known", () => {
  assert.deepEqual(review(design({ pattern: "agent" })), [{ rule: "autonomy-without-need", severity: "medium" }]);
  assert.deepEqual(rules(design({ pattern: "multi-agent", agents: 1 })), ["autonomy-without-need"]);
  assert.deepEqual(rules(design({ pattern: "agent", path_known: false })), []);
  assert.deepEqual(rules(design({ pattern: "augmented" })), []);
});

test("e3 several agents need independent parts and no shared context", () => {
  assert.deepEqual(review(design({ pattern: "multi-agent", path_known: false, agents: 3, shared_context: true, parallel_independent: true })), [{ rule: "team-without-independence", severity: "high" }]);
  assert.deepEqual(rules(design({ pattern: "multi-agent", path_known: false, agents: 3, parallel_independent: false })), ["team-without-independence"]);
  assert.deepEqual(rules(design({ pattern: "multi-agent", path_known: false, agents: 3, parallel_independent: true })), []);
  assert.deepEqual(rules(design({ pattern: "agent", path_known: false, agents: 1, shared_context: true })), []);
});

test("e4 an unapproved write is a finding only when an audit is needed", () => {
  assert.deepEqual(review(design({ writes_without_approval: true })), [{ rule: "unapproved-write", severity: "high" }]);
  assert.deepEqual(rules(design({ writes_without_approval: true, needs_audit: false })), []);
  assert.deepEqual(rules(design({ writes_without_approval: false })), []);
});

test("e5 output that nobody validates is flagged", () => {
  assert.deepEqual(review(design({ stages: stages({ output: ["send"] }) })), [{ rule: "unvalidated-output", severity: "medium" }]);
  assert.deepEqual(rules(design({ stages: stages({ output: ["validate"] }) })), []);
});

test("e6 findings are ordered by severity then rule and the verdict follows the worst", () => {
  const messy = design({ pattern: "agent", writes_without_approval: true, stages: { input: ["parse"], processing: ["act"], output: ["send"], feedback: [] } });
  assert.deepEqual(review(messy).map((f) => [f.severity, f.rule]), [["high", "no-feedback"], ["high", "unapproved-write"], ["medium", "autonomy-without-need"], ["medium", "unvalidated-output"]]);
  assert.equal(verdict(review(messy)), "reject");
  assert.equal(verdict(review(design({ pattern: "agent" }))), "revise");
  assert.equal(verdict([{ rule: "x", severity: "medium" }, { rule: "y", severity: "high" }]), "reject");
  assert.equal(verdict([{ rule: "x", severity: "medium" }]), "revise");
});

test("e7 the cheapest design that is not rejected wins and ties go by name", () => {
  const pick = (designs: unknown[]) => {
    const result = solution.cheapestAdequate(designs);
    assert.ok(result === null || typeof result === "string", "cheapestAdequate returned something that is not a name");
    return result;
  };
  const cheapButRejected = design({ name: "a-cheap", cost: 1, stages: stages({ feedback: [] }) });
  const revise = design({ name: "b-revise", cost: 2, pattern: "agent" });
  const sound = design({ name: "c-sound", cost: 5 });
  assert.equal(pick([cheapButRejected, sound, revise]), "b-revise");
  assert.equal(pick([design({ name: "zeta", cost: 2 }), design({ name: "alpha", cost: 2 }), design({ name: "mid", cost: 4 })]), "alpha");
  assert.equal(pick([cheapButRejected]), null);
  assert.equal(pick([]), null);
});
