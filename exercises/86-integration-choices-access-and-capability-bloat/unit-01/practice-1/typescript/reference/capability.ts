/** Capability design: what a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides. See ../../statement.md. */
import { logger } from "../logger.ts";
const log = logger("capability");

export type Agent = { holds: string[]; needs: string[]; used: Record<string, number> };
export type Catalog = Record<string, { access: string; tokens: number }>;
export type Request = { credential: string | null; model: string; tool: string | null; recent: number };
export type Policy = { credentials: Record<string, string>; models: Record<string, string[]>; tools: Record<string, string[]>; limits: Record<string, number>; routes: Record<string, string> };
const RISKY = new Set(["money", "destroy"]);

function remove(agent: Agent): string[] {
  return agent.holds.filter((t) => !agent.needs.includes(t));
}

function risky(toRemove: string[], catalog: Catalog): string[] {
  return toRemove.filter((t) => RISKY.has(catalog[t].access));
}

function dormant(agent: Agent): string[] {
  return agent.holds.filter((t) => agent.needs.includes(t) && (agent.used[t] ?? 0) === 0);
}

export function audit(agent: Agent, catalog: Catalog) {
  log.debug("audit input", agent);
  const gone = remove(agent);
  return { remove: gone, risky: risky(gone, catalog), missing: agent.needs.filter((t) => !agent.holds.includes(t)), dormant: dormant(agent) };
}

function clamp(keep: number): number {
  return Math.max(3, Math.min(5, keep));
}

function defers(tools: Record<string, number>): boolean {
  return Object.keys(tools).length >= 10 || Object.values(tools).reduce((a, t) => a + t, 0) > 10000;
}

function ranked(tools: Record<string, number>, usage: Record<string, number>, keep: number): string[] {
  return Object.keys(tools).sort((a, b) => (usage[b] ?? 0) - (usage[a] ?? 0) || (a < b ? -1 : 1)).slice(0, keep);
}

export function planLoading(tools: Record<string, number>, usage: Record<string, number>, keep = 4, searchTokens = 350) {
  log.debug("planLoading input", tools);
  const names = Object.keys(tools);
  if (!defers(tools)) return { search: false, load_now: names, deferred: [] as string[], tokens: names.reduce((a, t) => a + tools[t], 0) };
  const first = ranked(tools, usage, clamp(keep));
  return { search: true, load_now: first, deferred: names.filter((t) => !first.includes(t)), tokens: first.reduce((a, t) => a + tools[t], 0) + searchTokens };
}

export function chooseMechanism(consumers: number, counterpart: string, path: string): string | null {
  if (counterpart === "agent") return "agent-to-agent";
  if (path === "fixed") return "direct call in code";
  return consumers > 1 ? "MCP server" : "custom tool";
}

export function authorize(tool: string, userScopes: string[], agentScopes: string[], required: Record<string, string>): string | null {
  if (!(tool in required)) return "deny: unknown tool";
  const scope = required[tool];
  if (!userScopes.includes(scope)) return `deny: user lacks ${scope}`;
  if (!agentScopes.includes(scope)) return `deny: agent lacks ${scope}`;
  return "allow";
}

function verdict(team: string | undefined, request: Request, policy: Policy): [string, string] {
  if (team === undefined) return ["deny", "unauthenticated"];
  if (!policy.models[team].includes(request.model)) return ["deny", "model not allowed"];
  if (request.tool !== null && !policy.tools[team].includes(request.tool)) return ["deny", "tool not allowed"];
  if (request.recent >= policy.limits[team]) return ["deny", "rate limited"];
  return ["allow", `routed to ${policy.routes[request.model] ?? request.model}`];
}

function record(team: string | undefined, request: Request, decision: string): Record<string, string> {
  return { team: team ?? "unknown", model: request.model, tool: request.tool ?? "none", decision };
}

export function gateway(request: Request, policy: Policy) {
  log.debug("gateway input", request);
  const team = request.credential === null ? undefined : policy.credentials[request.credential];
  const [decision, reason] = verdict(team, request, policy);
  return { decision, reason, audit: record(team, request, decision) };
}
