// Session state, offline: what continue, resume and fork send to the binary, and when a saved session is worth resuming.
//
// The Agent SDK starts the Claude Code binary; here the binary is `harness/fake_claude.py`, which replays a script, so no model is called and no network is used.
// The session ids come from the script (the stand-in does not store sessions); the flags are the ones the real SDK builds.
// `@anthropic-ai/claude-agent-sdk` 0.3.287, checked on 2026-10-03 against the "Work with sessions" page of the Claude Code documentation.
import { chmodSync, copyFileSync, mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { query } from "@anthropic-ai/claude-agent-sdk";
import { logger } from "./logger.ts";
const log = logger("session_state");

// The SDK starts the stand-in as a program, so it must be executable: use a copy marked so (the harness folder may be read-only).
const FAKE_SOURCE = new URL("../../../harness/fake_claude.py", import.meta.url).pathname;
const FAKE = (() => {
  const exe = join(mkdtempSync(join(tmpdir(), "fake-claude-")), "fake_claude.py");
  copyFileSync(FAKE_SOURCE, exe);
  chmodSync(exe, 0o755);
  return exe;
})();

/** resume, resume with a notice, or start fresh with a summary, from the files a session analysed and the files now. */
export function decide(saved: Record<string, string>, current: Record<string, string>, idleDays: number): [string, string[]] {
  const changed = Object.keys(saved).filter((p) => p in current && saved[p] !== current[p]).sort();
  const gone = Object.keys(saved).filter((p) => !(p in current)).sort();
  if ((changed.length + gone.length) / Object.keys(saved).length > 0.5 || idleDays > 7) return ["start fresh with a summary", changed];
  return changed.length || gone.length ? ["resume with a notice", changed] : ["resume", changed];
}

export function notice(changed: string[]): string {
  return ["Since your earlier analysis:", `- changed: ${changed.join(", ")}`,
    "Re-read these files before relying on earlier conclusions about them. Every other file is unchanged."].join("\n");
}

/** The session flags of one command line, in the order they appear. */
export function flags(argv: string[]): string {
  const out: string[] = [];
  argv.forEach((arg, i) => {
    if (arg === "--resume") out.push(`--resume ${argv[i + 1]}`);
    else if (arg.startsWith("--resume=")) out.push(`--resume ${arg.split("=")[1]}`); // the Python SDK writes the id after an equals sign
    else if (arg === "--fork-session" || arg === "--continue") out.push(arg);
  });
  return out.join(", ") || "none";
}

/** One single-shot run against the stand-in, scripted to report `sessionId`; returns the id the result carries and the session flags sent. */
async function run(sessionId: string, options: Record<string, unknown>): Promise<[string | undefined, string]> {
  const folder = mkdtempSync(join(tmpdir(), "session-"));
  const script = join(folder, "script.json"), record = join(folder, "record.jsonl");
  writeFileSync(script, JSON.stringify({ session_id: sessionId, turns: [[{ say: "ok" }, { result: { subtype: "success", result: "ok", cost: 0.01, turns: 1 } }]] }));
  process.env.FAKE_CLAUDE_SCRIPT = script;
  process.env.FAKE_CLAUDE_RECORD = record;
  let seen: string | undefined;
  const all = { pathToClaudeCodeExecutable: FAKE, cwd: folder, settingSources: [], env: { ...process.env }, ...options };
  for await (const message of query({ prompt: "Continue the review", options: all as any }) as AsyncIterable<any>) {
    if (message.type === "result") seen = message.session_id;
  }
  const line = readFileSync(record, "utf8").split("\n").find((l) => l.includes('"argv"')) as string;
  return [seen, flags(JSON.parse(line).argv)];
}

async function main() {
  console.log("what each control sends to the binary");
  const runs: Array<[string, string, Record<string, unknown>]> = [
    ["new session", "s-auth-1", {}], ["resume s-auth-1", "s-auth-1", { resume: "s-auth-1" }],
    ["resume s-auth-1 and fork", "s-auth-2", { resume: "s-auth-1", forkSession: true }],
    ["resume s-auth-1 again", "s-auth-1", { resume: "s-auth-1" }], ["continue the latest", "s-auth-1", { continue: true }],
  ];
  for (const [label, scripted, options] of runs) {
    const [seen, sent] = await run(scripted, options);
    console.log(`  ${label.padEnd(26)} -> session ${seen}, flags: ${sent}`);
  }
  const saved = { "a.py": "d1", "b.py": "d2", "c.py": "d3", "d.py": "d4" };
  console.log("\nwhat to do with a saved session (4 files analysed)");
  const cases: Array<[string, Record<string, string>, number]> = [
    ["nothing changed, idle 1 day", saved, 1], ["b.py changed", { ...saved, "b.py": "x" }, 1],
    ["3 of 4 files changed", { "a.py": "x", "b.py": "x", "c.py": "x", "d.py": "d4" }, 1], ["nothing changed, idle 8 days", saved, 8],
  ];
  for (const [label, current, idle] of cases) console.log(`  ${label.padEnd(30)} -> ${decide(saved, current, idle)[0]}`);
  console.log("\nthe notice for the second case");
  for (const line of notice(decide(saved, cases[1][1], 1)[1]).split("\n")) console.log(`  ${line}`);
}

if (import.meta.main) await main();
