// Hooks with the Agent SDK and Claude Code: a refund gate, output normalisation, the options that register them, and a command hook. See ../../statement.md.
import { logger } from "./logger.ts";
const log = logger("hooks");
const AUTO_LIMIT = 200;
const ASK_LIMIT = 500;
const STATUS: Record<number, string> = { 0: "pending", 1: "approved", 2: "declined" };
const BLOCKED_COMMANDS: Array<[RegExp, string]> = [[/\bgit\s+push\b/, "Nothing is pushed from an agent session"], [/\brm\s+-rf\b/, "Recursive deletes are not allowed"]];

const answer = (event: string, fields: Record<string, unknown>) => ({ hookSpecificOutput: { hookEventName: event, ...fields } });

// PreToolUse for process_refund: allow small refunds, ask a person for middle ones, deny large ones, and fail closed on a bad amount.
export async function preRefund(input: any, toolUseId?: string, context?: unknown): Promise<any> {
  log.debug("preRefund input", input);
  // TODO 2 of 10 (finish this to pass e2): the tool filter. When the tool name is not process_refund, return an empty
  //   answer so that the call goes on untouched. Example: a get_order call with an amount of 9999 -> {}.
  const amount = input?.tool_input?.amount;
  // TODO 3 of 10 (finish this to pass e2): the fail-closed check. When the amount is missing, a boolean, not a number,
  //   not finite or not above zero, return a deny answer with the reason "The refund amount is missing or invalid".
  //   Example: amount "12" or True or 0 -> deny.
  if (amount <= AUTO_LIMIT) return answer("PreToolUse", { permissionDecision: "allow", permissionDecisionReason: `${amount} is within the automatic limit of ${AUTO_LIMIT}` });
  // TODO 1 of 10 (finish this to pass m1, e1): the two upper tiers of the refund gate (the allow tier above is written,
  //   to show the shape of the answer). `amount` is above AUTO_LIMIT here. Return the hook answer with permissionDecision
  //   ask up to ASK_LIMIT, otherwise deny, each with a permissionDecisionReason that names the amount. Example: 200 ->
  //   allow, 200.01 -> ask, 500 -> ask, 501 -> deny.
  return {};
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
  // TODO 4 of 10 (finish this to pass e3): the date and the status word. When `created` is a number (not a boolean),
  //   replace it with the text from the date helper; when `status` is an integer (not a boolean), replace it with its word
  //   from STATUS, or "unknown". Example: created 1700000000 -> "2023-11-14", status 1 -> "approved", status 9 ->
  //   "unknown".
  // TODO 5 of 10 (finish this to pass e3): the amount. When `amount_cents` is an integer (not a boolean), remove it and
  //   add `amount` as text with two decimals; leave a value that is not an integer untouched. Example: 12950 -> amount
  //   "129.50", and 5 -> "0.05".
  // TODO 6 of 10 (finish this to pass e4): the idempotence rule. When nothing changed, return an empty answer instead of
  //   a replacement. Example: {"status": "approved"} -> {} (and output that is not JSON is already left alone above).
  return answer("PostToolUse", { updatedToolOutput: JSON.stringify(out) });
}

// Options that register both hooks; a matcher is a tool name pattern, never a path.
export function buildOptions(cwd: string, cliPath?: string): any {
  return {
    cwd, allowedTools: ["get_order", "process_refund"], permissionMode: "default", settingSources: [], maxTurns: 6,
    // TODO 7 of 10 (finish this to pass e5, e8): the hook registration. Fill the `hooks` map of the options: a
    //   HookMatcher (Python) or {matcher, hooks, timeout} object (TypeScript) for PreToolUse with the matcher
    //   process_refund and the refund gate, and one for PostToolUse with the matcher get_order|get_refund and the
    //   normaliser, each with a timeout of 5.
    hooks: { PreToolUse: [{ hooks: [preRefund] }], PostToolUse: [{ hooks: [postNormalise] }] },
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
  // TODO 8 of 10 (finish this to pass e6): the fail-closed rule. When the parsed input is not an object, return exit 2
  //   with the reason "The hook input is not valid JSON, so the call is blocked to be safe". Example: "not json" -> {exit:
  //   2, stderr: ...}.
  if (data.tool_name !== "Bash") return { exit: 0, stderr: "" };
  const command = String(data.tool_input?.command ?? "");
  // TODO 9 of 10 (finish this to pass e6): the command rules. For each (pattern, reason) of BLOCKED_COMMANDS, when the
  //   pattern matches the command return exit 2 with the reason. Example: "git push origin main" -> {exit: 2, stderr:
  //   "Nothing is pushed from an agent session"}.
  return { exit: 0, stderr: "" };
}

// The hooks block of .claude/settings.json that runs the command hook before every Bash call.
export function settingsHooks(script = "python3 .claude/hooks/guard.py", timeout = 10): any {
  // TODO 10 of 10 (finish this to pass e7): the settings block. Return the hooks block for .claude/settings.json:
  //   PreToolUse with one entry whose matcher is Bash and whose hooks list has one command hook carrying the script and
  //   the timeout. Example: settings_hooks("sh guard.sh", 3) -> a command hook with command "sh guard.sh" and timeout 3.
  return { hooks: { PreToolUse: [{ matcher: "", hooks: [{ type: "command", command: script }] }] } };
}
