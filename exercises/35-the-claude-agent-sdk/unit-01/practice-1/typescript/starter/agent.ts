// Code around the Claude Agent SDK: options, a permission callback, a hook and the message handling. See ../../statement.md.
export const READ_TOOLS = ["Read", "Grep", "Glob"];
export const EDIT_TOOLS = ["Edit", "Write"];
export const SAFE_COMMANDS = ["ls", "cat", "pytest"];
export const STATUS: Record<string, string> = { success: "done", error_max_turns: "max_turns", error_max_budget_usd: "budget", error_during_execution: "failed" };

export type Verdict = { behavior: "allow" } | { behavior: "deny"; message: string; interrupt: boolean };

export function decide(toolName: string, toolInput: Record<string, any>, projectDir: string, mode = "readonly"): any {
  // TODO: the permission policy as data: { behavior: "allow" } or { behavior: "deny", message, interrupt }.
  return null;
}

export function makeCanUseTool(projectDir: string, mode = "readonly"): any {
  // TODO: an async callback (toolName, toolInput) that turns decide() into the SDK's allow or deny result.
  return null;
}

export async function bashGuard(input: any): Promise<any> {
  // TODO: a PreToolUse hook that denies a Bash command that runs git push.
  return null;
}

export function buildOptions(projectDir: string, cliPath: string, mode = "readonly"): any {
  // TODO: the SDK options for a read-only (or edit) agent confined to the project; see the statement.
  return null;
}

export function summarize(messages: any[]): any {
  // TODO: fold the messages of one run into { status, text, tools, turns, cost, denied }.
  return null;
}

export async function runAgent(prompt: string, projectDir: string, cliPath: string, mode = "readonly"): Promise<any> {
  // TODO: run query() with buildOptions and summarize what it yields.
  return null;
}
