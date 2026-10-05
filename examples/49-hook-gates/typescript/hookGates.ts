// Hooks in the Agent SDK, offline: a gate before a tool, a normaliser after another, and a command hook run as the process it is.
//
// The SDK starts the Claude Code binary; here the binary is `harness/fake_claude.py`, which replays a script, so no model is called and no network is used.
// The refund and order tools are scripted: their output is what the stand-in reports when a call is allowed.
// `@anthropic-ai/claude-agent-sdk` 0.3.287, checked on 2026-10-03 against the hooks pages of the Claude Code documentation.
import { spawnSync } from "node:child_process";
import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { query } from "@anthropic-ai/claude-agent-sdk";
import { logger } from "./logger.ts";
const log = logger("hook_gates");

export const FAKE = new URL("../../../harness/fake_claude.py", import.meta.url).pathname;
const GUARD = new URL("../guard_hook.py", import.meta.url).pathname;
export const ran: string[] = [];

/** The decision for a refund amount: small goes through, a middle one needs a person, a large one is refused. */
export function tier(amount: unknown): [string, string] {
  if (typeof amount !== "number" || !Number.isFinite(amount) || amount <= 0) return ["deny", "the amount is missing or invalid"];
  if (amount <= 200) return ["allow", "within the automatic limit"];
  if (amount <= 500) return ["ask", "needs a person's approval"];
  return ["deny", "above the limit of 500, escalate to a person"];
}

async function refundGate(input: any) {
  const [decision, reason] = tier(input.tool_input.amount);
  ran.push(`PreToolUse ${input.tool_name} amount=${input.tool_input.amount ?? "None"} -> ${decision}`);
  return { hookSpecificOutput: { hookEventName: "PreToolUse" as const, permissionDecision: decision as "allow" | "ask" | "deny", permissionDecisionReason: reason } };
}

export async function readableOrder(input: any) {
  const order = JSON.parse(input.tool_response);
  order.created = new Date(order.created * 1000).toISOString().slice(0, 10);
  order.status = ({ 0: "pending", 1: "approved", 2: "declined" } as Record<number, string>)[order.status] ?? "unknown";
  ran.push(`PostToolUse ${input.tool_name} -> readable output`);
  return { hookSpecificOutput: { hookEventName: "PostToolUse" as const, updatedToolOutput: JSON.stringify(order) } };
}

async function personSaysNo(toolName: string, toolInput: Record<string, any>) {
  if (toolName === "get_order") return { behavior: "allow" as const, updatedInput: toolInput };
  ran.push(`a person is asked about ${toolName} amount=${toolInput.amount} and declines`);
  return { behavior: "deny" as const, message: "A person declined this refund" };
}

function options(cwd: string) {
  return {
    pathToClaudeCodeExecutable: FAKE, cwd, permissionMode: "default" as const,
    settingSources: [] as ("user" | "project" | "local")[], maxTurns: 8, canUseTool: personSaysNo,
    hooks: { PreToolUse: [{ matcher: "process_refund", hooks: [refundGate] }], PostToolUse: [{ matcher: "get_order", hooks: [readableOrder] }] },
  };
}

const step = (id: string, name: string, input: Record<string, unknown>, output: string) => ({ tool: { id, name, input, output } });

const SCRIPT = [
  step("t1", "get_order", { order: "A-7" }, JSON.stringify({ order: "A-7", created: 1700000000, status: 1, total_cents: 12950 })),
  step("t2", "process_refund", { order: "A-7", amount: 50 }, "refund R-1 created"),
  step("t3", "process_refund", { order: "A-7", amount: 350 }, "refund R-2 created"),
  step("t4", "process_refund", { order: "A-7", amount: 900 }, "refund R-3 created"),
  step("t5", "process_refund", { order: "A-7" }, "refund R-4 created"),
  { result: { subtype: "success", result: "done", cost: 0.02, turns: 6 } },
];

/** Run the command hook as Claude Code does: JSON on standard input, then read the exit code and standard error. */
export function runGuard(event: string): [number, string] {
  const done = spawnSync("python3", [GUARD], { input: event, encoding: "utf8" });
  return [done.status ?? -1, done.stderr.trim()];
}

async function main() {
  const workdir = mkdtempSync(join(tmpdir(), "hooks-"));
  const script = join(workdir, "script.json");
  writeFileSync(script, JSON.stringify({ session_id: "demo", turns: [SCRIPT] }));
  process.env.FAKE_CLAUDE_SCRIPT = script;
  process.env.FAKE_CLAUDE_RECORD = join(workdir, "record.jsonl");
  const names: Record<string, string> = {};
  for await (const message of query({ prompt: "Refund order A-7", options: options(workdir) as any }) as AsyncIterable<any>) {
    if (message.type === "assistant") {
      for (const block of message.message.content) if (block.type === "tool_use") names[block.id] = `${block.name} ${JSON.stringify(block.input)}`;
    } else if (message.type === "user" && Array.isArray(message.message.content)) {
      for (const block of message.message.content) if (block.type === "tool_result") console.log(`${names[block.tool_use_id]}\n  the model sees: ${block.content}`);
    } else if (message.type === "result") console.log(`done: ${message.subtype}`);
  }
  console.log("\nwhat ran in this process, in order:");
  for (const line of ran) console.log(" ", line);
  console.log("\nthe command hook, as a process:");
  for (const event of [{ tool_name: "Bash", tool_input: { command: "git push origin main" } }, { tool_name: "Bash", tool_input: { command: "git status" } }, "{not json"]) {
    const [code, reason] = runGuard(typeof event === "string" ? event : JSON.stringify(event));
    console.log(`  '${typeof event === "string" ? event : event.tool_input.command}': exit ${code}${reason ? ", " + reason : ""}`);
  }
}

if (import.meta.main) await main();
