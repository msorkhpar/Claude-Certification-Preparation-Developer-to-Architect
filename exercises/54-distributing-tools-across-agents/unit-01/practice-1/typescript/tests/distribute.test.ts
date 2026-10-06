import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { assignTools, authorize, cacheImpact, checkTurn, planTurn } = await import(pathToFileURL(resolve(dir, "distribute.ts")).href);

const CATALOG = [
  { name: "web_search", tags: ["web"] },
  { name: "fetch_page", tags: ["web"] },
  { name: "load_document", tags: ["documents"] },
  { name: "extract_data_points", tags: ["documents"] },
  { name: "summarize_content", tags: ["synthesis"] },
  { name: "verify_fact", tags: ["web"], scoped: true },
  { name: "write_report", tags: ["reports"] },
  { name: "publish_report", tags: ["reports"], irreversible: true },
];
const ROLES = { searcher: { specialisation: ["web"] }, analyst: { specialisation: ["documents"] }, synthesizer: { specialisation: ["synthesis"], extra: ["verify_fact"] }, reporter: { specialisation: ["reports"] } };
const POLICY = { tools: { process_refund: { cap: 500, irreversible: true }, lookup_order: { cap: null, irreversible: false } } };

function assign(roles: any = ROLES, catalog: any = CATALOG, ...rest: any[]) {
  const result = assignTools(roles, catalog, ...rest);
  assert.ok(result !== null && result !== undefined, "assignTools returned nothing");
  return result;
}

const call = (over: Record<string, any> = {}) => ({ id: "c1", tool: "process_refund", amount: 50, customer: "C-1", verified_customer: "C-1", ...over });

function decide(c: any, approvals: any = []) {
  const result = authorize(c, POLICY, approvals);
  assert.ok(result !== null && result !== undefined, "authorize returned nothing");
  return result;
}

test("m1 each role gets only the tools of its specialisation in catalog order", () => {
  assert.deepEqual(assign(), { searcher: ["web_search", "fetch_page", "verify_fact"], analyst: ["load_document", "extract_data_points"], synthesizer: ["summarize_content", "verify_fact"], reporter: ["write_report"] });
});

test("e1 a role over its budget or given an unknown or unscoped outside tool or a duplicate catalog name is refused", () => {
  let refused = 0;
  const cases: Array<[any, any, any[]]> = [
    [ROLES, CATALOG, [2]],
    [{ synthesizer: { specialisation: ["synthesis"], extra: ["nope"] } }, CATALOG, []],
    [{ synthesizer: { specialisation: ["synthesis"], extra: ["fetch_page"] } }, CATALOG, []],
    [ROLES, [...CATALOG, { name: "web_search", tags: ["web"] }], []],
    [{ nobody: { specialisation: [] } }, CATALOG, []],
  ];
  for (const [roles, catalog, rest] of cases) {
    try { assignTools(roles, catalog, ...rest); } catch { refused++; }
  }
  assert.equal(refused, 5);
  assert.deepEqual(assign(ROLES, CATALOG, 3).searcher, ["web_search", "fetch_page", "verify_fact"]);
  assert.deepEqual(assign({ searcher: { specialisation: ["web"], extra: ["fetch_page"] } }).searcher, ["web_search", "fetch_page", "verify_fact"]);
});

test("e2 an irreversible tool is given only by an explicit grant", () => {
  assert.ok(!assign().reporter.includes("publish_report"));
  assert.deepEqual(assign({ reporter: { specialisation: ["reports"], extra: ["publish_report"] } }).reporter, ["write_report", "publish_report"]);
  assert.deepEqual(assign({ analyst: { specialisation: ["documents"], extra: ["publish_report"] } }).analyst, ["load_document", "extract_data_points", "publish_report"]);
});

