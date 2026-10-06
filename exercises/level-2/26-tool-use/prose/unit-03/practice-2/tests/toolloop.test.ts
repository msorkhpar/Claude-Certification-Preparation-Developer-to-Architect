import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { RequestError, ToolError, runAgent } = await import(pathToFileURL(resolve(dir, "toolloop.ts")).href);

const text = (t: string) => ({ type: "text", text: t });
const toolUse = (id: string, name: string, input: Record<string, unknown> = {}) => ({ type: "tool_use", id, name, input });
const reply = (content: unknown[], stop_reason = "end_turn") => ({ id: "msg_illustrative", type: "message", role: "assistant", model: "claude-sonnet-5-5", content, stop_reason, usage: { input_tokens: 1, output_tokens: 1 } });

/** A hand-written, illustrative model: returns the next reply and records a copy of each request. */
function scripted(...replies: any[]) {
  const seen: any[] = [];
  const ask = (request: any) => {
    seen.push(JSON.parse(JSON.stringify(request)));
    return replies.shift();
  };
  return { ask, seen };
}

const CALLS: Array<[string, any]> = [];
const schema = { type: "object", properties: { city: { type: "string" } }, required: ["city"] };
const tools = () => [
  { name: "get_weather", description: "Current weather for a city.", input_schema: schema, handler: (a: any) => { CALLS.push(["get_weather", a]); return `${a.city}: 18 C`; } },
  { name: "get_time", description: "Local time for a city.", input_schema: schema, handler: (a: any) => { CALLS.push(["get_time", a]); return `${a.city}: 14:05`; } },
  { name: "broken", description: "Always fails.", input_schema: schema, handler: () => { throw new ToolError("the weather service is down"); } },
  { name: "structured", description: "Returns an object.", input_schema: schema, handler: (a: any) => ({ city: a.city, temp_c: 18, tags: ["mild"] }) },
];

/** The RequestError field fn throws, "crash" for another error, null when it returns. */
function failureOf(fn: () => unknown): string | null {
  try {
    fn();
  } catch (err) {
    return err instanceof RequestError ? err.field : "crash";
  }
  return null;
}
/** The parsed JSON, or a marker string when the text is not JSON. */
function jsonOrText(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return `not JSON: ${text}`;
  }
}
const messagesOf = (m: { seen: any[] }, call: number): any[] => m.seen[call]?.messages ?? [];

test("m1 a tool call is run and its result sent back until the model ends its turn", () => {
  CALLS.length = 0;
  const first = [text("Let me check."), toolUse("tu_1", "get_weather", { city: "Oslo" })];
  const m = scripted(reply(first, "tool_use"), reply([text("It is 18 C in Oslo.")]));
  const r = runAgent(m.ask, tools(), "Weather in Oslo?") ?? {};
  assert.deepEqual([r.status, r.text, r.turns], ["done", "It is 18 C in Oslo.", 2]);
  assert.deepEqual(CALLS, [["get_weather", { city: "Oslo" }]]);
  const second = messagesOf(m, 1);
  assert.deepEqual(second.map((x) => x.role), ["user", "assistant", "user"]);
  assert.deepEqual(second[1]?.content, first);
  assert.deepEqual(second[2]?.content, [{ type: "tool_result", tool_use_id: "tu_1", content: "Oslo: 18 C" }]);
  const sent = m.seen[0];
  assert.ok(sent.model === "claude-sonnet-5-5" && JSON.stringify(sent.tools.map((t: any) => t.name)) === JSON.stringify(["get_weather", "get_time", "broken", "structured"]));
  assert.ok(sent.tools.every((t: any) => !("handler" in t)) && !("tool_choice" in sent));
});

test("e1 parallel calls get one user message with every result in order", () => {
  CALLS.length = 0;
  const content = [text("Checking both."), { type: "server_tool_use", id: "srvtoolu_1", name: "web_search", input: { query: "x" } },
    toolUse("tu_a", "get_time", { city: "Rome" }), toolUse("tu_b", "get_weather", { city: "Rome" })];
  const m = scripted(reply(content, "tool_use"), reply([text("done")]));
  runAgent(m.ask, tools(), "Rome?");
  const second = messagesOf(m, 1);
  assert.deepEqual(second.map((x) => x.role), ["user", "assistant", "user"]);
  assert.deepEqual(second[1]?.content, content);
  assert.deepEqual((second[2]?.content ?? []).map((r: any) => [r.type, r.tool_use_id, r.content]), [["tool_result", "tu_a", "Rome: 14:05"], ["tool_result", "tu_b", "Rome: 18 C"]]);
  assert.deepEqual(CALLS.map(([name]) => name), ["get_time", "get_weather"]);
});

