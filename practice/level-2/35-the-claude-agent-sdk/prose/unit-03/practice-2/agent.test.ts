import { test } from "node:test";
import assert from "node:assert/strict";
import { chmodSync, copyFileSync, existsSync, mkdtempSync, readFileSync, realpathSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { bashGuard, buildOptions, decide, makeCanUseTool, runAgent, summarize } = await import(pathToFileURL(resolve(dir, "agent.ts")).href);

const P = "/proj";
const FAKE = (() => {
  const candidates = [join(import.meta.dirname, "harness", "fake_claude.py"), "/w/harness/fake_claude.py"];
  for (let d = resolve("."), i = 0; i < 8; i++, d = resolve(d, "..")) candidates.push(join(d, "harness", "fake_claude.py"));
  // The SDK starts the stand-in as a program, so it must be executable: use a copy marked so (the harness folder may be read-only).
  const exe = join(mkdtempSync(join(tmpdir(), "fake-claude-")), "fake_claude.py");
  copyFileSync(candidates.find((c) => existsSync(c)) as string, exe);
  chmodSync(exe, 0o755);
  return exe;
})();
(await import("node:fs")).chmodSync(FAKE, 0o755);

const tool = (id: string, name: string, input: Record<string, unknown>) => ({ tool: { id, name, input, output: `${name} ok` } });
const finish = (text = "All done.", subtype = "success", cost = 0.02, turns = 3) => [{ say: text }, { result: { subtype, result: text, cost, turns } }];

/** Run runAgent against the scripted CLI. The summary is what it returned, or the error it threw. */
async function runRaw(steps: any[], mode = "readonly") {
  const d = mkdtempSync(join(tmpdir(), "agent-"));
  const script = join(d, "script.json"), record = join(d, "record.jsonl");
  writeFileSync(script, JSON.stringify({ session_id: "s1", turns: [steps] }));
  process.env.FAKE_CLAUDE_SCRIPT = script;
  process.env.FAKE_CLAUDE_RECORD = record;
  let summary: any;
  try {
    summary = (await runAgent("go", d, FAKE, mode)) ?? {};
  } catch (e) {
    summary = e instanceof Error ? e : new Error(String(e));
  }
  const records: any[] = existsSync(record) ? readFileSync(record, "utf8").split("\n").filter(Boolean).map((l) => JSON.parse(l)) : [];
  return { summary, records, d };
}

async function run(steps: any[], mode = "readonly") {
  const out = await runRaw(steps, mode);
  if (out.summary instanceof Error) throw new assert.AssertionError({ message: `runAgent threw ${out.summary.message}` });
  return out;
}

const asks = (records: any[]) => records.filter((r) => r.ask).map((r) => [r.ask.subtype, r.ask.tool_name]);
const argvOf = (records: any[]): string[] => records.find((r) => r.argv)?.argv ?? [];
const flag = (records: any[], name: string) => { const a = argvOf(records); return a.includes(name) ? a[a.indexOf(name) + 1] : null; };
const verdict = (name: string, input: Record<string, unknown>, mode = "readonly"): any => decide(name, input, P, mode) ?? {};
const denial = (name: string, input: Record<string, unknown>, mode = "readonly") => { const v = verdict(name, input, mode); return [v.behavior, v.message, v.interrupt]; };

test("m1 every tool call goes through the permission callback and the run is summarised", async () => {
  const { summary, records } = await run([tool("t1", "Read", { file_path: "a.txt" }), tool("t2", "Bash", { command: "pytest -q" }), ...finish()]);
  assert.deepEqual(summary, { status: "done", text: "All done.", tools: ["Read", "Bash"], turns: 3, cost: 0.02, denied: 0 });
  assert.deepEqual(asks(records), [["can_use_tool", "Read"], ["hook_callback", undefined], ["can_use_tool", "Bash"]].map(([a, b]) => (b === undefined ? [a, null] : [a, b])));
});

test("e1 the options reach the cli as flags and the run starts in the project", async () => {
  const { records, d } = await run(finish(), "readonly");
  assert.ok(flag(records, "--tools") === "Read,Grep,Glob,Bash" && flag(records, "--disallowedTools") === "Bash(rm *)");
  assert.ok(flag(records, "--max-turns") === "6" && flag(records, "--max-budget-usd") === "0.5" && flag(records, "--permission-mode") === "default");
  assert.ok([null, ""].includes(flag(records, "--allowedTools")));
  assert.ok(argvOf(records).includes("--setting-sources="));
  const cwd: string = records.find((r) => r.cwd)?.cwd ?? "";
  assert.ok(cwd !== "" && realpathSync(cwd) === realpathSync(d));
  const edit = await run(finish(), "edit");
  assert.equal(flag(edit.records, "--tools"), "Read,Grep,Glob,Bash,Edit,Write");
  const options = buildOptions(P, FAKE);
  assert.ok(options && options.pathToClaudeCodeExecutable === FAKE && options.maxTurns === 6);
});

test("e2 file tools stay inside the project and away from secrets", () => {
  assert.equal(verdict("Read", { file_path: "src/a.py" }).behavior, "allow");
  assert.equal(verdict("Read", { file_path: "/proj/src/a.py" }).behavior, "allow");
  assert.deepEqual(denial("Read", { file_path: "../secret.txt" }), ["deny", "../secret.txt is outside the project", false]);
  assert.deepEqual(denial("Read", { file_path: "/etc/passwd" }), ["deny", "/etc/passwd is outside the project", false]);
  assert.equal(denial("Read", { file_path: "src/../../x" })[0], "deny");
  assert.equal(denial("Read", { file_path: "/proj-other/x" })[0], "deny");
  assert.deepEqual(denial("Read", { file_path: ".env" }), ["deny", ".env holds secrets and is never read", false]);
  assert.equal(denial("Read", { file_path: "config/.env.local" })[1], ".env.local holds secrets and is never read");
  assert.equal(verdict("Read", { file_path: ".env.example" }).behavior, "allow");
  assert.equal(verdict("Grep", { pattern: "x" }).behavior, "allow");
  assert.equal(verdict("Glob", { path: "src", pattern: "*.py" }).behavior, "allow");
  assert.equal(denial("Grep", { pattern: "x", path: "/other" })[0], "deny");
  assert.deepEqual(denial("Write", { file_path: "src/a.py" }), ["deny", "Edits are not allowed in readonly mode", false]);
  assert.equal(verdict("Write", { file_path: "src/a.py" }, "edit").behavior, "allow");
  assert.equal(verdict("Edit", { file_path: "src/a.py" }, "edit").behavior, "allow");
  assert.deepEqual(denial("Write", { file_path: ".git/config" }, "edit"), ["deny", ".git/config is inside .git", false]);
  assert.equal(denial("Write", { file_path: "src/.git/hooks/x" }, "edit")[0], "deny");
  assert.equal(denial("Write", { file_path: "../x" }, "edit")[0], "deny");
  assert.equal(denial("Edit", { file_path: ".env" }, "edit")[0], "deny");
  assert.deepEqual(denial("WebFetch", { url: "https://example.invalid" }), ["deny", "WebFetch is not allowed", false]);
});

test("e3 bash is limited to a few commands and dangerous ones stop the run", async () => {
  for (const ok of ["ls -la", "cat README.md", "pytest -q tests/", "  ls"]) assert.equal(verdict("Bash", { command: ok }).behavior, "allow", ok);
  assert.deepEqual(denial("Bash", { command: "python x.py" }), ["deny", "Command not allowed: only ls, cat and pytest", false]);
  assert.equal(denial("Bash", { command: "" })[0], "deny");
  for (const chained of ["ls; rm x", "cat a > b", "ls $(whoami)", "ls | wc", "pytest && ls", "cat `x`"]) {
    assert.deepEqual(denial("Bash", { command: chained }), ["deny", "Command not allowed: no chaining or redirection", false], chained);
  }
  for (const dangerous of ["sudo ls", "rm -rf /tmp/x", "pytest && sudo reboot"]) assert.deepEqual(denial("Bash", { command: dangerous }), ["deny", "Dangerous command", true], dangerous);
  const callback: any = makeCanUseTool(P);
  assert.deepEqual(await callback("Bash", { command: "ls" }, {}), { behavior: "allow", updatedInput: { command: "ls" } });
  const stopped = await callback("Bash", { command: "sudo ls" }, {});
  assert.deepEqual([stopped.behavior, stopped.message, stopped.interrupt], ["deny", "Dangerous command", true]);
});

test("e4 a hook blocks a push before the permission callback is asked", async () => {
  const guard = (command: string) => bashGuard({ tool_input: { command } }, "t", {});
  const deny = { hookSpecificOutput: { hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: "Nothing is pushed from an agent" } };
  for (const command of ["git push", "git  push --force", "echo done && git push origin main"]) assert.deepEqual(await guard(command), deny, command);
  for (const command of ["git status", "git pushd", "legit push", "ls"]) assert.deepEqual(await guard(command), {}, command);
  assert.deepEqual(await bashGuard({}, "t", {}), {});
  const { summary, records } = await run([tool("t1", "Bash", { command: "git push origin main" }), tool("t2", "Bash", { command: "pytest" }), ...finish()]);
  assert.deepEqual(asks(records), [["hook_callback", null], ["hook_callback", null], ["can_use_tool", "Bash"]]);
  assert.equal(summary.denied, 1);
  assert.deepEqual(summary.tools, ["Bash", "Bash"]);
});

test("e5 the messages of a run fold into a summary", () => {
  const result = (subtype = "success", text: string | null = "final", cost: number | null = 0.5, turns = 4) => ({ type: "result", subtype, num_turns: turns, result: text, total_cost_usd: cost });
  const said = { type: "assistant", message: { content: [{ type: "text", text: "first words" }, { type: "tool_use", id: "t1", name: "Read", input: {} }, { type: "text", text: "last words" }] } };
  const more = { type: "assistant", message: { content: [{ type: "tool_use", id: "t2", name: "Bash", input: {} }] } };
  const errors = { type: "user", message: { content: [{ type: "tool_result", tool_use_id: "t1", content: "no", is_error: true }, { type: "tool_result", tool_use_id: "t2", content: "yes", is_error: false }, { type: "tool_result", tool_use_id: "t3", content: "n" }] } };
  assert.deepEqual(summarize([said, errors, more, result()]), { status: "done", text: "final", tools: ["Read", "Bash"], turns: 4, cost: 0.5, denied: 1 });
  assert.equal(summarize([said, result("success", null)])?.text, "last words");
  assert.equal(summarize([said, result("success", "")])?.text, "last words");
  assert.equal(summarize([{ type: "user", message: { content: "plain text" } }, result()])?.denied, 0);
  for (const [subtype, status] of [["error_max_turns", "max_turns"], ["error_max_budget_usd", "budget"], ["error_during_execution", "failed"], ["error_max_structured_output_retries", "error_max_structured_output_retries"]]) {
    assert.equal(summarize([result(subtype)])?.status, status);
  }
  assert.equal(summarize([result("success", "final", null)])?.cost, 0);
  assert.deepEqual(summarize([said, errors]), { status: "incomplete", text: "last words", tools: ["Read"], turns: 0, cost: 0, denied: 1 });
});

test("e6 an error result still gives its summary and a crash is not hidden", async () => {
  const steps = [...Array.from({ length: 8 }, (_, i) => tool(`t${i}`, "Read", { file_path: "a.txt" })), ...finish()]; // the CLI stops at the turn limit and exits with code 1
  const { summary, records } = await run(steps);
  assert.deepEqual([summary.status, summary.turns, summary.tools?.length], ["max_turns", 6, 5]);
  assert.equal(flag(records, "--max-turns"), "6");
  const budget = (await run([tool("t1", "Read", { file_path: "a.txt" }), ...finish("Stopped early.", "error_max_budget_usd", 0.5, 2)])).summary;
  assert.deepEqual([budget.status, budget.cost, budget.text, budget.tools], ["budget", 0.5, "Stopped early.", ["Read"]]);
  const crashed = (await runRaw([tool("t1", "Read", { file_path: "a.txt" }), { exit: 2 }])).summary; // the process dies before it sends any result
  assert.ok(crashed instanceof Error);
});

test("e7 denied calls are counted and the run still ends with a result", async () => {
  const { summary, records } = await run([tool("t1", "Write", { file_path: "src/a.py", content: "x" }), tool("t2", "Bash", { command: "ls; rm x" }), tool("t3", "Read", { file_path: "../secret.txt" }), tool("t4", "Read", { file_path: "src/a.py" }), ...finish()]);
  assert.deepEqual([summary.status, summary.denied, summary.tools], ["done", 3, ["Write", "Bash", "Read", "Read"]]);
  assert.deepEqual(asks(records), [["hook_callback", null], ["can_use_tool", "Bash"], ["can_use_tool", "Read"], ["can_use_tool", "Read"]]);
});
