// Distributing tools across agents: scoped tool sets, the tool choice of a turn, a check of the reply and the authorisation of a call. See ../../statement.md.

const NO_FORCING = new Set(["claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"]); // models whose API rejects tool_choice any and tool, as read on 2026-10-03

export function assignTools(roles: Record<string, { specialisation?: string[]; extra?: string[] }>, catalog: Array<Record<string, any>>, budget = 5): any {
  // TODO: give each role the tools of its specialisation, plus what it is explicitly granted; refuse a set that is too large.
  return null;
}

export function planTurn(model: string, need: string, tools: string[], forced: string | null = null, manualThinking = false): any {
  // TODO: the tool_choice and tool list of one request, with the portable fallback where forcing is not accepted.
  return null;
}

export function cacheImpact(previous: Record<string, any> | null, next: Record<string, any>): any {
  // TODO: what does a change between two requests cost in prompt caching: none, messages or all?
  return null;
}

export function checkTurn(blocks: Array<Record<string, any>>, need: string, forced: string | null = null): any {
  // TODO: compare a reply with the call that was required: ok, missed_call or wrong_tool.
  return null;
}

export function authorize(call: Record<string, any>, policy: Record<string, any>, approvals: Iterable<string>): any {
  // TODO: decide a call to a tool that cannot be undone, in the tool layer.
  return null;
}
