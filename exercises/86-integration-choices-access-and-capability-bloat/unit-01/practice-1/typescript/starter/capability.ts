/** Capability design: what a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides. See ../../statement.md. */

export type Agent = { holds: string[]; needs: string[]; used: Record<string, number> };
export type Catalog = Record<string, { access: string; tokens: number }>;
export type Request = { credential: string | null; model: string; tool: string | null; recent: number };
export type Policy = { credentials: Record<string, string>; models: Record<string, string[]>; tools: Record<string, string[]>; limits: Record<string, number>; routes: Record<string, string> };
const RISKY = new Set(["money", "destroy"]);

export function audit(agent: Agent, catalog: Catalog): any {
  // TODO: the tools to remove (held, not needed), the risky ones among them, the needed tools the agent lacks and the held, needed tools nobody used.
  return null;
}

export function planLoading(tools: Record<string, number>, usage: Record<string, number>, keep = 4, searchTokens = 350): any {
  // TODO: load everything for a small set, otherwise keep the most used tools loaded and defer the rest behind a search tool.
  return null;
}

export function chooseMechanism(consumers: number, counterpart: string, path: string): string | null {
  // TODO: agent-to-agent, direct call in code, MCP server or custom tool.
  return null;
}

export function authorize(tool: string, userScopes: string[], agentScopes: string[], required: Record<string, string>): string | null {
  // TODO: allow, or deny with the reason, when the user's rights and the agent's rights do not both cover the tool.
  return null;
}

export function gateway(request: Request, policy: Policy): any {
  // TODO: authenticate, check the model, the tool and the rate in that order, route an allowed request and keep a record of every decision.
  return null;
}
