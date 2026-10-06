/** Capability design: what a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides. See ../../statement.md. */
import { logger } from "./logger.ts";
const log = logger("capability");

export type Agent = { holds: string[]; needs: string[]; used: Record<string, number> };
export type Catalog = Record<string, { access: string; tokens: number }>;
export type Request = { credential: string | null; model: string; tool: string | null; recent: number };
export type Policy = { credentials: Record<string, string>; models: Record<string, string[]>; tools: Record<string, string[]>; limits: Record<string, number>; routes: Record<string, string> };
const RISKY = new Set(["money", "destroy"]);

function remove(agent: Agent): string[] {
  // TODO 1 of 10 (unlocks m1): the tools to take away.
  // Receives the agent (`holds`, `needs`, `used`). Returns the held tools that the role does not need, in the order they are held.
  // Example: holds ["read", "refund"], needs ["read"] -> ["refund"]
  return [];
}

function risky(toRemove: string[], catalog: Catalog): string[] {
  // TODO 2 of 10 (unlocks m1): the risky tools among those to remove.
  // Receives the tools to remove and the catalog (tool to { access, tokens }). Returns those whose access class is in RISKY, in the same order.
  // Example: remove ["refund", "export"], refund has access "money" and export has "read" -> ["refund"]
  return [];
}

function dormant(agent: Agent): string[] {
  // TODO 3 of 10 (unlocks e1): the tools that are kept but never used.
  // Receives the agent. Returns the held tools that the role needs and that have no calls in `used` (a tool missing from `used` has none), in the order held. They are
  // reported and never removed.
  // Example: holds ["a", "b"], needs ["a", "b"], used { a: 5, b: 0 } -> ["b"]
  return [];
}

export function audit(agent: Agent, catalog: Catalog) {
  log.debug("audit input", agent);
  const gone = remove(agent);
  return { remove: gone, risky: risky(gone, catalog), missing: agent.needs.filter((t) => !agent.holds.includes(t)), dormant: dormant(agent) };
}

function clamp(keep: number): number {
  // TODO 4 of 10 (unlocks e3): how many tools to keep loaded.
  // Receives the number asked for. Returns it limited to the range from 3 to 5.
  // Example: clamp(1) -> 3, clamp(8) -> 5, clamp(4) -> 4
  return keep;
}

function defers(tools: Record<string, number>): boolean {
  // TODO 5 of 10 (unlocks e2): must the definitions be deferred?
  // Receives the tools (name to the tokens of its definition). Returns true when there are 10 tools or more, or the definitions together are over 10000 tokens (10000 itself is fine).
  // Example: 9 tools of 100 tokens -> false; 10 tools -> true; two tools of 5000 and 5001 -> true
  return false;
}

function ranked(tools: Record<string, number>, usage: Record<string, number>, keep: number): string[] {
  // TODO 6 of 10 (unlocks e3): the tools to load first.
  // Receives the tools, the usage counts (a tool missing from `usage` has 0) and how many to keep. Returns that many tool names, the most used first and by name among equals.
  // Example: usage { b: 9, a: 9, c: 1 }, keep 2 -> ["a", "b"]
  return Object.keys(tools).slice(0, keep);
}

export function planLoading(tools: Record<string, number>, usage: Record<string, number>, keep = 4, searchTokens = 350) {
  log.debug("planLoading input", tools);
  const names = Object.keys(tools);
  if (!defers(tools)) return { search: false, load_now: names, deferred: [] as string[], tokens: names.reduce((a, t) => a + tools[t], 0) };
  const first = ranked(tools, usage, clamp(keep));
  return { search: true, load_now: first, deferred: names.filter((t) => !first.includes(t)), tokens: first.reduce((a, t) => a + tools[t], 0) + searchTokens };
}

export function chooseMechanism(consumers: number, counterpart: string, path: string): string | null {
  // TODO 7 of 10 (unlocks e4): how a capability is connected.
  // Receives the number of clients that will use it, the counterpart ("agent" or "tool") and the path ("fixed" or "model-chosen"). Decide in this order:
  // a counterpart that is an agent gives "agent-to-agent"; a fixed path gives "direct call in code"; otherwise "MCP server" for more than one client and "custom tool" for one.
  // Example: chooseMechanism(4, "tool", "model-chosen") -> "MCP server"
  return null;
}

export function authorize(tool: string, userScopes: string[], agentScopes: string[], required: Record<string, string>): string | null {
  // TODO 8 of 10 (unlocks e5): whose rights a tool call uses.
  // Receives the tool name, the scopes of the user, the scopes of the agent and `required` (tool to the scope it needs). Returns "deny: unknown tool" for a tool
  // that is not in `required`; then "deny: user lacks <scope>"; then "deny: agent lacks <scope>"; otherwise "allow". Both the user and the agent must hold the scope.
  // Example: scope "refunds:write" held by the agent only -> "deny: user lacks refunds:write"
  return null;
}

function verdict(team: string | undefined, request: Request, policy: Policy): [string, string] {
  // TODO 9 of 10 (unlocks e6): the decision and its reason.
  // Receives the team (undefined when the credential is unknown), the request (`model`, `tool` or null, `recent`) and the policy (`models`, `tools` and `limits` per team,
  // `routes`). Checks in this order and stops at the first that fails, returning ["deny", reason]: "unauthenticated" when there is no team; "model not allowed";
  // "tool not allowed" (only when a tool is named); "rate limited" when `recent` is at or over the team's limit. Otherwise ["allow", "routed to <name>"], where the name is
  // `policy.routes` for the model, or the model itself when it has no route.
  // Example: team "support", model "standard", no tool, recent 1, limit 2 -> ["allow", "routed to claude-sonnet-5-5"]
  return ["allow", ""];
}

function record(team: string | undefined, request: Request, decision: string): Record<string, string> {
  // TODO 10 of 10 (unlocks e7): the record kept of a decision.
  // Receives the team (undefined when unknown), the request and the decision. Returns { team, model, tool, decision }: the team or "unknown", the model, the tool or
  // "none", and the decision. Nothing else: no prompt and no credential.
  // Example: team undefined, model "deep", tool null, "deny" -> { team: "unknown", model: "deep", tool: "none", decision: "deny" }
  return {};
}

export function gateway(request: Request, policy: Policy) {
  log.debug("gateway input", request);
  const team = request.credential === null ? undefined : policy.credentials[request.credential];
  const [decision, reason] = verdict(team, request, policy);
  return { decision, reason, audit: record(team, request, decision) };
}
