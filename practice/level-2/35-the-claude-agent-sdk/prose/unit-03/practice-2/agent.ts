// Code around the Claude Agent SDK: options, a permission callback, a hook and the message handling. See ../../statement.md.
import { posix as path } from "node:path";
import { query } from "@anthropic-ai/claude-agent-sdk";
import { logger } from "./logger.ts";
const log = logger("agent");

export const READ_TOOLS = ["Read", "Grep", "Glob"];
export const EDIT_TOOLS = ["Edit", "Write"];
export const SAFE_COMMANDS = ["ls", "cat", "pytest"];
export const STATUS: Record<string, string> = { success: "done", error_max_turns: "max_turns", error_max_budget_usd: "budget", error_during_execution: "failed" };

const absolute = (projectDir: string, p: string) => (p.startsWith("/") ? path.normalize(p).replace(/(.)\/$/, "$1") : path.normalize(path.join(projectDir, p)).replace(/(.)\/$/, "$1"));

function inside(projectDir: string, p: string): boolean {
  const full = absolute(projectDir, p), root = path.normalize(projectDir).replace(/(.)\/$/, "$1");
  return full === root || full.startsWith(root + "/");
}

export type Verdict = { behavior: "allow" } | { behavior: "deny"; message: string; interrupt: boolean };

function isSecretName(base: string): boolean {
  // TODO 1 of 9 (unlocks e2): does this file name hold secrets?
  // Receives a file name (no folders). Returns true when it starts with `.env` unless it is `.env.example`.
  // Example: isSecretName(".env.local") -> true, isSecretName(".env.example") -> false
  return false;
}
function isDangerous(command: string): boolean {
  // TODO 2 of 9 (unlocks e3): is this shell command dangerous enough to stop the run?
  // Receives a command. Returns true when it contains the word `sudo` (as a whole word) or the text `rm -rf`.
  // Example: isDangerous("pytest && sudo reboot") -> true, isDangerous("ls") -> false
  return false;
}
function isChained(command: string): boolean {
  // TODO 3 of 9 (unlocks e3): does this command chain, pipe, redirect or substitute?
  // Receives a command. Returns true when it contains `;`, `&`, `|`, `<`, `>`, a backtick or `$(`.
  // Example: isChained("ls | wc") -> true, isChained("ls -la") -> false
  return false;
}
function commandAllowed(command: string): boolean {
  // TODO 4 of 9 (unlocks e3): is the first word of the command one of SAFE_COMMANDS?
  // Receives a command. Returns true when its first word is in SAFE_COMMANDS; false for anything else, including an empty command.
  // Example: commandAllowed("  pytest -q") -> true, commandAllowed("python x.py") -> false
  return false;
}

/** The permission policy as plain data. */
export function decide(toolName: string, toolInput: Record<string, any>, projectDir: string, mode = "readonly"): Verdict {
  log.debug("decide input", toolInput);
  const deny = (message: string, interrupt = false): Verdict => ({ behavior: "deny", message, interrupt });
  if (READ_TOOLS.includes(toolName) || EDIT_TOOLS.includes(toolName)) {
    const p: string | undefined = toolInput.file_path || toolInput.path;
    if (EDIT_TOOLS.includes(toolName) && mode !== "edit") return deny("Edits are not allowed in readonly mode");
    if (p !== undefined) {
      if (!inside(projectDir, p)) return deny(`${p} is outside the project`);
      const full = absolute(projectDir, p);
      const base = path.basename(full);
      if (isSecretName(base)) return deny(`${base} holds secrets and is never read`);
      if (EDIT_TOOLS.includes(toolName) && full.split("/").includes(".git")) return deny(`${p} is inside .git`);
    }
    return { behavior: "allow" };
  }
  if (toolName === "Bash") {
    const command: string = toolInput.command ?? "";
    if (isDangerous(command)) return deny("Dangerous command", true);
    if (isChained(command)) return deny("Command not allowed: no chaining or redirection");
    if (commandAllowed(command)) return { behavior: "allow" };
    return deny("Command not allowed: only ls, cat and pytest");
  }
  return deny(`${toolName} is not allowed`);
}

