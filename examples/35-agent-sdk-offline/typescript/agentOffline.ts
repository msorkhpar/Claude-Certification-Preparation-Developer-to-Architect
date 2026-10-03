// The Agent SDK driven offline: a custom tool, a hook and a permission callback, against a scripted stand-in for the Claude Code binary.
//
// The Agent SDK is a library around the Claude Code binary: it starts the binary, translates the options into flags, and answers the
// binary's control requests (hooks, permission questions, calls to your in-process tools). Here the binary is `harness/fake_claude.py`,
// which speaks the same stream-json protocol and replays a script, so no model is called and no network is used.
// `@anthropic-ai/claude-agent-sdk` 0.3.287, checked on 2026-10-03 against the Agent SDK pages of the Claude Code documentation.
import { mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { createSdkMcpServer, query, tool } from "@anthropic-ai/claude-agent-sdk";
import { z } from "zod";

export const FAKE = new URL("../../../harness/fake_claude.py", import.meta.url).pathname;

export const add = tool("add", "Add two whole numbers", { a: z.number().int(), b: z.number().int() }, async (args) => ({ content: [{ type: "text" as const, text: `Sum: ${args.a + args.b}` }] }));

export const divide = tool("divide", "Divide one whole number by another", { a: z.number().int(), b: z.number().int() }, async (args) => {
  if (args.b === 0) return { content: [{ type: "text" as const, text: "Cannot divide by zero" }], isError: true }; // the message Claude reads, written by you
  return { content: [{ type: "text" as const, text: `Quotient: ${Math.floor(args.a / args.b)}` }] };
});

/** A PreToolUse hook: it runs in your process before the tool and can deny the call. */
export async function noPush(input: any) {
  if ((input.tool_input?.command ?? "").includes("git push")) {
    return { hookSpecificOutput: { hookEventName: "PreToolUse" as const, permissionDecision: "deny" as const, permissionDecisionReason: "Nothing is pushed from an agent" } };
  }
  return {};
}

/** The permission callback: it is asked about a call that no rule has decided. The tools of your own server are allowed here, not by
 * allowedTools: an allowedTools entry approves a tool before this callback is consulted, and the SDK warns when it would shadow it. */
export async function askMe(toolName: string, toolInput: Record<string, any>) {
  if (toolName.startsWith("mcp__calc__") || (toolName === "Bash" && toolInput.command.split(" ")[0] === "ls")) return { behavior: "allow" as const, updatedInput: toolInput };
  return { behavior: "deny" as const, message: `${toolName} is not allowed here` };
}

const SCRIPT = [
  { say: "I will add, then divide." },
  { tool: { id: "t1", name: "mcp__calc__add", input: { a: 2, b: 3 } } },
  { tool: { id: "t2", name: "mcp__calc__divide", input: { a: 1, b: 0 } } },
  { tool: { id: "t3", name: "Bash", input: { command: "git push origin main" }, output: "pushed" } },
  { tool: { id: "t4", name: "Bash", input: { command: "ls" }, output: "README.md" } },
  { say: "Two sums, one refused push." },
  { result: { subtype: "success", result: "Two sums, one refused push.", cost: 0.02, turns: 5 } },
];

function options(workdir: string) {
  return {
    pathToClaudeCodeExecutable: FAKE, cwd: workdir, tools: ["Read", "Bash"], disallowedTools: ["Bash(rm *)"], maxTurns: 6,
    permissionMode: "default" as const, settingSources: [] as ("user" | "project" | "local")[], mcpServers: { calc: createSdkMcpServer({ name: "calc", tools: [add, divide] }) },
    canUseTool: askMe, hooks: { PreToolUse: [{ matcher: "Bash", hooks: [noPush] }] },
  };
}

function show(message: any) {
  if (message.type === "system" && message.subtype === "init") console.log(`init: the binary reports tools [${message.tools.map((t: string) => `'${t}'`).join(", ")}]`);
  else if (message.type === "assistant") {
    for (const block of message.message.content) console.log(block.type === "text" ? "claude says:" : "claude calls:", block.type === "text" ? block.text : `${block.name} ${JSON.stringify(block.input)}`);
  } else if (message.type === "user" && Array.isArray(message.message.content)) {
    for (const block of message.message.content) if (block.type === "tool_result") console.log("  result:", block.is_error ? "ERROR" : "ok   ", block.content);
  } else if (message.type === "result") console.log(`done: ${message.subtype}, ${message.num_turns} turns, cost $${message.total_cost_usd.toFixed(2)}`);
}

async function main() {
  const workdir = mkdtempSync(join(tmpdir(), "agent-"));
  const script = join(workdir, "script.json"), record = join(workdir, "record.jsonl");
  writeFileSync(script, JSON.stringify({ session_id: "demo", turns: [SCRIPT] }));
  process.env.FAKE_CLAUDE_SCRIPT = script;
  process.env.FAKE_CLAUDE_RECORD = record;
  for await (const message of query({ prompt: "Add 2 and 3, divide 1 by 0, push, then list the files.", options: options(workdir) as any })) show(message);
  const lines = readFileSync(record, "utf8").split("\n").filter(Boolean).map((l) => JSON.parse(l));
  const argv: string[] = lines.find((l) => l.argv).argv;
  console.log();
  for (const flag of ["--tools", "--disallowedTools", "--max-turns", "--permission-mode"]) console.log(`flag ${flag} ${argv[argv.indexOf(flag) + 1]}`);
  const asked: Record<string, number> = {};
  for (const l of lines.filter((l) => l.ask)) asked[l.ask.subtype] = (asked[l.ask.subtype] ?? 0) + 1;
  console.log("what the binary asked your process:", `{${Object.keys(asked).sort().map((k) => `'${k}': ${asked[k]}`).join(", ")}}`);
}

if (import.meta.main) await main();
