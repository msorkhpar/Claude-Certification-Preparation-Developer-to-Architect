// Invoking a subagent with the Agent SDK, offline: the definitions sent to the binary, the spawn in the stream and the messages that ran inside it.
//
// The Agent SDK starts the Claude Code binary; here the binary is `harness/fake_claude.py`, which replays a script, so no model is called and no network is
// used. The stand-in puts the messages of the subagent after the call that started it, which the real binary may order differently.
// `@anthropic-ai/claude-agent-sdk` 0.3.287, checked on 2026-10-03 against the Agent SDK pages of the Claude Code documentation.
import { chmodSync, copyFileSync, mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { query } from "@anthropic-ai/claude-agent-sdk";
import { logger } from "./logger.ts";
const log = logger("subagent_run");

// The SDK starts the stand-in as a program, so it must be executable: use a copy marked so (the harness folder may be read-only).
const FAKE_SOURCE = new URL("../../../harness/fake_claude.py", import.meta.url).pathname;
export const FAKE = (() => {
  const exe = join(mkdtempSync(join(tmpdir(), "fake-claude-")), "fake_claude.py");
  copyFileSync(FAKE_SOURCE, exe);
  chmodSync(exe, 0o755);
  return exe;
})();

export const AGENTS = {
  reviewer: { description: "Reviews one module for security problems. Use for any review request.", prompt: "You review code and report findings only.", tools: ["Read", "Grep"], model: "sonnet" },
  finder: { description: "Finds the files that touch a feature.", prompt: "You list files.", tools: ["Read", "Grep", "Glob"] },
};

export const BRIEF = "Task: Review auth.py for injection risks\nFiles:\n- src/auth.py\nKnown:\n- the login query is built with string formatting\nReturn: findings, one line each";

const SCRIPT = [{ say: "I will delegate the review." },
  { tool: { id: "t1", name: "Agent", input: { subagent_type: "reviewer", description: "Review auth.py", prompt: BRIEF }, output: "1 finding: string-built query in login()." } },
  { tool: { id: "t2", name: "Read", input: { file_path: "src/auth.py" }, output: "def login(): ...", parent: "t1" } },
  { tool: { id: "t3", name: "Grep", input: { pattern: "execute(" }, output: "src/auth.py:12", parent: "t1" } },
  { say: "One finding, from the reviewer." },
  { result: { subtype: "success", result: "One finding, from the reviewer.", cost: 0.04, turns: 3 } }];

/** One line per interesting message: a spawn, a message from inside a subagent, a report, the result. */
export function describe(message: any): string[] {
  const lines: string[] = [];
  const inside = message.parent_tool_use_id;
  if (message.type === "assistant") {
    for (const block of message.message.content) {
      if (block.type === "tool_use" && (block.name === "Agent" || block.name === "Task")) lines.push(`spawn: ${block.name} -> ${block.input.subagent_type}, brief of ${block.input.prompt.split("\n").length} lines`);
      else if (block.type === "tool_use") lines.push(inside ? `  inside ${inside}: ${block.name} ${JSON.stringify(block.input)}` : `coordinator calls ${block.name}`);
      else if (block.type === "text") lines.push(`coordinator says: ${block.text}`);
    }
  } else if (message.type === "user" && Array.isArray(message.message.content)) {
    for (const block of message.message.content) if (block.type === "tool_result" && !inside) lines.push(`report to the coordinator: ${block.content}`);
  } else if (message.type === "result") {
    lines.push(`done: ${message.subtype}, cost $${message.total_cost_usd.toFixed(2)}`);
  }
  return lines;
}

async function main() {
  const workdir = mkdtempSync(join(tmpdir(), "team-"));
  const script = join(workdir, "script.json"), record = join(workdir, "record.jsonl");
  writeFileSync(script, JSON.stringify({ session_id: "demo", turns: [SCRIPT] }));
  process.env.FAKE_CLAUDE_SCRIPT = script;
  process.env.FAKE_CLAUDE_RECORD = record;
  const options = {
    pathToClaudeCodeExecutable: FAKE, cwd: workdir, agents: AGENTS, allowedTools: ["Agent", "Read", "Grep", "Glob"], maxBudgetUsd: 0.5, maxTurns: 10,
    permissionMode: "default" as const, settingSources: [] as ("user" | "project" | "local")[], env: { ...process.env, CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH: "1" },
  };
  for await (const message of query({ prompt: "Review auth.py", options: options as any })) for (const line of describe(message)) console.log(line);
  const lines = readFileSync(record, "utf8").split("\n").filter(Boolean).map((l) => JSON.parse(l));
  const sent = lines.find((l) => l.agents).agents;
  const py = (v: unknown) => (Array.isArray(v) ? `[${v.map((x) => `'${x}'`).join(", ")}]` : v === undefined ? "None" : String(v));
  console.log();
  for (const name of Object.keys(sent).sort()) console.log(`agent sent to the binary: ${name} tools=${py(sent[name].tools)} model=${sent[name].model ?? "not set"}`);
  const argv: string[] = lines.find((l) => l.argv).argv;
  for (const flag of ["--allowedTools", "--max-budget-usd", "--max-turns"]) console.log(`flag ${flag} ${argv[argv.indexOf(flag) + 1]}`);
}

if (import.meta.main) await main();
