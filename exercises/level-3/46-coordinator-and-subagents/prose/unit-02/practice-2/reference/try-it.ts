// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { coordinate } from "./coordinator.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// The four model roles are plain functions, like the ones the tests script.
// Splits the question into three subtasks, each with the brief its subagent will get.
const planner = (_question: string) => ({ delegate: true, answer: null, subtasks: [
  { scope: "chips", brief: "chips: find 2024 chip supply news" },
  { scope: "cars", brief: "cars: find 2024 car output news" },
  { scope: "rates", brief: "rates: find 2024 interest rates" }] });
// A subagent knows only its brief: here it just reports on the topic that starts it.
const subagent = (brief: string) => brief.split(":")[0] + " report";
const reviewer = (_question: string, _findings: any[]) => [] as string[]; // no gaps: nothing more to ask
const synthesizer = (_question: string, findings: any[]) => findings.map((f) => f.text).join(" | ");

const result: any = coordinate(planner, subagent, reviewer, synthesizer, "How did supply change?") ?? {};

console.log("status:", result.status, "| subagent calls:", result.subagent_calls, "| rounds:", result.rounds);
console.log("findings:", JSON.stringify(result.findings));
console.log("answer:", result.answer);
