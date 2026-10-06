import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";
import { ScriptedModel, reply, text, toolUse } from "./scripted.ts";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { runAgent } = await import(pathToFileURL(resolve(dir, "agent.ts")).href);

const tools = { add: ({ a, b }: { a: number; b: number }) => String(a + b) };

test("two turns returns final text", () => {
  const m = new ScriptedModel(
    reply([text("Adding."), toolUse("tu_1", "add", { a: 2, b: 3 })], "tool_use"),
    reply([text("The sum is 5.")], "end_turn"),
  );
  assert.equal(runAgent(m.call, tools, "2+3?"), "The sum is 5.");
  assert.equal(m.seen.length, 2);
});

test("tool results are appended as one user turn", () => {
  const m = new ScriptedModel(
    reply([toolUse("tu_1", "add", { a: 2, b: 3 }), toolUse("tu_2", "add", { a: 1, b: 1 })], "tool_use"),
    reply([text("done")], "end_turn"),
  );
  runAgent(m.call, tools, "go");
  const second = m.seen[1];
  assert.deepEqual(second.map((t) => t.role), ["user", "assistant", "user"]);
  const results = second[2].content as { type: string; tool_use_id: string; content: string }[];
  assert.deepEqual(results.map((r) => [r.type, r.tool_use_id, r.content]),
    [["tool_result", "tu_1", "5"], ["tool_result", "tu_2", "2"]]);
});

test("failing and unknown tools become error results", () => {
  const boom = () => { throw new Error("bad input"); };
  const m = new ScriptedModel(
    reply([toolUse("tu_1", "boom"), toolUse("tu_2", "nope")], "tool_use"),
    reply([text("recovered")], "end_turn"),
  );
  assert.equal(runAgent(m.call, { boom }, "go"), "recovered");
  const res = m.seen[1][2].content as { is_error: boolean; content: string }[];
  assert.equal(res[0].is_error, true);
  assert.match(res[0].content, /bad input/);
  assert.equal(res[1].is_error, true);
  assert.match(res[1].content, /nope/);
});

test("no tool use returns immediately", () => {
  const m = new ScriptedModel(reply([text("hi")], "end_turn"));
  assert.equal(runAgent(m.call, tools, "hello"), "hi");
  assert.equal(m.seen.length, 1);
});

test("turn cap throws", () => {
  const loop = Array.from({ length: 10 }, (_, i) => reply([toolUse(`tu_${i}`, "add", { a: 1, b: 1 })], "tool_use"));
  assert.throws(() => runAgent(new ScriptedModel(...loop).call, tools, "go", 3), /max_turns/);
});

test("text in a tool_use turn does not end the loop", () => {
  const m = new ScriptedModel(
    reply([text("I will call a tool."), toolUse("tu_1", "add", { a: 1, b: 2 })], "tool_use"),
    reply([text("3")], "end_turn"),
  );
  assert.equal(runAgent(m.call, tools, "go"), "3");
});
