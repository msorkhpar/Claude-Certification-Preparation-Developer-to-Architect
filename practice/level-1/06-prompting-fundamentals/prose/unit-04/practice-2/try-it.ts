// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { buildPrompt } from "./promptBuilder.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A small spec like the first main test case: a role, one document, one constraint and a task with a placeholder.
const spec = {
  role: "You are a careful support analyst for {{company}}.",
  documents: [{ name: "policy.txt", text: "Refunds within 30 days." }],
  constraints: ["Answer in one word."],
  task: "Classify the message about {{topic}}.",
};
const prompt = buildPrompt(spec, { company: "Acme", topic: "delivery" });

console.log("prompt length:", prompt.length);
console.log("starts with:", JSON.stringify(prompt.slice(0, 20)));
console.log("has task block:", prompt.includes("<task>\nClassify the message about delivery.\n</task>"));
console.log(prompt);
