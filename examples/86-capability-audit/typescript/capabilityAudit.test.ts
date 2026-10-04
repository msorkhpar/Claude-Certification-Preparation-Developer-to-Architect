import { test } from "node:test";
import assert from "node:assert/strict";
import { CATALOG, POLICY, TOOLS, USAGE, audit, authorize, chooseMechanism, gateway, planLoading } from "./capabilityAudit.ts";

test("a role loses the tools it does not need and the risky ones are named", () => {
  assert.deepEqual(audit(Object.keys(CATALOG), ["read_ticket", "draft_reply"], CATALOG), { remove: ["issue_refund", "delete_account"], risky: ["issue_refund", "delete_account"], missing: [] });
  assert.deepEqual(audit(["read_ticket"], ["read_ticket", "draft_reply"], CATALOG), { remove: [], risky: [], missing: ["draft_reply"] });
});

test("a large tool set defers all but the most used and a small one loads whole", () => {
  const plan = planLoading(TOOLS, USAGE);
  assert.equal(plan.search, true);
  assert.deepEqual(plan.load_now, ["github_create_issue", "github_search_code", "slack_post_message", "github_get_pr"]);
  assert.equal(plan.deferred.length, 20);
  assert.equal(plan.tokens, 2320);
  const small = Object.fromEntries(Object.keys(TOOLS).slice(0, 9).map((n) => [n, 100]));
  assert.deepEqual(planLoading(small, USAGE), { search: false, load_now: Object.keys(small), deferred: [], tokens: 900 });
  assert.equal(planLoading({ a: 6000, b: 5000 }, {}).search, true);
  assert.equal(planLoading(TOOLS, USAGE, 9).load_now.length, 5);
  assert.equal(planLoading(TOOLS, USAGE, 1).load_now.length, 3);
});

test("the mechanism follows the counterpart then the path then the number of clients", () => {
  const rows: Array<[number, string, string]> = [[1, "agent", "fixed"], [4, "tool", "fixed"], [4, "tool", "model-chosen"], [1, "tool", "model-chosen"]];
  assert.deepEqual(rows.map(([c, k, p]) => chooseMechanism(c, k, p)), ["agent-to-agent", "direct call in code", "MCP server", "custom tool"]);
});

test("the agent rights are a ceiling and the user rights decide", () => {
  const required = { issue_refund: "refunds:write" };
  assert.equal(authorize("issue_refund", ["tickets:read"], ["refunds:write"], required), "deny: user lacks refunds:write");
  assert.equal(authorize("issue_refund", ["refunds:write"], ["tickets:read"], required), "deny: agent lacks refunds:write");
  assert.equal(authorize("issue_refund", ["refunds:write"], ["refunds:write"], required), "allow");
  assert.equal(authorize("wipe", ["x"], ["x"], required), "deny: unknown tool");
});

test("the gateway decides in order and keeps a record of every decision", () => {
  assert.equal(gateway(null, "standard", 0, POLICY).reason, "unauthenticated");
  assert.equal(gateway("key-a", "deep", 0, POLICY).reason, "model not allowed");
  assert.equal(gateway("key-a", "standard", 30, POLICY).reason, "rate limited");
  assert.deepEqual(gateway("key-b", "deep", 3, POLICY), { decision: "allow", reason: "routed to claude-opus-5-5", audit: { team: "research", model: "deep", decision: "allow" } });
  assert.deepEqual(gateway("nope", "deep", 0, POLICY).audit, { team: "unknown", model: "deep", decision: "deny" });
});
