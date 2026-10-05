// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { existsSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { runAgent } from "./agent.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// The tests never call the model: they start harness/fake_claude.py, a scripted stand-in for the Claude Code binary.
const candidates = ["/w/harness/fake_claude.py"];
for (let d = resolve("."), i = 0; i < 8; i++, d = resolve(d, "..")) candidates.push(join(d, "harness", "fake_claude.py"));
const fake = candidates.find((c) => existsSync(c)) as string;

// The script: the agent reads a file, runs the tests with Bash, then finishes.
const steps = [
  { tool: { id: "t1", name: "Read", input: { file_path: "a.txt" }, output: "Read ok" } },
  { tool: { id: "t2", name: "Bash", input: { command: "pytest -q" }, output: "Bash ok" } },
  { say: "All done." },
  { result: { subtype: "success", result: "All done.", cost: 0.02, turns: 3 } },
];
const project = mkdtempSync(join(tmpdir(), "agent-"));
writeFileSync(join(project, "script.json"), JSON.stringify({ session_id: "s1", turns: [steps] }));
process.env.FAKE_CLAUDE_SCRIPT = join(project, "script.json");
process.env.FAKE_CLAUDE_RECORD = join(project, "record.jsonl");

try {
  const summary: any = (await runAgent("go", project, fake, "readonly")) ?? {};
  console.log("status:", summary.status);
  console.log("tools used:", JSON.stringify(summary.tools));
  console.log("turns and cost:", summary.turns, summary.cost);
  console.log("calls denied:", summary.denied);
} catch (err) {
  console.log("raised:", String(err));
}
