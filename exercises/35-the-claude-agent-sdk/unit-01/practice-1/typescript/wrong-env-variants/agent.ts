// Code around the Claude Agent SDK: options, a permission callback, a hook and the message handling. See ../../statement.md.
import { posix as path } from "node:path";
import { query } from "@anthropic-ai/claude-agent-sdk";

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

/** The permission policy as plain data. */
export function decide(toolName: string, toolInput: Record<string, any>, projectDir: string, mode = "readonly"): Verdict {
  const deny = (message: string, interrupt = false): Verdict => ({ behavior: "deny", message, interrupt });
  if (READ_TOOLS.includes(toolName) || EDIT_TOOLS.includes(toolName)) {
    const p: string | undefined = toolInput.file_path || toolInput.path;
    if (EDIT_TOOLS.includes(toolName) && mode !== "edit") return deny("Edits are not allowed in readonly mode");
    if (p !== undefined) {
      if (!inside(projectDir, p)) return deny(`${p} is outside the project`);
      const full = absolute(projectDir, p);
      const base = path.basename(full);
      if (base === ".env") return deny(`${base} holds secrets and is never read`);
      if (EDIT_TOOLS.includes(toolName) && full.split("/").includes(".git")) return deny(`${p} is inside .git`);
    }
    return { behavior: "allow" };
  }
  if (toolName === "Bash") {
    const command: string = toolInput.command ?? "";
    if (/\bsudo\b/.test(command) || command.includes("rm -rf")) return deny("Dangerous command", true);
    if (/[;&|<>`]|\$\(/.test(command)) return deny("Command not allowed: no chaining or redirection");
    if (SAFE_COMMANDS.includes(command.trim().split(/\s+/)[0])) return { behavior: "allow" };
    return deny("Command not allowed: only ls, cat and pytest");
  }
  return deny(`${toolName} is not allowed`);
}

export function makeCanUseTool(projectDir: string, mode = "readonly") {
  return async (toolName: string, toolInput: Record<string, unknown>) => {
    const verdict = decide(toolName, toolInput, projectDir, mode);
    return verdict.behavior === "allow" ? { behavior: "allow" as const, updatedInput: toolInput } : { behavior: "deny" as const, message: verdict.message, interrupt: verdict.interrupt };
  };
}

/** A PreToolUse hook: nothing is pushed from an agent. */
export async function bashGuard(input: any) {
  if (/\bgit\s+push\b/.test(input?.tool_input?.command ?? "")) {
    return { hookSpecificOutput: { hookEventName: "PreToolUse" as const, permissionDecision: "deny" as const, permissionDecisionReason: "Nothing is pushed from an agent" } };
  }
  return {};
}

export function buildOptions(projectDir: string, cliPath: string, mode = "readonly") {
  return {
    pathToClaudeCodeExecutable: cliPath, cwd: projectDir, tools: [...READ_TOOLS, "Bash", ...(mode === "edit" ? EDIT_TOOLS : [])], allowedTools: [] as string[],
    disallowedTools: ["Bash(rm *)"], maxTurns: 6, maxBudgetUsd: 0.5, permissionMode: "default" as const, settingSources: [] as ("user" | "project" | "local")[],
    canUseTool: makeCanUseTool(projectDir, mode), hooks: { PreToolUse: [{ matcher: "Bash", hooks: [bashGuard] }] },
  };
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
      denied += m.message.content.filter((b: any) => b.type === "tool_result" && b.is_error).length;
    } else if (m.type === "result") {
      result = m;
    }
  }
  if (result === null) return { status: "incomplete", text, tools, turns: 0, cost: 0.0, denied };
  return { status: STATUS[result.subtype] ?? result.subtype, text: result.result || text, tools, turns: result.num_turns, cost: result.total_cost_usd ?? 0.0, denied };
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
