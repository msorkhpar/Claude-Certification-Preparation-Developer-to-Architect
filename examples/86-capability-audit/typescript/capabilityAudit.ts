/**
 * Integration design decisions in code: which tools a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides.
 *
 * The numbers are invented for the example, and the sizes of tool definitions are the example's own. The rules are those of the Claude Certified Architect - Professional exam guide (domain 3), the Claude documentation page
 * "Tool search tool", the Model Context Protocol security best practices and the Claude Code gateway pages, read on 2026-10-04. Nothing here calls a model.
 */
export const CATALOG: Record<string, [string, number]> = { read_ticket: ["read", 160], draft_reply: ["draft", 220], issue_refund: ["money", 240], delete_account: ["destroy", 210] };
const RISKY = new Set(["money", "destroy"]);
const SERVER_TOOLS: Record<string, string[]> = {
  github: ["create_issue", "search_code", "get_pr", "list_prs", "merge_pr", "comment", "list_repos", "get_file"],
  slack: ["post_message", "search", "list_channels", "get_thread", "react", "upload"],
  sentry: ["list_issues", "get_event", "resolve", "assign", "search"],
  grafana: ["query", "list_dashboards", "get_panel", "create_alert", "list_alerts"],
};
const SERVER_SIZE: Record<string, number> = { github: 520, slack: 410, sentry: 480, grafana: 620 };
export const TOOLS: Record<string, number> = Object.fromEntries(Object.entries(SERVER_TOOLS).flatMap(([server, names]) => names.map((name) => [`${server}_${name}`, SERVER_SIZE[server]])));
export const USAGE: Record<string, number> = { github_create_issue: 90, github_search_code: 70, slack_post_message: 60, github_get_pr: 50, sentry_list_issues: 20, grafana_query: 10 };
export type Policy = { credentials: Record<string, string>; models: Record<string, string[]>; limits: Record<string, number>; routes: Record<string, string> };
export const POLICY: Policy = {
  credentials: { "key-a": "support", "key-b": "research" },
  models: { support: ["standard"], research: ["standard", "deep"] },
  limits: { support: 30, research: 10 },
  routes: { standard: "claude-sonnet-5-5", deep: "claude-opus-5-5" },
};

/** Least privilege: a tool the role does not need is removed from its configuration, not logged or put behind a confirmation. */
export function audit(holds: string[], needs: string[], catalog: Record<string, [string, number]>) {
  const remove = holds.filter((t) => !needs.includes(t));
  return { remove, risky: remove.filter((t) => RISKY.has(catalog[t][0])), missing: needs.filter((t) => !holds.includes(t)) };
}

/** With 10 or more tools, or definitions over 10,000 tokens, the 3 to 5 most used tools stay loaded and the rest are found through a search tool. */
export function planLoading(tools: Record<string, number>, usage: Record<string, number>, keep = 4, searchTokens = 350) {
  keep = Math.max(3, Math.min(5, keep));
  const names = Object.keys(tools);
  const total = names.reduce((a, t) => a + tools[t], 0);
  if (names.length < 10 && total <= 10000) return { search: false, load_now: names, deferred: [] as string[], tokens: total };
  const ranked = [...names].sort((a, b) => (usage[b] ?? 0) - (usage[a] ?? 0) || (a < b ? -1 : 1)).slice(0, keep);
  return { search: true, load_now: ranked, deferred: names.filter((t) => !ranked.includes(t)), tokens: ranked.reduce((a, t) => a + tools[t], 0) + searchTokens };
}

/** Another agent is reached agent-to-agent; a step with a known path is a call in code; a capability several clients share is an MCP server; otherwise it is a tool of the one application. */
export function chooseMechanism(consumers: number, counterpart: string, path: string): string {
  if (counterpart === "agent") return "agent-to-agent";
  if (path === "fixed") return "direct call in code";
  return consumers > 1 ? "MCP server" : "custom tool";
}

