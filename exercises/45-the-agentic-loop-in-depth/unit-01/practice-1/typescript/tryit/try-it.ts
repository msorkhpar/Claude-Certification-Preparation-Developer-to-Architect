// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { runAgent } from "./agent.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const lookup = (args: Record<string, any>) => `record ${args.n}`;

// A scripted model, like the one the tests use: it asks for one tool call, then ends its turn.
const replies: any[] = [
  { stop_reason: "tool_use", content: [{ type: "text", text: "Looking." }, { type: "tool_use", id: "t1", name: "lookup", input: { n: 1 } }] },
  { stop_reason: "end_turn", content: [{ type: "text", text: "Record 1 found." }] },
];
const model = (messages: any[]) => {
  console.log("model called with", messages.length, "messages");
  return replies.shift() ?? { stop_reason: "end_turn", content: [{ type: "text", text: "script ran out" }] };
};

const result = runAgent(model, { lookup }, "find record 1") ?? {};

console.log("status:", result.status, "| turns:", result.turns);
console.log("final text:", result.text);
console.log("roles:", (result.messages ?? []).map((m: any) => m.role).join(", "));
