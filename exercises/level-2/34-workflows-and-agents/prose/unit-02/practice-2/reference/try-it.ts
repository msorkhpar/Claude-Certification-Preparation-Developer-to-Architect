// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { orchestrate } from "./workflows.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const PLAN = '["research the topic", "draft the outline", "check the facts"]';

// A stand-in for the model, like the one the tests script: the start of the prompt says which step is asking.
const model = (prompt: string): string => {
  if (prompt.startsWith("Plan")) return PLAN;
  if (prompt.startsWith("Subtask")) return "done: " + prompt.split("\n")[0].slice("Subtask: ".length);
  return "FINAL";
};

// The orchestrator asks for a plan, runs one worker per subtask, then combines the results.
const result: any = orchestrate(model, "Write a guide") ?? {};

console.log("status:", result.status, "| fallback:", result.fallback, "| calls:", result.calls);
console.log("plan:", JSON.stringify(result.plan));
console.log("results:", JSON.stringify((result.results ?? []).map((r: any) => [r.subtask, r.status])));
console.log("answer:", result.answer);
