// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { runComputerLoop } from "./computer.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A toy screen in memory: a search box and a payment button.
const screen = { width: 1920, height: 1080, cursor: [0, 0], typed: "", log: [] as unknown[][],
  elements: [{ id: "search", x: 1000, y: 200, w: 400, h: 40, risk: "none" }, { id: "pay", x: 800, y: 600, w: 200, h: 60, risk: "payment" }] };

const use = (id: string, name: string, input: Record<string, unknown> = {}) =>
  ({ type: "tool_use", id, name, toolset_name: "computer", input });

// The model is a function that returns scripted replies in order, like the one the tests use.
const replies = [
  { content: [use("t1", "screenshot")], stop_reason: "tool_use" },
  { content: [use("t2", "left_click", { coordinate: [894, 164] }), use("t3", "type", { text: "weather" }), use("t4", "screenshot")], stop_reason: "tool_use" },
  { content: [{ type: "text", text: "Typed it." }], stop_reason: "end_turn" },
];
const requests: number[] = [];
const ask = (request: any) => {
  requests.push(request.messages.length);
  // when the script runs out, the model just ends the conversation
  return replies.shift() ?? { content: [{ type: "text", text: "script ran out" }], stop_reason: "end_turn" };
};

const result: any = runComputerLoop(ask, screen) ?? {};

console.log("status and turns:", result.status, result.turns);
console.log("messages in each request:", requests.join(", "));
console.log("actions performed on the screen:", JSON.stringify(screen.log));
console.log("typed text:", JSON.stringify(screen.typed));
