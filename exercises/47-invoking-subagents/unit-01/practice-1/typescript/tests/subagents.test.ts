import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { buildOptions, bySubagent, makeBrief, mergeFindings, packageFinding, runTeam, spawned } = await import(pathToFileURL(resolve(dir, "subagents.ts")).href);

const FAKE = (() => {
  const candidates = ["/w/harness/fake_claude.py"];
  for (let d = resolve("."), i = 0; i < 8; i++, d = resolve(d, "..")) candidates.push(join(d, "harness", "fake_claude.py"));
  return candidates.find((c) => existsSync(c)) as string;
})();

const SPECS = {
  reviewer: { description: "Reviews one module for security problems. Use for any review request.", prompt: "You review code.", tools: ["Read", "Grep"], model: "sonnet" },
  finder: { description: "Finds the files that touch a feature.", prompt: "You find files." },
};

function options(specs: Record<string, any> = SPECS, ...rest: any[]) {
  const built = buildOptions(specs, "/proj", ...rest);
  assert.ok(built !== null && built !== undefined, "buildOptions returned nothing");
  return built;
}

function refused(specs: Record<string, any>) {
  try {
    buildOptions(specs, "/proj");
  } catch {
    return true;
  }
  return false;
}

const call = (id: string, name: string, input: Record<string, unknown> = {}, parent: string | null = null) =>
  ({ type: "assistant", parent_tool_use_id: parent, message: { role: "assistant", content: [{ type: "tool_use", id, name, input }] } });
const returned = (id: string, content: string, parent: string | null = null) =>
  ({ type: "user", parent_tool_use_id: parent, message: { role: "user", content: [{ type: "tool_result", tool_use_id: id, content }] } });

test("m1 the options register each named agent with its description prompt tools and model", () => {
  const o = options();
  assert.deepEqual(Object.keys(o.agents).sort(), ["finder", "reviewer"]);
  const r = o.agents.reviewer;
  assert.deepEqual([r.description, r.prompt, r.tools, r.model], [SPECS.reviewer.description, "You review code.", ["Read", "Grep"], "sonnet"]);
  assert.ok(o.allowedTools[0] === "Agent" && o.cwd === "/proj" && o.permissionMode === "default" && o.settingSources.length === 0);
});

test("e1 a subagent gets read only tools by default and never the right to spawn another", () => {
  const o = options();
  assert.deepEqual([o.agents.finder.tools, o.agents.finder.model], [["Read", "Grep", "Glob"], "inherit"]);
  assert.deepEqual(options({ boss: { description: "Delegates work.", prompt: "p", tools: ["Read", "Agent", "Bash"] } }).agents.boss.tools, ["Read", "Bash"]);
  assert.deepEqual(options({ mute: { description: "Says nothing.", prompt: "p", tools: [] } }).agents.mute.tools, []);
});

test("e2 a definition with a bad name or no description or no prompt is refused", () => {
  const base = { description: "Does a job.", prompt: "p" };
  assert.ok(refused({ "Bad Name": base }) && refused({ under_score: base }) && refused({ "9lives": base }));
  assert.ok(refused({ ok: { ...base, description: "  " } }) && refused({ ok: { prompt: "p" } }) && refused({ ok: { ...base, prompt: "" } }));
  assert.ok(!refused({ "ok-name-2": base }));
});

test("e3 depth concurrency budget and turn limits are set on the options", () => {
  const o = options(SPECS, "/x/claude", 1.5, 9, 3);
  assert.equal(o.env.CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH, "1");
  assert.equal(o.env.CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS, "3");
  assert.ok(o.env.PATH !== undefined, "the environment keeps PATH, because the TypeScript SDK replaces it");
  assert.deepEqual([o.maxBudgetUsd, o.maxTurns, o.pathToClaudeCodeExecutable], [1.5, 9, "/x/claude"]);
  const d = options();
  assert.deepEqual([d.maxBudgetUsd, d.maxTurns, d.env.CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS], [2.0, 20, "5"]);
});

test("e4 a spawn is recognised under the new and the old tool name and nothing else is", () => {
  const messages = [call("a", "Agent", { subagent_type: "reviewer", description: "review auth", prompt: "Review auth.py" }), call("b", "Task", { description: "old name", prompt: "Look" }),
    call("c", "Read", { file_path: "x" }), { type: "assistant", parent_tool_use_id: null, message: { role: "assistant", content: [{ type: "text", text: "hi" }] } }, returned("a", "done")];
  assert.deepEqual(spawned(messages), [{ id: "a", subagent_type: "reviewer", description: "review auth", prompt: "Review auth.py" },
    { id: "b", subagent_type: "general-purpose", description: "old name", prompt: "Look" }]);
});

