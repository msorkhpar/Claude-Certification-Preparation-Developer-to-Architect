// Three loops over the same scripted replies: one ends on stop_reason, one on a word in the text, one after a fixed count.
//
// The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
// Only the first loop is right; the other two show what the two anti-patterns of the Architect exam do to a run.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

export const MODEL = "claude-sonnet-5-5";
const TOOLS: Anthropic.Tool[] = [
  { name: "save_file", description: "Save the report under a file name. Use it once the report is written.",
    input_schema: { type: "object", properties: { file: { type: "string" } }, required: ["file"] } },
  { name: "lookup", description: "Look up item number n and return its record.",
    input_schema: { type: "object", properties: { n: { type: "integer" } }, required: ["n"] } },
];

type Outcome = [status: string, calls: number, ran: string[], answer: string];

function runTools(reply: Anthropic.Message, ran: string[]): Anthropic.ToolResultBlockParam[] {
  const results: Anthropic.ToolResultBlockParam[] = [];
  for (const block of reply.content) {
    if (block.type === "tool_use") {
      ran.push(block.name);
      results.push({ type: "tool_result", tool_use_id: block.id, content: `${block.name} ok` });
    }
  }
  return results;
}

const textOf = (reply: Anthropic.Message) => reply.content.map((b) => (b.type === "text" ? b.text : "")).join("");
const ask = (client: Anthropic, messages: Anthropic.MessageParam[]) => client.messages.create({ model: MODEL, max_tokens: 500, tools: TOOLS, messages });

/** Right: the model's own stop_reason decides. The count is only a backstop with a status of its own. */
export async function byStopReason(client: Anthropic, task: string, backstop = 10): Promise<Outcome> {
  const messages: Anthropic.MessageParam[] = [{ role: "user", content: task }];
  const ran: string[] = [];
  let reply!: Anthropic.Message;
  for (let call = 1; call <= backstop; call++) {
    reply = await ask(client, messages);
    messages.push({ role: "assistant", content: reply.content });
    if (reply.stop_reason !== "tool_use") return ["done", call, ran, textOf(reply)];
    messages.push({ role: "user", content: runTools(reply, ran) });
  }
  return ["max_turns", backstop, ran, textOf(reply)];
}

/** Wrong: it reads the words. A reply that says 'done' ends the run, even with a tool call in the same reply. */
export async function byTextMarker(client: Anthropic, task: string): Promise<Outcome> {
  const messages: Anthropic.MessageParam[] = [{ role: "user", content: task }];
  const ran: string[] = [];
  for (let call = 1; ; call++) {
    const reply = await ask(client, messages);
    messages.push({ role: "assistant", content: reply.content });
    if (textOf(reply).toLowerCase().includes("done")) return ["done", call, ran, textOf(reply)];
    messages.push({ role: "user", content: runTools(reply, ran) });
  }
}

/** Wrong: the count is the loop. Whatever the third reply holds is returned as the answer. */
export async function byFixedCount(client: Anthropic, task: string, turns = 3): Promise<Outcome> {
  const messages: Anthropic.MessageParam[] = [{ role: "user", content: task }];
  const ran: string[] = [];
  let reply!: Anthropic.Message;
  for (let call = 1; call <= turns; call++) {
    reply = await ask(client, messages);
    messages.push({ role: "assistant", content: reply.content });
    if (reply.stop_reason === "tool_use") messages.push({ role: "user", content: runTools(reply, ran) });
  }
  return ["done", turns, ran, textOf(reply)];
}

const usage = { input_tokens: 1, output_tokens: 1 };
export const scenarioA = () => [
  { body: message([text("All done with the analysis. Saving it now."), { type: "tool_use", id: "toolu_01", name: "save_file", input: { file: "report.txt" } }], "tool_use", usage, MODEL) },
  { body: message([text("Saved report.txt.")], "end_turn", usage, MODEL) },
];
export const scenarioB = () => [1, 2, 3, 4].map((n) => ({ body: message([{ type: "tool_use", id: `toolu_0${n}`, name: "lookup", input: { n } }], "tool_use", usage, MODEL) }))
  .concat([{ body: message([text("Looked up 4 items.")], "end_turn", usage, MODEL) }]);

export function clientFor(replies: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(replies as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

/** Python's repr of a string, so that both languages print the same lines. */
const py = (s: string) => (s.includes("'") && !s.includes('"') ? `"${s}"` : `'${s}'`);

function show(label: string, [status, calls, ran, answer]: Outcome) {
  console.log(`  ${label.padEnd(26)} status=${status.padEnd(9)} model calls=${calls}  tools run=${ran.length}  text=${py(answer)}`);
}

async function main() {
  console.log("A: the reply says 'All done' and also calls save_file");
  show("stop_reason loop", await byStopReason(clientFor(scenarioA()).client, "Write and save the report."));
  show("text-marker loop", await byTextMarker(clientFor(scenarioA()).client, "Write and save the report."));
  console.log("B: the task needs four lookups, then the model ends its turn");
  show("stop_reason loop, cap 10", await byStopReason(clientFor(scenarioB()).client, "Look up items 1 to 4."));
  show("stop_reason loop, cap 3", await byStopReason(clientFor(scenarioB()).client, "Look up items 1 to 4.", 3));
  show("fixed-count loop of 3", await byFixedCount(clientFor(scenarioB()).client, "Look up items 1 to 4."));
}

if (import.meta.main) await main();
