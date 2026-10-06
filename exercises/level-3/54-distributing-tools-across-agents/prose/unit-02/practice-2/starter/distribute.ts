// Distributing tools across agents: scoped tool sets, the tool choice of a turn, a check of the reply and the authorisation of a call. See ../../statement.md.
import { logger } from "./logger.ts";
const log = logger("distribute");

const NO_FORCING = new Set(["claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"]); // models whose API rejects tool_choice any and tool, as read on 2026-10-03

type Tool = { name: string; tags?: string[]; scoped?: boolean; irreversible?: boolean };

/** Give each role the tools of its specialisation, plus what it is explicitly granted, and refuse a set that is too large. */
export function assignTools(roles: Record<string, { specialisation?: string[]; extra?: string[] }>, catalog: Tool[], budget = 5): Record<string, string[]> {
  const byName = new Map<string, Tool>();
  for (const tool of catalog) {
    if (byName.has(tool.name)) throw new Error(`duplicate tool name: ${tool.name}`);
    byName.set(tool.name, tool);
  }
  const result: Record<string, string[]> = {};
  for (const [role, spec] of Object.entries(roles)) {
    const tags = new Set(spec.specialisation ?? []);
    if (tags.size === 0) throw new Error(`role ${role} has no specialisation`);
    const shares = (tool: Tool) => (tool.tags ?? []).some((t) => tags.has(t));
    // TODO 1 of 8 (finish this to pass m1, e2): the tools of a role. Receives the catalog and the role's tags. Return
    //   the names, in catalog order, of the tools that share a tag with the role and are not irreversible (an irreversible
    //   tool is only given by an explicit grant). Example: tags {billing}, catalog [lookup(billing), refund(billing,
    //   irreversible)] -> [lookup].
    const names = [...byName.keys()];
    for (const extra of spec.extra ?? []) {
      const tool = byName.get(extra);
      if (tool === undefined) throw new Error(`role ${role}: unknown tool ${extra}`);
      if (!(shares(tool) || tool.scoped || tool.irreversible)) throw new Error(`role ${role}: ${extra} is outside the specialisation and is not a scoped cross-role tool`);
      if (!names.includes(extra)) names.push(extra);
    }
    // TODO 2 of 8 (finish this to pass e1): the budget. When the role would hold more tools than the budget, refuse with
    //   an error that names the role, the count and the budget. Example: 6 tools, budget 5 -> raises; exactly 5 is fine.
    result[role] = names;
  }
  return result;
}

/** The tool_choice and tool list of one request, with the portable fallback where forcing is not accepted. */
export function planTurn(model: string, need: string, tools: string[], forced: string | null = null, manualThinking = false): Record<string, any> {
  if (!["free", "none", "any", "named"].includes(need)) throw new Error(`unknown need: ${need}`);
  if (need === "named" && (forced === null || !tools.includes(forced))) throw new Error("a named choice needs a tool from the list");
  const rejects = NO_FORCING.has(model) || manualThinking;
  if (need === "free") return { tool_choice: { type: "auto" }, tools: [...tools], strict: false, verify_call: false };
  if (need === "none") return { tool_choice: { type: "none" }, tools: [...tools], strict: false, verify_call: false };
  // TODO 3 of 8 (finish this to pass e3): the choice for `any` and `named`. Receives the model, the need, the tools, the
  //   forced name and `rejects`. When the model accepts forcing, return tool_choice {type: any} or {type: tool, name}, the
  //   tools, strict false, verify_call false. When it rejects forcing, return tool_choice auto, the tools (all for any,
  //   only the named one for named), strict true and verify_call true. Example: sonnet, named, forced lookup -> auto,
  //   [lookup], strict, verify.
  return { tool_choice: { type: "auto" }, tools: [...tools], strict: false, verify_call: false };
}

/** What a change between two requests costs in prompt caching: none, messages (tool_choice changed) or all (the tools changed). */
export function cacheImpact(previous: Record<string, any> | null, next: Record<string, any>): string {
  if (previous === null) return "none";
  // TODO 4 of 8 (finish this to pass e4): the cost in prompt caching. Receives the previous request and the new one (the
  //   first request has no previous). Return all when the tools differ, otherwise messages when the tool_choice differs,
  //   otherwise none. Example: same tools, auto then {type: any} -> messages.
  return "none";
}

/** Compare a reply with the call that was required: ok, missed_call or wrong_tool. */
export function checkTurn(blocks: Array<Record<string, any>>, need: string, forced: string | null = null): string {
  if (need !== "any" && need !== "named") return "ok";
  const calls = blocks.filter((b) => b.type === "tool_use");
  if (calls.length === 0) return "missed_call";
  // TODO 5 of 8 (finish this to pass e5): the check of a named call. When the need is named and the first tool call is
  //   not the forced tool, return wrong_tool. Example: need named, forced lookup, first call refund -> wrong_tool.
  return "ok";
}

function answer(allowed: boolean, code: string, message: string, escalate = false) {
  return { allowed, code, message, escalate };
}

/** Decide a call to a tool that cannot be undone, in the tool layer, whatever the model says. */
export function authorize(call: Record<string, any>, policy: Record<string, any>, approvals: Iterable<string>) {
  log.debug("authorize input", call);
  // TODO 6 of 8 (finish this to pass e6): the policy lookup. Find the rule of the called tool in the policy; when the
  //   policy does not name the tool, return the answer (allowed false, code unknown_tool, a message that names the tool).
  //   Example: tool delete_all, policy without it -> unknown_tool.
  const rule = policy.tools?.[call.tool] ?? {};
  const cap = rule.cap ?? null;
  const amount = call.amount;
  // TODO 7 of 8 (finish this to pass e6): the amount and the owner. When the tool has a cap and the amount is missing, a
  //   boolean, a decimal or not above zero, return bad_amount. When there is no verified customer or the call's customer
  //   is another, return not_owner. Example: amount 0 -> bad_amount; customer C2 while C1 is verified -> not_owner.
  // TODO 8 of 8 (finish this to pass e7): the cap and the approval. When the amount is above the cap, return over_cap
  //   with escalate true (an amount equal to the cap is allowed). When the tool is irreversible and the call's id is not
  //   among the approvals, return needs_approval with escalate true. Example: cap 500, amount 501 -> over_cap, even when
  //   approved.
  return answer(true, "ok", "allowed");
}
