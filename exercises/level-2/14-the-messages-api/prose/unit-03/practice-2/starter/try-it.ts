// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { Conversation } from "./conversation.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A stand-in for the API, like the one the tests use: it answers every request the same way.
const fakeSend = (_body: Record<string, any>) => ({
  content: [{ type: "text", text: "Paris." }],
  stop_reason: "end_turn",
  usage: { input_tokens: 10, output_tokens: 5 },
});

const chat = new Conversation(fakeSend, "claude-sonnet-5-5", 64, "Be brief.");
const reply = chat.say("Capital of France?");
chat.say("Since when?");

console.log("reply text:", reply.text);
console.log("stop reason:", reply.stopReason, "| truncated:", reply.truncated);
console.log("history size:", chat.history().length);
console.log("totals:", chat.totals());
