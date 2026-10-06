import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const solution = await import(pathToFileURL(resolve(dir, "escalation.ts")).href);
const got = (fn: (...args: any[]) => any) => (...args: any[]) => {
  const value = fn(...args);
  assert.ok(value !== null && value !== undefined, `${fn.name} returned nothing`);
  return value;
};
const [clarifyingFields, decide, handoffText] = [solution.clarifyingFields, solution.decide, solution.handoffText].map(got);

const action = (c: object, maxAttempts?: number) => decide(c, maxAttempts).action;

test("m1 a customer who asks for a person is escalated at once even when the agent could resolve it", () => {
  const result = decide({ asked_for_person: true, matches: 1, policy_covers: true, attempts_without_progress: 0, sentiment: "calm", confidence: 95 });
  assert.deepEqual(result, { action: "escalate", reason: "customer asked for a person", acknowledge: false });
});

test("e1 frustration alone does not escalate and the reply acknowledges it", () => {
  assert.deepEqual(decide({ sentiment: "frustrated" }), { action: "resolve", reason: "within capability", acknowledge: true });
  assert.equal(decide({ sentiment: "calm" }).acknowledge, false);
});

test("e2 a request the policy does not cover is escalated and a covered one is resolved", () => {
  assert.deepEqual(decide({ policy_covers: false }), { action: "escalate", reason: "policy does not cover the request", acknowledge: false });
  assert.equal(action({ policy_covers: true }), "resolve");
});

test("e3 several matching records need a clarifying question and never a guess", () => {
  assert.deepEqual(decide({ matches: 3 }), { action: "clarify", reason: "ambiguous customer match", acknowledge: false });
  assert.ok(action({ matches: 1 }) === "resolve" && action({ matches: 0 }) === "resolve");
});

test("e4 an explicit request for a person outranks an ambiguous match", () => {
  assert.equal(action({ asked_for_person: true, matches: 4 }), "escalate");
  assert.equal(action({ asked_for_person: true, policy_covers: false }), "escalate");
});

test("e5 no progress after the attempt limit escalates and below it does not", () => {
  assert.deepEqual(decide({ attempts_without_progress: 2 }), { action: "escalate", reason: "no progress", acknowledge: false });
  assert.equal(action({ attempts_without_progress: 1 }), "resolve");
  assert.equal(action({ attempts_without_progress: 3 }, 4), "resolve");
});

test("e6 sentiment and confidence scores never change the decision", () => {
  for (const sentiment of ["calm", "frustrated", "angry"]) for (const confidence of [5, 50, 99]) assert.equal(action({ sentiment, confidence }), "resolve");
  assert.equal(action({ sentiment: "angry", confidence: 1, matches: 2 }), "clarify");
});

test("e7 the clarifying question names only the fields that tell the matches apart", () => {
  const matches = [{ id: "c1", name: "Ana Ruiz", email: "ana@example.com", zip: "10115" }, { id: "c2", name: "Ana Ruiz", email: "ana.r@example.com", zip: "10115" }];
  assert.deepEqual(clarifyingFields(matches), ["email"]);
  assert.deepEqual(clarifyingFields([{ id: "c1", name: "Ana", zip: "1" }, { id: "c2", name: "Bo", zip: "2" }, { id: "c3", name: "Ana", zip: "3" }]), ["name", "zip"]);
  assert.deepEqual(clarifyingFields([{ id: "c1", name: "Ana" }]), []);
});

test("e8 the hand off carries the structured facts and no transcript and refuses a case without an id", () => {
  const c = { customer_id: "C-77", issue: "refund over the limit", root_cause: "duplicate charge", amount: "$129.50", actions: ["verified identity", "checked order"], recommended: "approve the refund", transcript: "user: hello ... 40 turns ..." };
  assert.equal(handoffText(c), "Customer: C-77\nIssue: refund over the limit\nRoot cause: duplicate charge\nAmount: $129.50\nActions taken: verified identity; checked order\nRecommended action: approve the refund");
  assert.ok(!handoffText(c).includes("40 turns"));
  assert.equal(handoffText({ customer_id: "C-1", issue: "late parcel" }), "Customer: C-1\nIssue: late parcel\nRoot cause: unknown\nAmount: unknown\nActions taken: none\nRecommended action: review the case");
  assert.throws(() => handoffText({ issue: "late parcel" }));
});