export function makeCanUseTool(projectDir: string, mode = "readonly") {
  // TODO 5 of 9 (unlocks m1 and e7): the callback the SDK asks for a tool that is not already decided.
  // Receives the project directory and the mode. Returns an async function (toolName, toolInput) that turns `decide()` into
  // { behavior: "allow", updatedInput: toolInput } or { behavior: "deny", message, interrupt }.
  // Example: for { behavior: "deny", message: "no", interrupt: false } it returns { behavior: "deny", message: "no", interrupt: false }
  return async (toolName: string, toolInput: Record<string, unknown>) => ({ behavior: "deny" as const, message: "not written yet", interrupt: false });
}

function isPush(command: string): boolean {
  // TODO 6 of 9 (unlocks e4): does this command run `git push`?
  // Receives a command. Returns true when it has the two words `git` and `push` with any white space between them, as whole words.
  // Example: isPush("echo ok && git  push origin") -> true, isPush("git pushd") -> false, isPush("legit push") -> false
  return false;
}

/** A PreToolUse hook: nothing is pushed from an agent. */
export async function bashGuard(input: any) {
  if (isPush(input?.tool_input?.command ?? "")) {
    return { hookSpecificOutput: { hookEventName: "PreToolUse" as const, permissionDecision: "deny" as const, permissionDecisionReason: "Nothing is pushed from an agent" } };
  }
  return {};
}

export function buildOptions(projectDir: string, cliPath: string, mode = "readonly") {
  // TODO 7 of 9 (unlocks e1): the SDK options of the agent.
  // Receives the project directory, the CLI path and the mode. Returns an options object with pathToClaudeCodeExecutable, cwd (the project),
  // tools (the read tools, "Bash", and the edit tools in "edit" mode), no allowedTools, "Bash(rm *)" disallowed, maxTurns 6, maxBudgetUsd 0.5,
  // permissionMode "default", no settingSources, canUseTool from makeCanUseTool and a PreToolUse hook for "Bash" (bashGuard).
  // Example: buildOptions("/proj", "/bin/cli").maxTurns -> 6
  return { pathToClaudeCodeExecutable: cliPath, cwd: projectDir };
}

function countDenied(content: any[]): number {
  // TODO 8 of 9 (unlocks e5 and e7): how many tool results in this list of blocks are errors?
  // Receives a list of content blocks. Returns how many have type "tool_result" and a true is_error (a missing flag is not an error).
  // Example: [{ type: "tool_result", is_error: true }, { type: "tool_result", is_error: false }] -> 1
  return 0;
}
function statusOf(subtype: string): string {
  // TODO 9 of 9 (unlocks e5): the course status of a result subtype.
  // Receives a result subtype. Returns STATUS[subtype], or the subtype itself when it is not in STATUS.
  // Example: statusOf("error_max_turns") -> "max_turns", statusOf("something_new") -> "something_new"
  return "";
}

export function summarize(messages: any[]) {
  let text = "", denied = 0, result: any = null;
  const tools: string[] = [];
  for (const m of messages) {
    if (m.type === "assistant") {
      for (const block of m.message?.content ?? []) {
        if (block.type === "text") text = block.text;
        else if (block.type === "tool_use") tools.push(block.name);
      }
    } else if (m.type === "user" && Array.isArray(m.message?.content)) {
      denied += countDenied(m.message.content);
    } else if (m.type === "result") {
      result = m;
    }
  }
  if (result === null) return { status: "incomplete", text, tools, turns: 0, cost: 0.0, denied };
  return { status: statusOf(result.subtype), text: result.result || text, tools, turns: result.num_turns, cost: result.total_cost_usd ?? 0.0, denied };
}

export async function runAgent(prompt: string, projectDir: string, cliPath: string, mode = "readonly") {
  const messages: any[] = [];
  try {
    for await (const m of query({ prompt, options: buildOptions(projectDir, cliPath, mode) as any })) messages.push(m);
  } catch (error) {
    // After an error result (turn limit, budget) a single-shot query() yields the result and then raises, because the process exits with a
    // nonzero code. That is not a failure of the run; a crash before any result message is, and is not hidden.
    if (!messages.some((m) => m.type === "result")) throw error;
  }
  return summarize(messages);
}