test("e2 a failing unknown or malformed call becomes an error result and the loop goes on", () => {
  CALLS.length = 0;
  const calls = [toolUse("t1", "broken", { city: "Oslo" }), toolUse("t2", "teleport", { city: "Oslo" }), toolUse("t3", "get_weather"), toolUse("t4", "get_time", { city: "Oslo" })];
  const m = scripted(reply(calls, "tool_use"), reply([text("recovered")]));
  const r = runAgent(m.ask, tools(), "go") ?? {};
  assert.ok(r.status === "done" && r.text === "recovered");
  const results = messagesOf(m, 1)[2]?.content ?? [];
  assert.deepEqual(results.map((x: any) => [x.tool_use_id, x.is_error ?? false]), [["t1", true], ["t2", true], ["t3", true], ["t4", false]]);
  assert.ok(String(results[0]?.content).includes("weather service is down"));
  assert.ok(String(results[1]?.content).includes("teleport") && String(results[2]?.content).includes("city"));
  assert.deepEqual(CALLS, [["get_time", { city: "Oslo" }]]);
});

test("e3 the number of turns is bounded", () => {
  const m = scripted(...Array.from({ length: 10 }, (_, i) => reply([toolUse(`tu_${i}`, "get_time", { city: "Oslo" })], "tool_use")));
  const r = runAgent(m.ask, tools(), "go", "claude-sonnet-5-5", 3) ?? {};
  assert.deepEqual([r.status, r.turns], ["max_turns", 3]);
  assert.equal(m.seen.length, 3);
});

test("e4 refusal and truncation end the loop and a paused turn continues", () => {
  const refused = runAgent(scripted(reply([text("I can't help.")], "refusal")).ask, tools(), "go") ?? {};
  assert.deepEqual([refused.status, refused.turns], ["refused", 1]);
  const cut = runAgent(scripted(reply([text("The answer is")], "max_tokens")).ask, tools(), "go") ?? {};
  assert.deepEqual([cut.status, cut.text], ["truncated", "The answer is"]);
  const paused = [{ type: "server_tool_use", id: "srvtoolu_1", name: "web_search", input: { query: "x" } }];
  const m = scripted(reply(paused, "pause_turn"), reply([text("found it")]));
  const r = runAgent(m.ask, tools(), "search") ?? {};
  assert.deepEqual([r.status, r.text, r.turns], ["done", "found it", 2]);
  const again = messagesOf(m, 1);
  assert.deepEqual(again.map((x) => x.role), ["user", "assistant"]);
  assert.deepEqual(again[1]?.content, paused);
});

test("e5 tool choice is validated for the model and a forced choice applies to the first request only", () => {
  for (const choice of [{ type: "any" }, { type: "tool", name: "get_time" }]) {
    for (const modelId of ["claude-sonnet-5-5", "claude-opus-5-5", "claude-fable-5-1"]) {
      assert.equal(failureOf(() => runAgent(scripted(reply([text("x")])).ask, tools(), "go", modelId, 8, choice)), "tool_choice", `${JSON.stringify(choice)} ${modelId}`);
    }
  }
  assert.equal(failureOf(() => runAgent(scripted(reply([text("x")])).ask, tools(), "go", "claude-sonnet-5-5", 8, { type: "maybe" })), "tool_choice.type");
  assert.equal(failureOf(() => runAgent(scripted(reply([text("x")])).ask, tools(), "go", "claude-opus-5", 8, { type: "tool", name: "nope" })), "tool_choice.name");
  assert.equal(failureOf(() => runAgent(scripted(reply([text("x")])).ask, tools(), "go", "claude-sonnet-5-5", 8, { type: "auto", disable_parallel_tool_use: "yes" })), "tool_choice.disable_parallel_tool_use");
  const m = scripted(reply([toolUse("tu_1", "get_time", { city: "Oslo" })], "tool_use"), reply([text("ok")]));
  const r = runAgent(m.ask, tools(), "go", "claude-opus-5", 8, { type: "tool", name: "get_time" }) ?? {};
  assert.equal(r.status, "done");
  assert.deepEqual(m.seen.map((x) => x.tool_choice), [{ type: "tool", name: "get_time" }, { type: "auto" }]);
  const auto = scripted(reply([toolUse("tu_1", "get_time", { city: "Oslo" })], "tool_use"), reply([text("ok")]));
  runAgent(auto.ask, tools(), "go", "claude-sonnet-5-5", 8, { type: "auto", disable_parallel_tool_use: true });
  assert.deepEqual(auto.seen.map((x) => x.tool_choice), [{ type: "auto", disable_parallel_tool_use: true }, { type: "auto", disable_parallel_tool_use: true }]);
  const none = scripted(reply([text("ok")]));
  runAgent(none.ask, tools(), "go", "claude-sonnet-5-5", 8, { type: "none" });
  assert.deepEqual(none.seen[0]?.tool_choice, { type: "none" });
});

test("e6 results that are not text are sent as json text", () => {
  const m = scripted(reply([toolUse("tu_1", "structured", { city: "Oslo" })], "tool_use"), reply([text("ok")]));
  runAgent(m.ask, tools(), "go");
  const content = messagesOf(m, 1)[2]?.content?.[0]?.content;
  assert.equal(typeof content, "string");
  assert.deepEqual(jsonOrText(String(content)), { city: "Oslo", temp_c: 18, tags: ["mild"] });
});