test("e3 a model that accepts forcing gets the native choice and the others get auto with strict tools and one named tool", () => {
  const tools = ["extract_metadata", "enrich"];
  assert.deepEqual(planTurn("claude-opus-5", "named", tools, "extract_metadata"), { tool_choice: { type: "tool", name: "extract_metadata" }, tools, strict: false, verify_call: false });
  assert.deepEqual(planTurn("claude-opus-5", "any", tools), { tool_choice: { type: "any" }, tools, strict: false, verify_call: false });
  for (const model of ["claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"]) {
    assert.deepEqual(planTurn(model, "named", tools, "extract_metadata"), { tool_choice: { type: "auto" }, tools: ["extract_metadata"], strict: true, verify_call: true });
    assert.deepEqual(planTurn(model, "any", tools), { tool_choice: { type: "auto" }, tools, strict: true, verify_call: true });
    assert.deepEqual(planTurn(model, "free", tools).tool_choice, { type: "auto" });
    assert.deepEqual(planTurn(model, "none", tools).tool_choice, { type: "none" });
  }
  assert.deepEqual(planTurn("claude-opus-5", "named", tools, "enrich", true).tools, ["enrich"]);
  let refused = 0;
  for (const args of [["claude-opus-5", "sometimes", tools], ["claude-opus-5", "named", tools]]) {
    try { (planTurn as any)(...args); } catch { refused++; }
  }
  assert.equal(refused, 2);
});

test("e4 a change of tool choice costs the cached messages a change of tools costs everything and a repeat costs nothing", () => {
  const tools = ["extract_metadata", "enrich"];
  const free = { tool_choice: { type: "auto" }, tools };
  assert.equal(cacheImpact(null, free), "none");
  assert.equal(cacheImpact(free, { tool_choice: { type: "auto" }, tools: ["extract_metadata", "enrich"] }), "none");
  const any = { tool_choice: { type: "any" }, tools };
  assert.ok(cacheImpact(free, any) === "messages" && cacheImpact(any, free) === "messages");
  const a = { tool_choice: { type: "tool", name: "a" }, tools }, b = { tool_choice: { type: "tool", name: "b" }, tools };
  assert.ok(cacheImpact(a, b) === "messages" && cacheImpact(a, a) === "none");
  const fallback = planTurn("claude-sonnet-5-5", "named", tools, "extract_metadata");
  assert.ok(fallback !== null && cacheImpact(free, fallback) === "all");
  assert.equal(cacheImpact(free, { tool_choice: { type: "auto" }, tools: ["enrich"] }), "all");
});

test("e5 a reply is checked against the call that was required", () => {
  const text = { type: "text", text: "I think so." }, useA = { type: "tool_use", name: "a" }, useB = { type: "tool_use", name: "b" };
  assert.equal(checkTurn([text], "any"), "missed_call");
  assert.equal(checkTurn([text, useB], "any"), "ok");
  assert.equal(checkTurn([], "named", "a"), "missed_call");
  assert.equal(checkTurn([useB], "named", "a"), "wrong_tool");
  assert.equal(checkTurn([useA, useB], "named", "a"), "ok");
  assert.equal(checkTurn([useB, useA], "named", "a"), "wrong_tool");
  assert.ok(checkTurn([text], "free") === "ok" && checkTurn([useA], "none") === "ok");
});

test("e6 an unknown tool a wrong owner or a bad amount is refused", () => {
  const unknown = decide(call({ tool: "delete_account" }));
  assert.ok(unknown.allowed === false && unknown.code === "unknown_tool" && unknown.escalate === false);
  for (const other of ["C-2", null]) {
    const r = decide(call({ customer: other }));
    assert.ok(r.allowed === false && r.code === "not_owner" && r.escalate === false);
  }
  assert.equal(decide(call({ verified_customer: null })).code, "not_owner");
  for (const amount of [0, -5, 12.5, "50", true, null]) {
    const r = decide(call({ amount }), ["c1"]);
    assert.ok(r.allowed === false && r.code === "bad_amount", String(amount));
  }
  const free = decide({ id: "c2", tool: "lookup_order", customer: "C-1", verified_customer: "C-1" });
  assert.ok(free.allowed === true && free.code === "ok");
});

test("e7 the cap is inclusive and an irreversible call needs approval that never lifts the cap", () => {
  assert.equal(decide(call({ amount: 500 }), new Set(["c1"])).code, "ok");
  const waiting = decide(call({ amount: 50 }));
  assert.ok(waiting.allowed === false && waiting.code === "needs_approval" && waiting.escalate === true);
  assert.equal(decide(call({ amount: 50 }), ["c1"]).allowed, true);
  assert.equal(decide(call({ amount: 50 }), new Set(["c9"])).code, "needs_approval");
  const over = decide(call({ amount: 501 }), new Set(["c1"]));
  assert.ok(over.allowed === false && over.code === "over_cap" && over.escalate === true && over.message.includes("500"));
  assert.equal(decide(call({ amount: 501, customer: "C-2" })).code, "not_owner");
});
