// A tool loop on the official SDK, against a scripted model: parallel calls, one failing tool and a tool_choice that is kept.
// The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("tool_loop");

export const MODEL = "claude-sonnet-5-5";
export const TOOLS: Anthropic.Tool[] = [
  { name: "get_weather", description: "Current weather for one city. Use it when the user asks about weather now. Returns a short sentence; it knows nothing about forecasts.",
    input_schema: { type: "object", properties: { city: { type: "string", description: "City name, for example Oslo" } }, required: ["city"] } },
  { name: "get_time", description: "Local time for one city, as HH:MM on a 24 hour clock. Use it when the user asks what time it is somewhere.",
    input_schema: { type: "object", properties: { city: { type: "string", description: "City name, for example Oslo" } }, required: ["city"] } },
];
const DATA: Record<string, Record<string, string>> = { get_weather: { Oslo: "Oslo: 4 C, light rain" }, get_time: { Oslo: "09:15", Rome: "09:15" } };

export function runTool(block: { id: string; name: string; input: any }) {
  const content = DATA[block.name]?.[block.input.city];
  if (content === undefined) return { type: "tool_result" as const, tool_use_id: block.id, content: `No data for '${block.input.city}'. Known cities: Oslo, Rome.`, is_error: true };
  return { type: "tool_result" as const, tool_use_id: block.id, content };
}

export async function loop(client: Anthropic, question: string, toolChoice?: Anthropic.ToolChoice) {
  const messages: Anthropic.MessageParam[] = [{ role: "user", content: question }];
  let choice = toolChoice;
  for (;;) {
    const reply = await client.messages.create({ model: MODEL, max_tokens: 500, tools: TOOLS, messages, ...(choice ? { tool_choice: choice } : {}) });
    messages.push({ role: "assistant", content: reply.content });
    if (reply.stop_reason !== "tool_use") return { reply, messages };
    messages.push({ role: "user", content: reply.content.filter((b): b is Anthropic.ToolUseBlock => b.type === "tool_use").map((b) => runTool(b)) });
    if (choice && (choice.type === "any" || choice.type === "tool")) choice = undefined; // a forced choice applies to the first request only; auto and none stay
  }
}

const usage = { input_tokens: 1, output_tokens: 1 };
export const REPLIES = () => [
  { body: message([text("Checking all three."), { type: "tool_use", id: "toolu_01", name: "get_weather", input: { city: "Oslo" } }, { type: "tool_use", id: "toolu_02", name: "get_time", input: { city: "Oslo" } },
    { type: "tool_use", id: "toolu_03", name: "get_weather", input: { city: "Atlantis" } }], "tool_use", usage, MODEL) },
  { body: message([text("In Oslo it is 09:15 and 4 C with light rain. I have no weather data for Atlantis.")], "end_turn", usage, MODEL) },
];

export function clientFor(replies: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(replies as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

/** Python's repr of a small value, so that both languages print the same lines. */
function py(v: any): string {
  if (v === null || v === undefined) return "None";
  if (typeof v === "boolean") return v ? "True" : "False";
  if (typeof v === "string") return v.includes("'") && !v.includes('"') ? `"${v}"` : `'${v}'`;
  if (Array.isArray(v)) return `[${v.map(py).join(", ")}]`;
  if (typeof v === "object") return `{${Object.entries(v).map(([k, x]) => `${py(k)}: ${py(x)}`).join(", ")}}`;
  return String(v);
}

async function main() {
  const { fake, client } = clientFor(REPLIES());
  const { reply, messages } = await loop(client, "Weather and time in Oslo, and the weather in Atlantis?", { type: "auto", disable_parallel_tool_use: false });
  console.log("model calls:", fake.seen.length, "| stop reasons:", py(["tool_use", reply.stop_reason]));
  console.log("roles after the first reply:", py(messages.map((m) => m.role)));
  const results = messages[2].content as any[];
  console.log("tool results in ONE user message:", results.length, "| ids in order:", py(results.map((r) => r.tool_use_id)));
  for (const r of results) console.log(`  ${r.tool_use_id}: is_error=${py(r.is_error ?? false)} content=${py(r.content)}`);
  console.log("tool_choice sent on requests 1 and 2:", py(fake.seen.map((r) => r.body.tool_choice ?? null)));
  console.log("tool definitions sent carry no handler:", py(fake.seen[0].body.tools.every((t: any) => Object.keys(t).sort().join() === "description,input_schema,name")));
  console.log("final text:", (reply.content[0] as { text: string }).text);
}

if (import.meta.main) await main();
