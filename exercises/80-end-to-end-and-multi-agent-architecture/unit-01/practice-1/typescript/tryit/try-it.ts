// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { cheapestAdequate, review, verdict } from "./architectureReview.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const stages = { input: ["parse"], processing: ["classify", "route"], output: ["validate", "send"], feedback: ["review a sample"] };
const design = (over: Record<string, any> = {}) => ({
  name: "intake", pattern: "workflow", agents: 1, cost: 3, path_known: true, parallel_independent: false,
  shared_context: false, needs_audit: true, writes_without_approval: false, stages, ...over,
});

// A sound workflow design passes; a multi-agent design that writes without approval and has no feedback loop does not.
const sound = design();
const risky = design({ name: "research", pattern: "multi-agent", agents: 4, writes_without_approval: true, stages: { ...stages, feedback: [] } });
for (const d of [sound, risky]) {
  const findings = review(d);
  console.log(d.name, "->", verdict(findings), findings.map((f: any) => f.rule));
}
console.log("cheapest design that is not rejected:", cheapestAdequate([sound, risky]));
