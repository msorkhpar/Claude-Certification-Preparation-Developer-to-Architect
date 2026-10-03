import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { runAgent } = await import(pathToFileURL(resolve(dir, "agent.ts")).href);

const text = (t: string) => ({ type: "text", text: t });
const call = (id: string, name: string, input: Record<string, unknown> = {}) => ({ type: "tool_use", id, name, input });
const reply = (stop_reason: string, ...content: unknown[]) => ({ stop_reason, content });

/** A scripted model: replies in order, or the last one again when `repeat`; it keeps a snapshot of every request. */
function scripted(replies: any[], repeat = false) {
  const seen: any[][] = [];
  const fn = (messages: any[]) => {
    seen.push([...messages]);
    if (replies.length > 1 || !repeat) {
      if (replies.length === 0) throw new assert.AssertionError({ message: "the loop called the model again after the script ended" });
      return replies.shift();
    }
    return replies[0];
  };
  return Object.assign(fn, { seen });
}

/** Handlers that record every call. */
function tools(handlers: Record<string, (input: any) => string>) {
  const calls: Array<[string, any]> = [];
  const wrapped: Record<string, (input: any) => string> = {};
  for (const [name, fn] of Object.entries(handlers)) wrapped[name] = (input) => { calls.push([name, input]); return fn(input); };
  return Object.assign(wrapped, { calls }) as Record<string, (input: any) => string> & { calls: Array<[string, any]> };
}

const lookup = (input: any) => `record ${input.n}`;

test("m1 a run alternates model and tools until the model ends its turn", () => {
  const model = scripted([reply("tool_use", text("Looking."), call("t1", "lookup", { n: 1 })), reply("end_turn", text("Record 1 found."))]);
  const t = tools({ lookup });
  const result = runAgent(model, t, "find record 1") ?? {};
  assert.deepEqual([result.status, result.text, result.turns], ["done", "Record 1 found.", 2]);
  assert.deepEqual((result.messages ?? []).map((m: any) => m.role), ["user", "assistant", "user", "assistant"]);
  assert.deepEqual(result.messages[0], { role: "user", content: "find record 1" });
  assert.deepEqual(result.messages[2].content, [{ type: "tool_result", tool_use_id: "t1", content: "record 1" }]);
  assert.deepEqual(model.seen.map((s) => s.length), [1, 3]);
  assert.deepEqual(t.calls, [["lookup", { n: 1 }]]);
});

test("e1 the stop reason decides and the words of the text do not", () => {
  const model = scripted([reply("tool_use", text("All done. Saving now."), call("t1", "lookup", { n: 7 })), reply("end_turn", text("Saved."))]);
  const t = tools({ lookup });
  const result = runAgent(model, t, "go") ?? {};
  assert.deepEqual([result.status, result.turns, t.calls], ["done", 2, [["lookup", { n: 7 }]]]);
  const announce = scripted([reply("end_turn", text("Next I will call the lookup tool."))]);
  const again = tools({ lookup });
  const ended = runAgent(announce, again, "go") ?? {};
  assert.deepEqual([ended.status, ended.turns, again.calls, announce.seen.length], ["done", 1, [], 1]);
});

test("e2 every call of a turn is answered in one user message in order", () => {
  const first = reply("tool_use", text("Three at once."), call("a", "lookup", { n: 1 }), call("b", "lookup", { n: 2 }), call("c", "lookup", { n: 3 }));
  const result = runAgent(scripted([first, reply("end_turn", text("ok"))]), tools({ lookup }), "go") ?? {};
  const messages = result.messages ?? [];
  assert.deepEqual(messages.map((m: any) => m.role), ["user", "assistant", "user", "assistant"]);
  assert.deepEqual(messages[1].content, first.content);
  assert.deepEqual(messages[2].content, [["a", 1], ["b", 2], ["c", 3]].map(([id, n]) => ({ type: "tool_result", tool_use_id: id, content: `record ${n}` })));
});

test("e3 a failing or unknown tool becomes an error result and the run goes on", () => {
  const broken = () => { throw new Error("database offline"); };
  const model = scripted([reply("tool_use", call("a", "broken"), call("b", "missing"), call("c", "lookup", { n: 5 })), reply("end_turn", text("Partly done."))]);
  const t = tools({ broken, lookup });
  const result = runAgent(model, t, "go") ?? {};
  assert.equal(result.status, "done");
  assert.deepEqual(result.messages[2].content, [
    { type: "tool_result", tool_use_id: "a", content: "database offline", is_error: true },
    { type: "tool_result", tool_use_id: "b", content: "Unknown tool: missing", is_error: true },
    { type: "tool_result", tool_use_id: "c", content: "record 5" }]);
  assert.deepEqual(t.calls.map(([name]) => name), ["broken", "lookup"]);
});

test("e4 the turn limit is a backstop that ends only a run the model has not ended", () => {
  const endless = scripted([reply("tool_use", call("t", "lookup", { n: 1 }))], true);
  const t = tools({ lookup });
  const result = runAgent(endless, t, "go", 3) ?? {};
  assert.deepEqual([result.status, result.turns, endless.seen.length, t.calls.length], ["max_turns", 3, 3, 3]);
  assert.ok(result.messages.at(-1).role === "user" && result.messages.length === 7);
  const last = scripted([reply("tool_use", call("a", "lookup", { n: 1 })), reply("tool_use", call("b", "lookup", { n: 2 })), reply("end_turn", text("Finished on the last turn."))]);
  const done = runAgent(last, tools({ lookup }), "go", 3) ?? {};
  assert.deepEqual([done.status, done.turns, done.text], ["done", 3, "Finished on the last turn."]);
});

test("e5 a cut off or refused reply ends the run with its own status", () => {
  const expected: Record<string, string> = { max_tokens: "truncated", refusal: "refused", stop_sequence: "done", some_new_reason: "unexpected" };
  for (const [stopReason, status] of Object.entries(expected)) {
    const model = scripted([reply(stopReason, text("partial words"))]);
    const result = runAgent(model, tools({ lookup }), "go") ?? {};
    assert.deepEqual([result.status, result.text, result.turns, model.seen.length], [status, "partial words", 1, 1], stopReason);
  }
});

test("e6 a tool use reply without a tool call is malformed and sends nothing more", () => {
  const model = scripted([reply("tool_use", text("I will call a tool."))]);
  const result = runAgent(model, tools({ lookup }), "go") ?? {};
  assert.deepEqual([result.status, result.turns, model.seen.length, (result.messages ?? []).length], ["malformed", 1, 1, 2]);
});
