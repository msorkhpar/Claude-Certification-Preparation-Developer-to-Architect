import { test } from "node:test";
import assert from "node:assert/strict";
import { budgetState, checkAgent, checkBudget, checkEnvironment, checkSessionResources, listCostCents } from "./managedAgent.ts";

test("an omitted networking field is a finding and limited needs the package flag", () => {
  assert.match(checkEnvironment({ type: "cloud" })[0], /unrestricted/);
  const limited: any = { type: "cloud", packages: { pip: ["x"] }, networking: { type: "limited" } };
  assert.ok(checkEnvironment(limited).some((f) => f.includes("allow_package_managers")));
  limited.networking.allow_package_managers = true;
  assert.deepEqual(checkEnvironment(limited), []);
});

test("hosts are bare and mcp hosts must be reachable", () => {
  const env: any = { type: "cloud", networking: { type: "limited", allowed_hosts: ["https://a.example.com", "b.example.com:443", "c.example.com"] } };
  const found = checkEnvironment(env, ["m.example.com", "c.example.com"]);
  assert.equal(found.filter((f) => f.includes("bare hostname")).length, 2);
  assert.equal(found.filter((f) => f.includes("MCP host")).length, 1);
  env.networking.allow_mcp_servers = true;
  assert.equal(checkEnvironment(env, ["m.example.com"]).filter((f) => f.includes("MCP host")).length, 0);
});

test("default policies flag bash on an open network and unreviewed mcp tools", () => {
  const agent = { tools: [{ type: "agent_toolset_20260401" }, { type: "mcp_toolset", mcp_server_name: "github", default_config: { permission_policy: { type: "always_allow" } } }] };
  assert.equal(checkAgent(agent, { type: "cloud" }).length, 2);
  assert.equal(checkAgent(agent, { type: "cloud", networking: { type: "limited" } }).length, 1);
  const asked = { tools: [{ type: "agent_toolset_20260401", configs: [{ name: "bash", permission_policy: { type: "always_ask" } }] }] };
  assert.deepEqual(checkAgent(asked, { type: "cloud" }), []);
});

test("self-hosted sandboxes accept only memory stores", () => {
  assert.deepEqual(checkSessionResources({ type: "self_hosted" }, [{ type: "file" }, { type: "github_repository" }, { type: "memory_store" }]), [
    "self-hosted sandboxes reject file resources (400)", "self-hosted sandboxes reject github_repository resources (400)"]);
  assert.deepEqual(checkSessionResources({ type: "cloud" }, [{ type: "file" }]), []);
});

test("list cost adds tokens, searches and running time", () => {
  assert.equal(listCostCents("claude-opus-5-5", 1_200_000, 150_000, 8, 7200), 804);
  assert.equal(listCostCents("claude-sonnet-5-5", 1_200_000, 150_000, 8, 7200), 414);
  assert.equal(listCostCents("claude-opus-5-5", 0, 0, 0, 3600), 8);
});

test("a budget is whole cents as a string and stops new work at the cap", () => {
  assert.deepEqual(["125", "25.00", "050", "0"].map(checkBudget), ["ok", ...["25.00", "050", "0"].map((a) => `amount '${a}' is rejected: write whole cents as a string with no leading zeros`)]);
  assert.equal(budgetState(804, "800"), "budget_reached");
  assert.equal(budgetState(800, "800"), "budget_reached");
  assert.equal(budgetState(804, "1000"), "running, 196 cents left");
});
