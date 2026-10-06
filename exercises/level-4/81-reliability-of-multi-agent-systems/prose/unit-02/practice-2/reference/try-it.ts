// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { Transient, runPlan } from "./reliableAgents.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const task = (id: string, needs: string[] = []) => ({ id, agent: "w", key: `key-${id}`, needs, fallback: null });

// A stand-in for a worker agent, like the tests use: its first call is lost, then it answers.
let calls = 0;
const flaky = (key: string, inputs: Record<string, string>) => {
  calls += 1;
  if (calls === 1) throw new Transient("lost");
  return key + "|" + Object.keys(inputs).sort().map((k) => inputs[k]).join(",");
};

// Task b needs the result of a; the lost call is retried, the result is checkpointed in the store.
const store: Record<string, string> = {};
const result = runPlan([task("a"), task("b", ["a"])], { w: flaky }, store);
console.log("done:", result.done);
console.log("attempts:", result.attempts);
console.log("failed:", result.failed, "| skipped:", result.skipped);
console.log("store:", store);
