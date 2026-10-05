// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { runAgent } from "./toolloop.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const tools = [{
  name: "get_weather",
  description: "Current weather for a city.",
  input_schema: { type: "object", properties: { city: { type: "string" } }, required: ["city"] },
  handler: (input: Record<string, any>) => `${input.city}: 18 C`,
}];

// A stand-in for the model, like the one the tests use: it asks for one tool call, then ends its turn.
const replies: any[] = [
  { content: [{ type: "text", text: "Let me check." }, { type: "tool_use", id: "tu_1", name: "get_weather", input: { city: "Oslo" } }], stop_reason: "tool_use" },
  { content: [{ type: "text", text: "It is 18 C in Oslo." }], stop_reason: "end_turn" },
];
const ask = (_request: Record<string, any>) => replies.shift();

const result = runAgent(ask, tools, "Weather in Oslo?");

console.log("status:", result.status);
console.log("text:", result.text);
console.log("model calls:", result.turns);
for (const message of result.messages) console.log("message:", message.role, JSON.stringify(message.content));