/** A call is allowed only when the user holds the scope the tool needs and the agent does too; the agent's own rights are a ceiling, not a licence. */
export function authorize(tool: string, userScopes: string[], agentScopes: string[], required: Record<string, string>): string {
  if (!(tool in required)) return "deny: unknown tool";
  const scope = required[tool];
  if (!userScopes.includes(scope)) return `deny: user lacks ${scope}`;
  if (!agentScopes.includes(scope)) return `deny: agent lacks ${scope}`;
  return "allow";
}

/** One place decides who is calling, which models that team may use and how many requests a minute it may send, and keeps a record of every decision. */
export function gateway(credential: string | null, model: string, recent: number, policy: Policy) {
  const team = credential === null ? undefined : policy.credentials[credential];
  let decision = "deny";
  let reason: string;
  if (team === undefined) reason = "unauthenticated";
  else if (!policy.models[team].includes(model)) reason = "model not allowed";
  else if (recent >= policy.limits[team]) reason = "rate limited";
  else {
    decision = "allow";
    reason = `routed to ${policy.routes[model] ?? model}`;
  }
  return { decision, reason, audit: { team: team ?? "unknown", model, decision } };
}

const percent = (part: number, whole: number) => Math.floor((200 * part + whole) / (2 * whole));

function main() {
  const holds = Object.keys(CATALOG);
  const needs = ["read_ticket", "draft_reply"];
  const result = audit(holds, needs, CATALOG);
  const cost = (names: string[]) => names.reduce((a, t) => a + CATALOG[t][1], 0);
  console.log(`support agent holds ${holds.length} tools (${cost(holds)} tokens) and needs ${needs.length}`);
  console.log(`  remove: ${result.remove.join(", ")}; risky among them: ${result.risky.join(", ")}`);
  console.log(`  after removal: ${holds.length - result.remove.length} tools, ${cost(needs)} tokens`);
  const three = Object.fromEntries(Object.entries(CATALOG).slice(0, 3).map(([k, v]) => [k, v[1]]));
  let plan = planLoading(three, USAGE);
  for (const [name, tools] of [["three tools", three], ["four servers", TOOLS]] as Array<[string, Record<string, number>]>) {
    plan = planLoading(tools, USAGE);
    const total = Object.values(tools).reduce((a, b) => a + b, 0);
    const saved = plan.search ? `, ${percent(total - plan.tokens, total)}% fewer` : "";
    console.log(`${name}: ${Object.keys(tools).length} tools, ${total} tokens of definitions -> search tool ${plan.search ? "yes" : "no"}, ${plan.load_now.length} loaded now, ${plan.deferred.length} deferred, ${plan.tokens} tokens up front${saved}`);
  }
  console.log("  loaded now: " + plan.load_now.join(", "));
  const rows: Array<[number, string, string]> = [[1, "tool", "model-chosen"], [4, "tool", "model-chosen"], [1, "tool", "fixed"], [1, "agent", "model-chosen"]];
  for (const [consumers, counterpart, path] of rows) console.log(`  ${consumers} application(s), counterpart ${counterpart.padEnd(5)}, path ${path.padEnd(12)} -> ${chooseMechanism(consumers, counterpart, path)}`);
  const required = { read_ticket: "tickets:read", issue_refund: "refunds:write" };
  const user = ["tickets:read"];
  const agent = ["tickets:read", "refunds:write"];
  console.log(`refund asked by a user who may only read: agent's rights alone -> ${agent.includes(required.issue_refund) ? "allow" : "deny"}; user's and agent's rights -> ${authorize("issue_refund", user, agent, required)}`);
  const log: Array<{ team: string; model: string; decision: string }> = [];
  const requests: Array<[string | null, string, number]> = [[null, "standard", 0], ["key-a", "deep", 0], ["key-a", "standard", 30], ["key-b", "deep", 3]];
  for (const [credential, model, recent] of requests) {
    const outcome = gateway(credential, model, recent, POLICY);
    log.push(outcome.audit);
    console.log(`  gateway: credential ${(credential ?? "none").padEnd(5)} model ${model.padEnd(8)} recent ${String(recent).padStart(2)} -> ${outcome.decision}: ${outcome.reason}`);
  }
  console.log(`  records kept: ${log.length}, denials among them: ${log.filter((r) => r.decision === "deny").length}`);
}

if (import.meta.main) main();
