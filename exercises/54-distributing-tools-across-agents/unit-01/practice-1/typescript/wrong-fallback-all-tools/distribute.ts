// Distributing tools across agents: scoped tool sets, the tool choice of a turn, a check of the reply and the authorisation of a call. See ../../statement.md.

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
    const names = catalog.filter((t) => shares(t) && !t.irreversible).map((t) => t.name);
    for (const extra of spec.extra ?? []) {
      const tool = byName.get(extra);
      if (tool === undefined) throw new Error(`role ${role}: unknown tool ${extra}`);
      if (!(shares(tool) || tool.scoped || tool.irreversible)) throw new Error(`role ${role}: ${extra} is outside the specialisation and is not a scoped cross-role tool`);
      if (!names.includes(extra)) names.push(extra);
    }
    if (names.length > budget) throw new Error(`role ${role} has ${names.length} tools, more than the budget of ${budget}`);
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
  if (!rejects) {
    const choice = need === "any" ? { type: "any" } : { type: "tool", name: forced };
    return { tool_choice: choice, tools: [...tools], strict: false, verify_call: false };
  }
  return { tool_choice: { type: "auto" }, tools: [...tools], strict: true, verify_call: true };
}

/** What a change between two requests costs in prompt caching: none, messages (tool_choice changed) or all (the tools changed). */
export function cacheImpact(previous: Record<string, any> | null, next: Record<string, any>): string {
  if (previous === null) return "none";
  if (JSON.stringify(previous.tools) !== JSON.stringify(next.tools)) return "all";
  if (JSON.stringify(previous.tool_choice) !== JSON.stringify(next.tool_choice)) return "messages";
  return "none";
}

/** Compare a reply with the call that was required: ok, missed_call or wrong_tool. */
export function checkTurn(blocks: Array<Record<string, any>>, need: string, forced: string | null = null): string {
  if (need !== "any" && need !== "named") return "ok";
  const calls = blocks.filter((b) => b.type === "tool_use");
  if (calls.length === 0) return "missed_call";
  if (need === "named" && calls[0].name !== forced) return "wrong_tool";
  return "ok";
}

function answer(allowed: boolean, code: string, message: string, escalate = false) {
  return { allowed, code, message, escalate };
}

/** Decide a call to a tool that cannot be undone, in the tool layer, whatever the model says. */
export function authorize(call: Record<string, any>, policy: Record<string, any>, approvals: Iterable<string>) {
  const rule = policy.tools?.[call.tool];
  if (rule === undefined) return answer(false, "unknown_tool", `${call.tool} is not an allowed tool`);
  const cap = rule.cap ?? null;
  const amount = call.amount;
  if (cap !== null && (typeof amount !== "number" || !Number.isInteger(amount) || amount <= 0)) return answer(false, "bad_amount", "the amount must be a positive whole number");
  if (call.verified_customer === null || call.verified_customer === undefined || call.customer !== call.verified_customer) return answer(false, "not_owner", "the call is not for the verified customer");
  if (cap !== null && amount > cap) return answer(false, "over_cap", `${amount} is above the limit of ${cap}: send this to a person`, true);
  if (rule.irreversible && !new Set(approvals).has(call.id)) return answer(false, "needs_approval", "this call cannot be undone: a person must approve it first", true);
  return answer(true, "ok", "allowed");
}
