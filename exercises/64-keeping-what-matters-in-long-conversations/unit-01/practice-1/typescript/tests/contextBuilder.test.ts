import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const solution = await import(pathToFileURL(resolve(dir, "contextBuilder.ts")).href);
const got = (fn: (...args: any[]) => any) => (...args: any[]) => {
  const value = fn(...args);
  assert.ok(value !== null && value !== undefined, `${fn.name} returned nothing`);
  return value;
};
const [buildContext, missingFromSummary, trimRecord, updateFacts, window] = [solution.buildContext, solution.missingFromSummary, solution.trimRecord, solution.updateFacts, solution.window].map(got);

const ORDER = { order_id: "A-1042", purchase_date: "2026-09-02", items: "2 x kettle", return_window: "30 days", warehouse_bin: "R7-22", carrier_hash: "9f3c", refund_amount: "$129.50" };
const fact = (customer = "c1", name = "refund", value = "$129.50", as_of = "2026-09-02") => ({ customer, name, value, as_of });
const msg = (role: string, kind: string, id: string, text: string) => ({ role, kind, id, text });

test("m1 trimming keeps only the named fields with their exact values in the named order", () => {
  assert.deepEqual(Object.entries(trimRecord(ORDER, ["refund_amount", "order_id"])), [["refund_amount", "$129.50"], ["order_id", "A-1042"]]);
  assert.ok(!("warehouse_bin" in trimRecord(ORDER, ["order_id", "items"])));
});

test("e1 a field that the record does not have is skipped", () => {
  assert.deepEqual(trimRecord(ORDER, ["order_id", "tracking_url"]), { order_id: "A-1042" });
  assert.deepEqual(trimRecord({}, ["order_id"]), {});
});

test("e2 a newer fact replaces the old one and the old value is kept as history", () => {
  const start = updateFacts({}, "address", "12 Oak St", "2026-08-01");
  assert.deepEqual(start, { address: { value: "12 Oak St", as_of: "2026-08-01", superseded: [] } });
  const later = updateFacts(start, "address", "9 Elm Rd", "2026-09-10");
  assert.deepEqual(later.address, { value: "9 Elm Rd", as_of: "2026-09-10", superseded: ["12 Oak St@2026-08-01"] });
  assert.ok(start.address.value === "12 Oak St" && start.address.superseded.length === 0);
  assert.deepEqual(updateFacts(start, "address", "9 Elm Rd", "2026-08-01").address, { value: "9 Elm Rd", as_of: "2026-08-01", superseded: ["12 Oak St@2026-08-01"] });
});

test("e3 an older fact that arrives late does not replace the current one", () => {
  const current = updateFacts({}, "address", "9 Elm Rd", "2026-09-10");
  const after = updateFacts(current, "address", "12 Oak St", "2026-08-01");
  assert.ok(after.address.value === "9 Elm Rd" && after.address.as_of === "2026-09-10");
  assert.deepEqual(after.address.superseded, ["12 Oak St@2026-08-01"]);
});

test("e4 the case facts of another customer never enter the context", () => {
  const text = buildContext("c1", [fact(), fact("c2", "refund", "$20.00")], "summary", []);
  assert.ok(text.includes("$129.50") && !text.includes("$20.00"));
  assert.ok(!buildContext("c3", [fact()], "summary", []).includes("## Case facts"));
});

test("e5 the context puts case facts first then the summary then the recent messages", () => {
  const text = buildContext("c1", [fact("c1", "refund"), fact("c1", "order", "A-1042")], "They want the money back.", [{ role: "user", text: "Any news?" }]);
  assert.equal(text, "## Case facts\nrefund: $129.50 (as of 2026-09-02)\norder: A-1042 (as of 2026-09-02)\n\n## Summary so far\nThey want the money back.\n\n## Recent messages\nuser: Any news?");
});

test("e6 a summary that loses an exact value is reported", () => {
  const facts = [fact("c1", "refund", "$129.50"), fact("c1", "deadline", "2026-09-30"), fact("c1", "order", "A-1042")];
  assert.deepEqual(missingFromSummary("Refund of about $130 for order A-1042, due end of month.", facts), ["refund", "deadline"]);
  assert.deepEqual(missingFromSummary("$129.50, 2026-09-30, A-1042", facts), []);
});

test("e7 the window drops the oldest messages and keeps a tool call with its result", () => {
  const messages = [msg("user", "text", "", "x".repeat(40)), msg("assistant", "tool_use", "t1", "y".repeat(40)), msg("user", "tool_result", "t1", "z".repeat(40)), msg("assistant", "text", "", "done.....")];
  assert.deepEqual(window(messages, 14).map((m: any) => m.text[0]), ["d"]);
  assert.deepEqual(window(messages, 23).map((m: any) => m.kind), ["tool_use", "tool_result", "text"]);
  assert.equal(window(messages, 1000).length, 4);
  assert.deepEqual(window(messages, 0), []);
});
