import { test } from "node:test";
import assert from "node:assert/strict";
import { chmodSync, copyFileSync, existsSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { query } from "@anthropic-ai/claude-agent-sdk";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { buildOptions, commandHook, postNormalise, preRefund, settingsHooks } = await import(pathToFileURL(resolve(dir, "hooks.ts")).href);

const FAKE = (() => {
  const candidates = ["/w/harness/fake_claude.py"];
  for (let d = resolve("."), i = 0; i < 8; i++, d = resolve(d, "..")) candidates.push(join(d, "harness", "fake_claude.py"));
  // The SDK starts the stand-in as a program, so it must be executable: use a copy marked so (the harness folder may be read-only).
  const exe = join(mkdtempSync(join(tmpdir(), "fake-claude-")), "fake_claude.py");
  copyFileSync(candidates.find((c) => existsSync(c)) as string, exe);
  chmodSync(exe, 0o755);
  return exe;
})();

async function pre(toolInput: Record<string, unknown> = {}, toolName = "process_refund"): Promise<any> {
  const out = await preRefund({ hook_event_name: "PreToolUse", tool_name: toolName, tool_input: toolInput }, "t1", undefined);
  assert.ok(out !== null && out !== undefined, "preRefund returned nothing");
  return out?.hookSpecificOutput ?? {};
}

async function post(response: unknown): Promise<any> {
  const out = await postNormalise({ hook_event_name: "PostToolUse", tool_name: "get_order", tool_input: {}, tool_response: response }, "t1", undefined);
  assert.ok(out !== null && out !== undefined, "postNormalise returned nothing");
  return out?.hookSpecificOutput?.updatedToolOutput ?? null;
}

const decision = async (amount: unknown) => (await pre({ amount })).permissionDecision;

test("m1 the gate allows a small refund asks about a middle one and denies a large one", async () => {
  const [small, middle, large] = [await pre({ amount: 50 }), await pre({ amount: 350 }), await pre({ amount: 900 })];
  assert.deepEqual([small.permissionDecision, middle.permissionDecision, large.permissionDecision], ["allow", "ask", "deny"]);
  assert.equal(small.hookEventName, "PreToolUse");
  assert.ok(large.permissionDecisionReason.includes("900") && large.permissionDecisionReason.includes("500"));
});

test("e1 each limit belongs to the lower tier and the next cent goes up", async () => {
  const got = [];
  for (const a of [200, 200.01, 500, 500.01]) got.push(await decision(a));
  assert.deepEqual(got, ["allow", "ask", "ask", "deny"]);
  assert.equal(await decision(0.01), "allow");
});

test("e2 a missing or invalid amount is denied and other tools are left alone", async () => {
  for (const bad of [undefined, null, 0, -5, "100", true, NaN, Infinity, [50]]) assert.equal(await decision(bad), "deny", `amount ${String(bad)} must be denied`);
  assert.equal((await pre({})).permissionDecision, "deny");
  const other = await preRefund({ hook_event_name: "PreToolUse", tool_name: "get_order", tool_input: { amount: 9999 } }, "t1", undefined);
  assert.ok(other !== null && other !== undefined && Object.keys(other).length === 0);
});

test("e3 the output gets a date a status word and a decimal amount", async () => {
  const out = await post(JSON.stringify({ order: "A-7", created: 1700000000, status: 2, amount_cents: 12950, note: "ok" }));
  assert.ok(out !== null);
  assert.deepEqual(JSON.parse(out), { order: "A-7", created: "2023-11-14", status: "declined", amount: "129.50", note: "ok" });
  assert.equal(JSON.parse((await post(JSON.stringify({ created: 1700000000000, status: 0, amount_cents: 5 }))) ?? "{}").created, "2023-11-14");
  assert.deepEqual(JSON.parse((await post(JSON.stringify({ status: 9, amount_cents: 5 }))) ?? "{}"), { status: "unknown", amount: "0.05" });
});

test("e4 output that is not json or is already readable is left alone", async () => {
  assert.equal(await post("plain text, not json"), null);
  assert.equal(await post("[1, 2, 3]"), null);
  assert.equal(await post(JSON.stringify({ created: "2023-11-14", status: "approved", amount: "129.50" })), null);
  const again = await post(JSON.stringify({ created: 1700000000, status: 1 }));
  assert.ok(again !== null);
  assert.equal(await post(again), null);
});

test("e5 the hooks are registered on tool name matchers with a timeout", async () => {
  const o = buildOptions("/proj");
  assert.ok(o !== null && o !== undefined && o.cwd === "/proj");
  assert.deepEqual(Object.keys(o.hooks).sort(), ["PostToolUse", "PreToolUse"]);
  const [pm, qm] = [o.hooks.PreToolUse[0], o.hooks.PostToolUse[0]];
  assert.ok(pm.matcher === "process_refund" && pm.hooks.length === 1 && pm.hooks[0] === preRefund && pm.timeout > 0 && pm.timeout <= 10);
  assert.ok(qm.matcher === "get_order|get_refund" && qm.hooks.length === 1 && qm.hooks[0] === postNormalise && qm.timeout > 0 && qm.timeout <= 10);
  assert.equal(buildOptions("/proj", "/bin/x").pathToClaudeCodeExecutable, "/bin/x");
});

test("e6 a command hook blocks with exit two and a reason and fails closed on bad input", () => {
  const run = (payload: unknown) => {
    const out = commandHook(typeof payload === "string" ? payload : JSON.stringify(payload));
    assert.ok(out !== null && out !== undefined, "commandHook returned nothing");
    return out;
  };
  const pushed = run({ tool_name: "Bash", tool_input: { command: "git push origin main" } });
  assert.ok(pushed.exit === 2 && pushed.stderr.includes("pushed"));
  assert.equal(run({ tool_name: "Bash", tool_input: { command: "rm -rf build" } }).exit, 2);
  assert.deepEqual(run({ tool_name: "Bash", tool_input: { command: "git status" } }), { exit: 0, stderr: "" });
  assert.equal(run({ tool_name: "Read", tool_input: { command: "git push" } }).exit, 0);
  for (const broken of ["{not json", "", "[1]", "null"]) assert.ok(run(broken).exit === 2 && run(broken).stderr.length > 0, `${broken} must block`);
});

test("e7 the settings block runs the command hook on bash with a timeout", () => {
  const s = settingsHooks();
  assert.ok(s !== null && s !== undefined);
  const entry = s.hooks.PreToolUse[0];
  assert.equal(entry.matcher, "Bash");
  assert.deepEqual(entry.hooks, [{ type: "command", command: "python3 .claude/hooks/guard.py", timeout: 10 }]);
  assert.deepEqual(settingsHooks("sh guard.sh", 3).hooks.PreToolUse[0].hooks[0], { type: "command", command: "sh guard.sh", timeout: 3 });
});

test("e8 a run through the sdk denies the large refund and shows the model a readable order", async () => {
  const d = mkdtempSync(join(tmpdir(), "hooks-"));
  const order = JSON.stringify({ order: "A-7", created: 1700000000, status: 1, amount_cents: 12950 });
  const steps = [{ tool: { id: "t1", name: "get_order", input: { order: "A-7" }, output: order } },
    { tool: { id: "t2", name: "process_refund", input: { order: "A-7", amount: 900 }, output: "refunded" } },
    { tool: { id: "t3", name: "process_refund", input: { order: "A-7", amount: 50 }, output: "refunded" } },
    { result: { subtype: "success", result: "done", cost: 0.01, turns: 4 } }];
  const script = join(d, "script.json");
  writeFileSync(script, JSON.stringify({ session_id: "s1", turns: [steps] }));
  process.env.FAKE_CLAUDE_SCRIPT = script;
  process.env.FAKE_CLAUDE_RECORD = join(d, "record.jsonl");
  const options = buildOptions(d, FAKE);
  assert.ok(options !== null && options !== undefined);
  const results: Record<string, string> = {};
  try {
    for await (const m of query({ prompt: "Refund order A-7", options })) {
      const content = (m as any)?.message?.content;
      if ((m as any).type === "user" && Array.isArray(content)) {
        for (const b of content) if (b.type === "tool_result") results[b.tool_use_id] = typeof b.content === "string" ? b.content : JSON.stringify(b.content);
      }
    }
  } catch (e) {
    assert.fail(`the run did not finish: ${e}`);
  }
  assert.deepEqual(JSON.parse(results.t1 ?? "null"), { order: "A-7", created: "2023-11-14", status: "approved", amount: "129.50" });
  assert.ok(!(results.t2 ?? "").includes("refunded") && (results.t2 ?? "").includes("limit"));
  assert.equal(results.t3, "refunded");
});
