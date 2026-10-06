import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { audit } = await import(pathToFileURL(resolve(dir, "audit.ts")).href);

const step = (tool: string, ok = true, right?: string) => ({ tool, ok, ...(right ? { right_tool: right } : {}) });
const session = (steps: unknown[], outcome = "resolved", needs_human = false, refund = 0, limit = 10000) =>
  ({ id: "s", steps, outcome, needs_human, refund_cents: refund, limit_cents: limit });
const CLEAN = [step("get_customer"), step("lookup_order"), step("process_refund")];
const times = (n: number, make: () => unknown) => Array.from({ length: n }, make);

test("m1 a mixed set of sessions gets every rate and the first fix", () => {
  const sessions = [
    session(CLEAN, "resolved", false, 2000),
    session([step("lookup_order"), step("get_customer"), step("process_refund")]),
    session([step("get_customer")], "escalated", true),
    session([step("get_customer")], "escalated"),
    session(CLEAN, "resolved", true),
    session([step("get_customer", true, "lookup_order"), step("lookup_order")]),
  ];
  assert.deepEqual(audit(sessions), { sessions: 6, resolved: 4, fcr: 0.667, meets_target: false, over_escalated: 1, under_escalated: 1,
    skipped_prerequisite: 1, wrong_tool: 1, over_limit_refunds: 0, diagnosis: "enforce_in_code" });
});

test("e1 no sessions give zero rates and no diagnosis", () => {
  assert.deepEqual(audit([]), { sessions: 0, resolved: 0, fcr: 0, meets_target: false, over_escalated: 0, under_escalated: 0,
    skipped_prerequisite: 0, wrong_tool: 0, over_limit_refunds: 0, diagnosis: "none" });
});

test("e2 a protected call before a successful identity check is a skipped prerequisite", () => {
  assert.equal(audit([session([step("get_customer"), step("lookup_order")])]).skipped_prerequisite, 0);
  assert.equal(audit([session([step("lookup_order"), step("get_customer")])]).skipped_prerequisite, 1);
  assert.equal(audit([session([step("get_customer", false), step("process_refund")])]).skipped_prerequisite, 1);
  assert.equal(audit([session([step("process_refund")]), session([step("lookup_order")]), session(CLEAN)]).skipped_prerequisite, 2);
});

test("e3 a refund over the limit counts only when it was made", () => {
  const made = audit([session(CLEAN, "resolved", false, 10001)]);
  assert.equal(made.over_limit_refunds, 1);
  assert.equal(made.diagnosis, "enforce_in_code");
  assert.equal(audit([session(CLEAN, "resolved", false, 10000)]).over_limit_refunds, 0);
  const refused = audit([session(CLEAN, "escalated", true, 50000)]);
  assert.equal(refused.over_limit_refunds, 0);
  assert.equal(refused.diagnosis, "none");
});

test("e4 money first then tool descriptions then escalation criteria", () => {
  const wrongTool = session([step("get_customer", true, "lookup_order")]);
  const over = session(CLEAN, "escalated");
  assert.equal(audit([session([step("process_refund"), step("get_customer")]), wrongTool, over]).diagnosis, "enforce_in_code");
  assert.equal(audit([wrongTool, wrongTool]).diagnosis, "rewrite_tool_descriptions");
  assert.equal(audit([wrongTool, over]).diagnosis, "rewrite_tool_descriptions");
  assert.equal(audit([wrongTool, over, over]).diagnosis, "write_escalation_criteria");
  assert.equal(audit([session(CLEAN, "resolved", true)]).diagnosis, "write_escalation_criteria");
  assert.equal(audit([session(CLEAN)]).diagnosis, "none");
});

test("e5 the target boundary and rounding of the first contact rate", () => {
  const escalated = () => session(CLEAN, "escalated", true);
  const fourOfFive = audit([...times(4, () => session(CLEAN)), escalated()]);
  assert.equal(fourOfFive.fcr, 0.8);
  assert.equal(fourOfFive.meets_target, true);
  const threeOfFive = audit([...times(3, () => session(CLEAN)), ...times(2, escalated)]);
  assert.equal(threeOfFive.fcr, 0.6);
  assert.equal(threeOfFive.meets_target, false);
  assert.equal(audit([session(CLEAN), session(CLEAN), escalated()]).fcr, 0.667);
  const mixed = audit([session(CLEAN, "escalated"), session(CLEAN, "escalated"), session(CLEAN, "resolved", true)]);
  assert.deepEqual([mixed.over_escalated, mixed.under_escalated], [2, 1]);
});

test("e6 a wrong tool counts sessions and ignores steps with no known right tool", () => {
  const twice = session([step("get_customer", true, "lookup_order"), step("lookup_order", true, "process_refund")]);
  const unknown = session([step("get_customer"), step("lookup_order", true, "lookup_order")]);
  assert.equal(audit([twice, unknown, session(CLEAN)]).wrong_tool, 1);
  assert.equal(audit([twice, twice]).wrong_tool, 2);
});
