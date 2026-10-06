import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { audit, authorize, chooseMechanism, gateway, planLoading } = await import(pathToFileURL(resolve(dir, "capability.ts")).href);

const CATALOG = {
  read_ticket: { access: "read", tokens: 160 },
  draft_reply: { access: "draft", tokens: 220 },
  issue_refund: { access: "money", tokens: 240 },
  delete_account: { access: "destroy", tokens: 210 },
  export_report: { access: "read", tokens: 300 },
};
const POLICY = {
  credentials: { k1: "support", k2: "research" },
  models: { support: ["standard"], research: ["standard", "deep"] },
  tools: { support: ["read_ticket", "draft_reply"], research: ["query"] },
  limits: { support: 2, research: 5 },
  routes: { standard: "claude-sonnet-5-5", deep: "claude-opus-5-5" },
};
const plan = (...args: any[]) => {
  const result = planLoading(...args);
  assert.ok(result && typeof result === "object", "planLoading returned nothing");
  return result;
};
const decide = (credential: string | null, model: string, tool: string | null, recent: number) => {
  const result = gateway({ credential, model, tool, recent }, POLICY);
  assert.ok(result && typeof result === "object", "gateway returned nothing");
  return result;
};
const request = (credential: string | null, model: string, tool: string | null, recent: number) => ({ credential, model, tool, recent });
const names = (count: number) => Object.fromEntries(Array.from({ length: count }, (_, i) => [`t${String(i + 1).padStart(2, "0")}`, 100]));

test("m1 an agent loses the tools its role does not need and the risky ones among them are named", () => {
  const agent = { holds: Object.keys(CATALOG), needs: ["read_ticket", "draft_reply"], used: { read_ticket: 12, draft_reply: 9 } };
  assert.deepEqual(audit(agent, CATALOG), { remove: ["issue_refund", "delete_account", "export_report"], risky: ["issue_refund", "delete_account"], missing: [], dormant: [] });
});

test("e1 a tool that is held and needed but never used is reported as dormant and never removed", () => {
  const agent = { holds: ["read_ticket", "draft_reply"], needs: ["read_ticket", "draft_reply", "issue_refund"], used: { read_ticket: 5, draft_reply: 0 } as Record<string, number> };
  assert.deepEqual(audit(agent, CATALOG), { remove: [], risky: [], missing: ["issue_refund"], dormant: ["draft_reply"] });
  agent.used = { read_ticket: 5 };
  assert.deepEqual(audit(agent, CATALOG).dormant, ["draft_reply"]);
});

test("e2 a small set loads whole and a large one defers when it has ten tools or over ten thousand tokens", () => {
  assert.deepEqual(plan(names(9), {}), { search: false, load_now: Object.keys(names(9)), deferred: [], tokens: 900 });
  const ten = plan(names(10), {});
  assert.equal(ten.search, true);
  assert.deepEqual(ten.load_now, ["t01", "t02", "t03", "t04"]);
  assert.equal(ten.deferred.length, 6);
  assert.equal(ten.tokens, 750);
  assert.equal(plan({ a: 5000, b: 5000 }, {}).search, false);
  const heavy = plan({ a: 5000, b: 5001 }, {});
  assert.equal(heavy.search, true);
  assert.equal(heavy.tokens, 10001 + 350);
  assert.deepEqual(heavy.deferred, []);
});

test("e3 the number of tools kept loaded stays between three and five and ties are broken by name", () => {
  assert.equal(plan(names(12), {}, 1).load_now.length, 3);
  assert.equal(plan(names(12), {}, 8).load_now.length, 5);
  assert.deepEqual(plan(Object.fromEntries(Object.entries(names(12)).reverse()), { t05: 9, t02: 9 }, 3).load_now, ["t02", "t05", "t01"]);
  assert.equal(plan(names(12), {}, 4, 0).tokens, 400);
});

test("e4 the mechanism follows the counterpart then the path then the number of clients", () => {
  const rows: Array<[number, string, string]> = [[1, "agent", "fixed"], [4, "agent", "model-chosen"], [4, "tool", "fixed"], [1, "tool", "fixed"], [4, "tool", "model-chosen"], [1, "tool", "model-chosen"]];
  assert.deepEqual(rows.map(([c, k, p]) => chooseMechanism(c, k, p)), ["agent-to-agent", "agent-to-agent", "direct call in code", "direct call in code", "MCP server", "custom tool"]);
});

test("e5 a call needs the users scope and the agents scope and an unknown tool is refused", () => {
  const required = { issue_refund: "refunds:write", read_ticket: "tickets:read" };
  assert.equal(authorize("issue_refund", ["tickets:read"], ["refunds:write"], required), "deny: user lacks refunds:write");
  assert.equal(authorize("issue_refund", ["refunds:write"], ["tickets:read"], required), "deny: agent lacks refunds:write");
  assert.equal(authorize("issue_refund", [], [], required), "deny: user lacks refunds:write");
  assert.equal(authorize("issue_refund", ["refunds:write"], ["refunds:write"], required), "allow");
  assert.equal(authorize("wipe_disk", ["x"], ["x"], required), "deny: unknown tool");
});

test("e6 the gateway checks the credential then the model then the tool then the rate", () => {
  const reason = (credential: string | null, model: string, tool: string | null, recent: number) => decide(credential, model, tool, recent).reason;
  assert.equal(reason(null, "deep", "wipe", 99), "unauthenticated");
  assert.equal(reason("zzz", "deep", "wipe", 99), "unauthenticated");
  assert.equal(reason("k1", "deep", "wipe", 99), "model not allowed");
  assert.equal(reason("k1", "standard", "issue_refund", 99), "tool not allowed");
  assert.equal(reason("k1", "standard", "read_ticket", 2), "rate limited");
  assert.equal(reason("k1", "standard", "read_ticket", 1), "routed to claude-sonnet-5-5");
  assert.equal(decide("k2", "deep", null, 0).decision, "allow");
  assert.equal(reason("k2", "deep", null, 0), "routed to claude-opus-5-5");
  assert.equal(decide("k1", "standard", "read_ticket", 2).decision, "deny");
});

test("e7 the gateway keeps a record of every decision with the team or unknown and no content", () => {
  assert.deepEqual(decide(null, "deep", null, 0).audit, { team: "unknown", model: "deep", tool: "none", decision: "deny" });
  assert.deepEqual(decide("k1", "standard", "issue_refund", 0).audit, { team: "support", model: "standard", tool: "issue_refund", decision: "deny" });
  assert.deepEqual(decide("k2", "deep", "query", 0).audit, { team: "research", model: "deep", tool: "query", decision: "allow" });
});
