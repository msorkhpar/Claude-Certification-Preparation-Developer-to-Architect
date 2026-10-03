// Hooks with the Agent SDK and Claude Code: a refund gate, output normalisation, the options that register them, and a command hook. See ../../statement.md.
const AUTO_LIMIT = 200;
const ASK_LIMIT = 500;
const STATUS: Record<number, string> = { 0: "pending", 1: "approved", 2: "declined" };
const BLOCKED_COMMANDS: Array<[RegExp, string]> = [[/\bgit\s+push\b/, "Nothing is pushed from an agent session"], [/\brm\s+-rf\b/, "Recursive deletes are not allowed"]];

const answer = (event: string, fields: Record<string, unknown>) => ({ hookSpecificOutput: { hookEventName: event, ...fields } });

// PreToolUse for process_refund: allow small refunds, ask a person for middle ones, deny large ones, and fail closed on a bad amount.
export async function preRefund(input: any, toolUseId?: string, context?: unknown): Promise<any> {
  if (input?.tool_name !== "process_refund") return {};
  const amount = input?.tool_input?.amount;
  if (typeof amount !== "number" || !Number.isFinite(amount) || amount <= 0) {
    return answer("PreToolUse", { permissionDecision: "deny", permissionDecisionReason: "The refund amount is missing or invalid" });
  }
  if (amount <= AUTO_LIMIT) return answer("PreToolUse", { permissionDecision: "allow", permissionDecisionReason: `${amount} is within the automatic limit of ${AUTO_LIMIT}` });
  if (amount <= ASK_LIMIT) return answer("PreToolUse", { permissionDecision: "ask", permissionDecisionReason: `${amount} needs a person's approval` });
  return answer("PreToolUse", { permissionDecision: "deny", permissionDecisionReason: `${amount} is above the limit of ${ASK_LIMIT}; escalate the case to a person` });
}

const isNumber = (v: unknown): v is number => typeof v === "number" && Number.isFinite(v);
const isoDate = (value: number) => new Date(value > 1e11 ? value : value * 1000).toISOString().slice(0, 10);

// PostToolUse: give the model readable values in place of epoch seconds, numeric status codes and cents.
export async function postNormalise(input: any, toolUseId?: string, context?: unknown): Promise<any> {
  const raw = input?.tool_response;
  let data: any = raw;
  if (typeof raw === "string") {
    try {
      data = JSON.parse(raw);
    } catch {
      return {};
    }
  }
  if (data === null || typeof data !== "object" || Array.isArray(data)) return {};
  const out: Record<string, unknown> = { ...data };
  let changed = false;
  if (isNumber(out.created)) { out.created = isoDate(out.created); changed = true; }
  if (Number.isInteger(out.status)) { out.status = STATUS[out.status as number] ?? "unknown"; changed = true; }
  if (Number.isInteger(out.amount_cents)) {
    const cents = out.amount_cents as number;
    delete out.amount_cents;
    out.amount = `${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, "0")}`;
    changed = true;
  }
  return changed ? answer("PostToolUse", { updatedToolOutput: JSON.stringify(out) }) : {};
}

// Options that register both hooks; a matcher is a tool name pattern, never a path.
export function buildOptions(cwd: string, cliPath?: string): any {
  return {
    cwd, allowedTools: ["get_order", "process_refund"], permissionMode: "default", settingSources: [], maxTurns: 6,
    hooks: {
      PreToolUse: [{ matcher: "process_refund", hooks: [preRefund] }],
      PostToolUse: [{ matcher: "get_order|get_refund", hooks: [postNormalise], timeout: 5 }],
    },
    ...(cliPath ? { pathToClaudeCodeExecutable: cliPath } : {}),
  };
}

// What a Claude Code command hook does with its stdin: exit 2 blocks (the reason goes to stderr), 0 lets the call go on, and bad input blocks.
export function commandHook(stdinText: string): { exit: number; stderr: string } {
  let data: any = null;
  try {
    data = JSON.parse(stdinText);
  } catch {
    data = null;
  }
  if (data === null || typeof data !== "object" || Array.isArray(data)) {
    return { exit: 2, stderr: "The hook input is not valid JSON, so the call is blocked to be safe" };
  }
  if (data.tool_name !== "Bash") return { exit: 0, stderr: "" };
  const command = String(data.tool_input?.command ?? "");
  for (const [pattern, reason] of BLOCKED_COMMANDS) {
    if (pattern.test(command)) return { exit: 2, stderr: reason };
  }
  return { exit: 0, stderr: "" };
}

// The hooks block of .claude/settings.json that runs the command hook before every Bash call.
export function settingsHooks(script = "python3 .claude/hooks/guard.py", timeout = 10): any {
  return { hooks: { PreToolUse: [{ matcher: "Bash", hooks: [{ type: "command", command: script, timeout }] }] } };
}
