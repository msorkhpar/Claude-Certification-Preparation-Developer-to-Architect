// Managed Agents, checked offline: lint the configuration you would send, and price a session against its budget.
//
// Claude Managed Agents is a hosted agent harness: you create an agent (model, system prompt, tools), an environment (where the
// tools run: an Anthropic-managed cloud sandbox or a self-hosted one) and a session, then exchange events. Nothing here calls the
// API. The rules below are the ones the documentation states for environments, permission policies and session budgets, read on
// 2026-10-03 (beta header managed-agents-2026-04-01); the prices are the list prices recorded in docs/VERSIONS.md on 2026-10-02.
import { logger } from "./logger.ts";
const log = logger("managed_agent");

type Json = Record<string, any>;

// --- configuration checks ---------------------------------------------------------------------------------------------------

/** Findings for an environment payload. An omitted networking field becomes `unrestricted` on the API, so it is a finding. */
export function checkEnvironment(env: Json, agentMcpHosts: string[] = []): string[] {
  const findings: string[] = [];
  if (env.type === "self_hosted") return findings;
  const net = env.networking;
  if (net === undefined) return ["networking omitted: a create request that omits it gets unrestricted"];
  if (net.type === "unrestricted") return ["unrestricted networking: any host except a safety blocklist; keep secrets out of the sandbox"];
  for (const host of net.allowed_hosts ?? []) {
    if (host.includes("://") || host.includes("/") || host.includes(":")) findings.push(`allowed_hosts entry '${host}': use a bare hostname, no scheme, port or path`);
  }
  if (env.packages && Object.keys(env.packages).length && !net.allow_package_managers) {
    findings.push("packages with limited networking need allow_package_managers: true, or the request is rejected (400)");
  }
  const hosts = new Set<string>(net.allowed_hosts ?? []);
  for (const host of agentMcpHosts) {
    if (!hosts.has(host) && !net.allow_mcp_servers) findings.push(`MCP host ${host} is not reachable: add it to allowed_hosts or set allow_mcp_servers, or session creation fails (400)`);
  }
  return findings;
}

/** Findings for an agent payload read together with the environment it will run in. */
export function checkAgent(agent: Json, env: Json): string[] {
  const findings: string[] = [];
  for (const tool of agent.tools ?? []) {
    if (tool.type === "agent_toolset_20260401") {
      const policy = (tool.default_config?.permission_policy ?? { type: "always_allow" }).type;
      const overrides: Record<string, string | undefined> = {};
      for (const c of tool.configs ?? []) overrides[c.name] = c.permission_policy?.type;
      const bash = overrides.bash ?? policy;
      const reach = (env.networking ?? { type: "unrestricted" }).type !== "limited";
      if (bash === "always_allow" && reach) findings.push("bash runs without approval and the sandbox can reach any host: set bash to always_ask or auto, or use limited networking");
      for (const c of tool.configs ?? []) {
        if (c.allowed_domains?.length && c.blocked_domains?.length) findings.push(`${c.name}: allowed_domains and blocked_domains cannot be combined`);
      }
    }
    if (tool.type === "mcp_toolset") {
      const policy = (tool.default_config?.permission_policy ?? { type: "always_ask" }).type;
      if (policy === "always_allow") findings.push(`mcp toolset ${tool.mcp_server_name}: always_allow lets new tools of that server run unreviewed`);
    }
  }
  return findings;
}

/** A self-hosted sandbox accepts memory_store resources only. */
export function checkSessionResources(env: Json, resources: Json[]): string[] {
  if (env.type !== "self_hosted") return [];
  const bad = [...new Set(resources.map((r) => r.type).filter((t) => t !== "memory_store"))].sort();
  return bad.map((t) => `self-hosted sandboxes reject ${t} resources (400)`);
}

// --- list cost and budget ---------------------------------------------------------------------------------------------------

const PRICES: Record<string, [number, number]> = { "claude-opus-5-5": [4, 20], "claude-sonnet-5-5": [2, 10] }; // dollars per million input and output tokens
const WEB_SEARCH_MICRO = 10_000; // $10 per 1,000 searches, in millionths of a dollar per search
const RUNNING_MICRO_PER_HOUR = 80_000; // $0.08 per hour of session running time

/** The session's list cost in whole cents, rounded to the nearest cent, as the platform reports it. */
export function listCostCents(model: string, inputTokens: number, outputTokens: number, searches: number, activeSeconds: number): number {
  const [priceIn, priceOut] = PRICES[model];
  const micro = inputTokens * priceIn + outputTokens * priceOut + searches * WEB_SEARCH_MICRO + Math.floor((activeSeconds * RUNNING_MICRO_PER_HOUR) / 3600);
  return Math.floor((micro + 5_000) / 10_000);
}

/** max_list_cost.amount is a whole number of cents as a string, no leading zeros, greater than zero. */
export function checkBudget(amount: string): string {
  if (!(/^\d+$/.test(amount) && !amount.startsWith("0"))) return `amount '${amount}' is rejected: write whole cents as a string with no leading zeros`;
  return "ok";
}

/** At or past the cap the session goes idle with stop_reason budget_reached; the request in flight still finishes. */
export function budgetState(costCents: number, amount: string): string {
  const cap = Number(amount);
  return costCents >= cap ? "budget_reached" : `running, ${cap - costCents} cents left`;
}

function main() {
  const env = { type: "cloud", packages: { pip: ["sqlalchemy==2.0.30"] }, networking: { type: "limited", allowed_hosts: ["https://api.example.com"] } };
  const agent = { tools: [{ type: "agent_toolset_20260401" }, { type: "mcp_toolset", mcp_server_name: "github", default_config: { permission_policy: { type: "always_allow" } } }] };
  console.log("environment findings:");
  for (const line of checkEnvironment(env, ["mcp.example.com"])) console.log(" -", line);
  console.log("omitted networking:", checkEnvironment({ type: "cloud" })[0]);
  console.log("agent findings, unrestricted sandbox:");
  for (const line of checkAgent(agent, { type: "cloud" })) console.log(" -", line);
  console.log("agent findings, limited sandbox:", pyList(checkAgent(agent, { type: "cloud", networking: { type: "limited" } })));
  console.log("self-hosted with a file resource:", pyList(checkSessionResources({ type: "self_hosted" }, [{ type: "file" }, { type: "memory_store" }])));
  console.log();
  for (const model of ["claude-opus-5-5", "claude-sonnet-5-5"]) {
    const cents = listCostCents(model, 1_200_000, 150_000, 8, 7200);
    console.log(`${model}: list cost ${cents} cents; budget 800 -> ${budgetState(cents, "800")}; budget 1000 -> ${budgetState(cents, "1000")}`);
  }
  console.log("budget '25.00':", checkBudget("25.00"), "| budget '050':", checkBudget("050"), "| budget '125':", checkBudget("125"));
}

/** A list printed the way Python prints it, so both languages show the same output. */
function pyList(items: string[]): string {
  return `[${items.map((s) => (s.includes("'") ? `"${s}"` : `'${s}'`)).join(", ")}]`;
}

if (import.meta.main) main();
