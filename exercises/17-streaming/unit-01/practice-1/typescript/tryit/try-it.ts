// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { StreamError, assemble } from "./assemble.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// The parsed `data:` events of one streamed reply, like the ones the tests build: start, one text block in three deltas, finish.
const events = [
  { type: "message_start", message: { id: "msg_x", type: "message", role: "assistant", model: "claude-sonnet-5-5",
    content: [], stop_reason: null, stop_sequence: null, usage: { input_tokens: 25, output_tokens: 1 } } },
  { type: "content_block_start", index: 0, content_block: { type: "text", text: "" } },
  { type: "content_block_delta", index: 0, delta: { type: "text_delta", text: "Hel" } },
  { type: "content_block_delta", index: 0, delta: { type: "text_delta", text: "lo, " } },
  { type: "content_block_delta", index: 0, delta: { type: "text_delta", text: "world." } },
  { type: "content_block_stop", index: 0 },
  { type: "message_delta", delta: { stop_reason: "end_turn", stop_sequence: null }, usage: { output_tokens: 15 } },
  { type: "message_stop" },
];

try {
  const message = assemble(events) ?? {};
  console.log("content:", JSON.stringify(message.content));
  console.log("stop reason:", message.stop_reason);
  console.log("usage:", JSON.stringify(message.usage));
} catch (err) {
  if (err instanceof StreamError) console.log("stream error:", err.message);
  else throw err;
}
