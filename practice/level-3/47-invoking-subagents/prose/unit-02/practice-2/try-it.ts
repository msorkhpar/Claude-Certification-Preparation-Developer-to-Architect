// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { buildOptions, makeBrief, mergeFindings, packageFinding } from "./subagents.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// The options register each named subagent the model may start with the Agent tool.
const specs = { reviewer: { description: "Reviews one module for security problems.", prompt: "You review code.", tools: ["Read", "Grep"], model: "sonnet" } };
const options: any = buildOptions(specs, "/proj");
console.log("registered agents:", Object.keys(options?.agents ?? {}).join(", "));
console.log("allowed tools:", JSON.stringify(options?.allowedTools));

// The brief carries the facts a subagent cannot get any other way: it does not see the conversation.
try {
  const brief = makeBrief("Review the payment module for security problems",
    ["src/payments.py"], ["Amounts are in cents"], "a list of findings, one per line");
  console.log(brief);
} catch (err) {
  console.log("brief refused:", String(err));
}

// Findings keep their sources; the same claim from two subagents is merged and keeps both.
const a = packageFinding("Refunds take 5 days", "https://example.invalid/refunds", "Refund policy");
const b = packageFinding("refunds take 5 days", "https://example.invalid/faq", undefined, 3);
const c = packageFinding("Shipping is free over 50 euros");
console.log("merged:", JSON.stringify(mergeFindings([a, b, c] as any)));
