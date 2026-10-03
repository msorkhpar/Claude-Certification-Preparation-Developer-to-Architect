import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { StreamError, assemble } = await import(pathToFileURL(resolve(dir, "assemble.ts")).href);

const start = (inputTokens = 25, outputTokens = 1) => ({
  type: "message_start",
  message: { id: "msg_x", type: "message", role: "assistant", model: "claude-sonnet-5-5", content: [], stop_reason: null, stop_sequence: null, usage: { input_tokens: inputTokens, output_tokens: outputTokens } },
});
const blockStart = (index: number, block: object) => ({ type: "content_block_start", index, content_block: block });
const delta = (index: number, kind: string, key: string, value: string) => ({ type: "content_block_delta", index, delta: { type: kind, [key]: value } });
const text = (index: number, value: string) => delta(index, "text_delta", "text", value);
const stop = (index: number) => ({ type: "content_block_stop", index });
const finish = (reason = "end_turn", outputTokens = 15) => [
  { type: "message_delta", delta: { stop_reason: reason, stop_sequence: null }, usage: { output_tokens: outputTokens } },
  { type: "message_stop" },
];
const TEXT_BLOCK = { type: "text", text: "" };

function errorOf(fn: () => unknown): unknown {
  try {
    fn();
  } catch (err) {
    return err;
  }
  return null;
}

test("m1 text deltas are joined and the message is complete", () => {
  const message = assemble([start(), blockStart(0, TEXT_BLOCK), text(0, "Hel"), text(0, "lo, "), text(0, "world."), stop(0), ...finish()]);
  assert.deepEqual(message.content, [{ type: "text", text: "Hello, world." }]);
  assert.deepEqual([message.id, message.role, message.model], ["msg_x", "assistant", "claude-sonnet-5-5"]);
  assert.deepEqual([message.stop_reason, message.stop_sequence], ["end_turn", null]);
});

test("e1 tool input is the fragments joined then parsed and empty input is an empty object", () => {
  const tool = { type: "tool_use", id: "toolu_1", name: "get_weather", input: {} };
  const events = [start(), blockStart(0, tool), delta(0, "input_json_delta", "partial_json", ""),
    delta(0, "input_json_delta", "partial_json", '{"ci'), delta(0, "input_json_delta", "partial_json", 'ty": "Pa'),
    delta(0, "input_json_delta", "partial_json", 'ris", "days": 3}'), stop(0),
    blockStart(1, { ...tool, id: "toolu_2" }), stop(1), ...finish("tool_use")];
  assert.deepEqual(assemble(events).content, [
    { type: "tool_use", id: "toolu_1", name: "get_weather", input: { city: "Paris", days: 3 } },
    { type: "tool_use", id: "toolu_2", name: "get_weather", input: {} },
  ]);
});

test("e2 ping and unknown event types are ignored", () => {
  const events = [start(), { type: "ping" }, blockStart(0, TEXT_BLOCK), { type: "ping" }, text(0, "ok"), { type: "some_future_event", data: { x: 1 } }, stop(0), ...finish()];
  assert.deepEqual(assemble(events).content, [{ type: "text", text: "ok" }]);
});

test("e3 an error event raises with its type and message", () => {
  const events = [start(), blockStart(0, TEXT_BLOCK), text(0, "partial"), { type: "error", error: { type: "overloaded_error", message: "Overloaded" } }];
  const err: any = errorOf(() => assemble(events));
  assert.ok(err instanceof StreamError, String(err));
  assert.deepEqual([err.errorType, err.detail], ["overloaded_error", "Overloaded"]);
});

test("e4 a stream that ends before message_stop is an error, not a short message", () => {
  const err: any = errorOf(() => assemble([start(), blockStart(0, TEXT_BLOCK), text(0, "cut off")]));
  assert.ok(err instanceof StreamError, String(err));
  assert.equal(err.errorType, "incomplete_stream");
});

test("e5 usage takes input tokens from the start and the cumulative output from the end", () => {
  const events = [start(52, 1), blockStart(0, TEXT_BLOCK), text(0, "x"), stop(0), ...finish("end_turn", 38)];
  assert.deepEqual(assemble(events).usage, { input_tokens: 52, output_tokens: 38 });
});

test("e6 blocks keep their index order and thinking fields are assembled", () => {
  const events = [start(), blockStart(0, { type: "thinking", thinking: "" }),
    delta(0, "thinking_delta", "thinking", "Let me "), delta(0, "thinking_delta", "thinking", "think."),
    delta(0, "signature_delta", "signature", "sig-abc"), stop(0),
    blockStart(1, TEXT_BLOCK), text(1, "Answer."), stop(1), ...finish()];
  assert.deepEqual(assemble(events).content, [
    { type: "thinking", thinking: "Let me think.", signature: "sig-abc" },
    { type: "text", text: "Answer." },
  ]);
});