test("e5 messages from inside a subagent are grouped under the call that started it", () => {
  const messages = [call("a", "Agent", { subagent_type: "reviewer", description: "d", prompt: "p" }), call("r1", "Read", { file_path: "x" }, "a"), returned("r1", "code", "a"),
    call("r2", "Grep", { pattern: "y" }, "a"), call("r3", "Read", { file_path: "z" }, "a"), returned("a", "report"),
    call("b", "Agent", { subagent_type: "finder", description: "d", prompt: "p" }), call("r4", "Glob", { pattern: "*" }, "b")];
  assert.deepEqual(bySubagent(messages), { a: { subagent_type: "reviewer", messages: 4, tools: ["Read", "Grep"] }, b: { subagent_type: "finder", messages: 1, tools: ["Glob"] } });
});

test("e6 a brief carries the task and every fact the subagent needs in a fixed layout", () => {
  const full = makeBrief("  Fix the broken check ", ["src/auth.py", " ", "tests/test_auth.py"], ["the error is KeyError: 'token'", "do not touch vendor/"], " a diff and one sentence ");
  assert.equal(full, "Task: Fix the broken check\nFiles:\n- src/auth.py\n- tests/test_auth.py\nKnown:\n- the error is KeyError: 'token'\n- do not touch vendor/\nReturn: a diff and one sentence");
  assert.equal(makeBrief("Find usages"), "Task: Find usages");
  let raised = false;
  try {
    makeBrief("   ");
  } catch {
    raised = true;
  }
  assert.ok(raised);
});

test("e7 findings keep the claim apart from its source and merging keeps every source", () => {
  const a = packageFinding(" Rates held in 2024 ", "https://example.com/a", "Bank statement", 3);
  const b = packageFinding("rates  held in 2024", "https://example.com/b");
  const c = packageFinding("Loans stayed dear");
  assert.deepEqual(a, { claim: "Rates held in 2024", source: { url: "https://example.com/a", title: "Bank statement", page: 3 } });
  assert.deepEqual(c, { claim: "Loans stayed dear", source: null });
  assert.deepEqual(mergeFindings([a, b, c, a]), [
    { claim: "Rates held in 2024", sources: [{ url: "https://example.com/a", title: "Bank statement", page: 3 }, { url: "https://example.com/b" }], attributed: true },
    { claim: "Loans stayed dear", sources: [], attributed: false }]);
});

test("e8 a run through the sdk shows the spawn the inner messages and the agents sent to the binary", async () => {
  const d = mkdtempSync(join(tmpdir(), "team-"));
  const steps = [{ say: "I will delegate." },
    { tool: { id: "t1", name: "Agent", input: { subagent_type: "reviewer", description: "review auth", prompt: "Review auth.py" }, output: "Two issues found." } },
    { tool: { id: "t2", name: "Read", input: { file_path: "auth.py" }, output: "code", parent: "t1" } },
    { say: "Two issues, see the report." }, { result: { subtype: "success", result: "Two issues, see the report.", cost: 0.03, turns: 2 } }];
  const script = join(d, "script.json"), record = join(d, "record.jsonl");
  writeFileSync(script, JSON.stringify({ session_id: "s1", turns: [steps] }));
  process.env.FAKE_CLAUDE_SCRIPT = script;
  process.env.FAKE_CLAUDE_RECORD = record;
  const o = buildOptions(SPECS, d, FAKE, 0.5);
  const run = (o ? await runTeam("Review auth.py", o) : null) ?? { messages: [], error: "nothing" };
  assert.ok(run.error === null && run.messages.length > 0);
  assert.deepEqual((spawned(run.messages) ?? []).map((c: any) => [c.id, c.subagent_type]), [["t1", "reviewer"]]);
  assert.deepEqual((bySubagent(run.messages) ?? {}).t1, { subagent_type: "reviewer", messages: 2, tools: ["Read"] });
  const lines = readFileSync(record, "utf8").split("\n").filter(Boolean).map((l) => JSON.parse(l));
  const agents = lines.find((l) => l.agents).agents;
  assert.ok(JSON.stringify(Object.keys(agents).sort()) === JSON.stringify(["finder", "reviewer"]) && JSON.stringify(agents.reviewer.tools) === JSON.stringify(["Read", "Grep"]));
  assert.deepEqual(agents.finder.tools, ["Read", "Grep", "Glob"]);
  const argv: string[] = lines.find((l) => l.argv).argv;
  assert.equal(argv[argv.indexOf("--max-budget-usd") + 1], "0.5");
});
