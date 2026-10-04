/** Capability design: what a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides. See ../../statement.md. */

export type Agent = { holds: string[]; needs: string[]; used: Record<string, number> };
export type Catalog = Record<string, { access: string; tokens: number }>;
export type Request = { credential: string | null; model: string; tool: string | null; recent: number };
export type Policy = { credentials: Record<string, string>; models: Record<string, string[]>; tools: Record<string, string[]>; limits: Record<string, number>; routes: Record<string, string> };
const RISKY = new Set(["money", "destroy"]);

export function audit(agent: Agent, catalog: Catalog) {
  const { holds, needs, used } = agent;
  const remove = holds.filter((t) => !needs.includes(t));
  return {
    remove,
    risky: remove.filter((t) => RISKY.has(catalog[t].access)),
    missing: needs.filter((t) => !holds.includes(t)),
    dormant: holds.filter((t) => needs.includes(t) && (used[t] ?? 0) === 0),
  };
}

export function planLoading(tools: Record<string, number>, usage: Record<string, number>, keep = 4, searchTokens = 350) {
  keep = Math.max(3, Math.min(5, keep));
  const names = Object.keys(tools);
  const total = names.reduce((a, t) => a + tools[t], 0);
  if (names.length < 10 && total <= 10000) return { search: false, load_now: names, deferred: [] as string[], tokens: total };
  const ranked = [...names].sort((a, b) => (usage[b] ?? 0) - (usage[a] ?? 0) || (a < b ? -1 : 1)).slice(0, keep);
  return { search: true, load_now: ranked, deferred: names.filter((t) => !ranked.includes(t)), tokens: ranked.reduce((a, t) => a + tools[t], 0) + searchTokens };
}

export function chooseMechanism(consumers: number, counterpart: string, path: string): string {
  if (counterpart === "agent") return "agent-to-agent";
  if (path === "fixed") return "direct call in code";
  return consumers > 1 ? "MCP server" : "custom tool";
}

export function authorize(tool: string, userScopes: string[], agentScopes: string[], required: Record<string, string>): string {
  if (!(tool in required)) return "deny: unknown tool";
  const scope = required[tool];
  if (!userScopes.includes(scope)) return `deny: user lacks ${scope}`;
  if (!agentScopes.includes(scope)) return `deny: agent lacks ${scope}`;
  return "allow";
}

export function gateway(request: Request, policy: Policy) {
  const team = request.credential === null ? undefined : policy.credentials[request.credential];
  let decision = "deny";
  let reason: string;
  if (team === undefined) reason = "unauthenticated";
  else if (!policy.models[team].includes(request.model)) reason = "model not allowed";
  else if (request.tool !== null && !policy.tools[team].includes(request.tool)) reason = "tool not allowed";
  else if (request.recent >= policy.limits[team]) reason = "rate limited";
  else {
    decision = "allow";
    reason = `routed to ${policy.routes[request.model] ?? request.model}`;
  }
  return { decision, reason, audit: { team: team ?? "unknown", model: request.model, tool: request.tool ?? "none", decision } };
}
