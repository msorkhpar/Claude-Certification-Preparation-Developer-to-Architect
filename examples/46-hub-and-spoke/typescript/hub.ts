// Hub and spoke on the Messages API: a coordinator plans by calling a plan tool, each subagent is its own conversation, the coordinator synthesizes.
//
// The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The point is what each
// request contains: a subagent's request holds its brief and nothing else, and only the synthesis request holds the findings.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

export const MODEL = "claude-sonnet-5-5";
const PLAN_TOOL: Anthropic.Tool = {
  name: "plan", description: "Record the plan: one subtask per independent part of the question, each with a scope and a self-contained brief.",
  input_schema: { type: "object", properties: { subtasks: { type: "array", items: { type: "object", properties: { scope: { type: "string" }, brief: { type: "string" } }, required: ["scope", "brief"] } } }, required: ["subtasks"] },
};
export const SUBAGENT_SYSTEM = "You are a research subagent. Answer only the brief you are given and end with a one-line source note.";

export const SUBTASKS = [
  { scope: "chips", brief: "Find what changed in 2024 chip supply. Return three bullet points and a source note. Do not cover cars or interest rates." },
  { scope: "cars", brief: "Find what changed in 2024 car output. Return three bullet points and a source note. Do not cover chips or interest rates." },
  { scope: "rates", brief: "Find what changed in 2024 interest rates. Return three bullet points and a source note. Do not cover chips or cars." },
];
export const REPORTS = ["CHIPS-REPORT: output of foundries rose, lead times fell. Source: industry survey.",
  "CARS-REPORT: plants ran fuller as chips arrived. Source: producer filings.",
  "RATES-REPORT: central banks held rates, loans stayed dear. Source: bank statements."];

const textOf = (reply: Anthropic.Message) => reply.content.map((b) => (b.type === "text" ? b.text : "")).join("");

export async function plan(client: Anthropic, question: string) {
  // tool_choice stays auto: Claude Sonnet 5.5 returns a 400 error for a forced choice, so the request asks for the tool in words
  const reply = await client.messages.create({ model: MODEL, max_tokens: 800, tools: [PLAN_TOOL], messages: [{ role: "user", content: `${question}\nRecord your plan by calling the plan tool.` }] });
  const block = reply.content.find((b): b is Anthropic.ToolUseBlock => b.type === "tool_use")!;
  return (block.input as { subtasks: Array<{ scope: string; brief: string }> }).subtasks;
}

/** A fresh conversation: the system prompt of the role and the brief. Nothing of the coordinator's history is passed. */
export async function runSubagent(client: Anthropic, brief: string) {
  return textOf(await client.messages.create({ model: MODEL, max_tokens: 800, system: SUBAGENT_SYSTEM, messages: [{ role: "user", content: brief }] }));
}

export async function synthesize(client: Anthropic, question: string, findings: Array<[string, string]>) {
  const listing = findings.map(([scope, report]) => `[${scope}] ${report}`).join("\n");
  return textOf(await client.messages.create({ model: MODEL, max_tokens: 800, messages: [{ role: "user", content: `Question: ${question}\nFindings:\n${listing}\nWrite one answer that uses every finding.` }] }));
}

const usage = { input_tokens: 1, output_tokens: 1 };
export const replies = () => [
  { body: message([text("Three independent parts."), { type: "tool_use", id: "toolu_01", name: "plan", input: { subtasks: SUBTASKS } }], "tool_use", usage, MODEL) },
  ...REPORTS.map((r) => ({ body: message([text(r)], "end_turn", usage, MODEL) })),
  { body: message([text("Chip supply recovered first, car output followed, and rates stayed high.")], "end_turn", usage, MODEL) },
];

export function clientFor(script: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(script as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

async function main() {
  const question = "How did the 2024 supply picture change for chips, cars and interest rates?";
  const { fake, client } = clientFor(replies());
  const subtasks = await plan(client, question);
  console.log("plan:", subtasks.length, "subtasks ->", `[${subtasks.map((s) => `'${s.scope}'`).join(", ")}]`);
  const findings: Array<[string, string]> = [];
  for (const [index, task] of subtasks.entries()) {
    const report = await runSubagent(client, task.brief);
    findings.push([task.scope, report]);
    const request = fake.seen[index + 1].body;
    const body = JSON.stringify(request);
    const others = REPORTS.filter((r) => r !== report && body.includes(r.split(":")[0]));
    const flag = (b: boolean) => (b ? "True" : "False");
    console.log(`subagent ${index + 1} request: ${request.messages.length} message, system prompt of the role: ${flag(request.system === SUBAGENT_SYSTEM)}, ` +
      `holds the coordinator's question: ${flag(body.includes(question))}, holds another report: ${flag(others.length > 0)}`);
  }
  const answer = await synthesize(client, question, findings);
  const synthesis = JSON.stringify(fake.seen[4].body);
  console.log("synthesis request: reports included:", REPORTS.filter((r) => synthesis.includes(r.split(":")[0])).length, "of", REPORTS.length);
  console.log("model calls:", fake.seen.length, "| one plan, three subagents, one synthesis");
  console.log("answer:", answer);
}

if (import.meta.main) await main();
