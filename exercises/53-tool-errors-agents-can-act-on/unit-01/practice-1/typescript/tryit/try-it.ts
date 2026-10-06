// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { ToolError, nextAction, runTool, toToolResult } from "./errors.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A scripted tool, like the one the tests use: it times out twice (a transient failure), then works.
const script: unknown[] = [new ToolError("transient", "The billing service timed out after 5 s."),
  new ToolError("transient", "The billing service timed out after 5 s."), "refund R-1 created"];
const waits: number[] = [];
const tool = (_args: Record<string, any>) => {
  const step = script.shift();
  if (step instanceof Error) throw step;
  return step;
};

// The sleep is injected, so nothing really waits: it records the delays asked for.
const result: any = runTool(tool, { order: "A-7", amount: 40 }, { max_retries: 2, base_delay_ms: 100 }, (ms) => waits.push(ms)) ?? {};

console.log("ok:", result.ok, "| content:", result.content, "| attempts:", result.attempts);
console.log("waits (ms):", JSON.stringify(waits));
console.log("tool_result block:", JSON.stringify(toToolResult("toolu_1", result)));
console.log("next action:", nextAction(result));
