// A streamed reply read three ways, from a scripted server-sent-event body.
// The stream is an illustrative, hand-written sequence of events in the API's framing
// (claude-sonnet-5-5): one text block, then one tool_use block whose input arrives in fragments.
import Anthropic from "@anthropic-ai/sdk";
import { scriptedFetch } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("streaming");

const MODEL = "claude-sonnet-5-5";
export const PARAMS = {
  model: MODEL,
  max_tokens: 128,
  messages: [{ role: "user" as const, content: "Weather in Paris?" }],
  tools: [{ name: "get_weather", description: "Weather for a city.", input_schema: { type: "object" as const, properties: { city: { type: "string" } }, required: ["city"] } }],
};

const delta = (index: number, kind: string, key: string, value: string) => ({ type: "content_block_delta", index, delta: { type: kind, [key]: value } });

export const EVENTS = [
  { type: "message_start", message: { id: "msg_illustrative", type: "message", role: "assistant", model: MODEL, content: [], stop_reason: null, stop_sequence: null, usage: { input_tokens: 52, output_tokens: 1 } } },
  { type: "content_block_start", index: 0, content_block: { type: "text", text: "" } },
  { type: "ping" },
  delta(0, "text_delta", "text", "Let me "),
  delta(0, "text_delta", "text", "check."),
  { type: "content_block_stop", index: 0 },
  { type: "content_block_start", index: 1, content_block: { type: "tool_use", id: "toolu_illustrative_1", name: "get_weather", input: {} } },
  delta(1, "input_json_delta", "partial_json", ""),
  delta(1, "input_json_delta", "partial_json", '{"ci'),
  delta(1, "input_json_delta", "partial_json", 'ty": "Par'),
  delta(1, "input_json_delta", "partial_json", 'is"}'),
  { type: "content_block_stop", index: 1 },
  { type: "message_delta", delta: { stop_reason: "tool_use", stop_sequence: null }, usage: { output_tokens: 38 } },
  { type: "message_stop" },
];
export const FAILING = [...EVENTS.slice(0, 5), { type: "error", error: { type: "overloaded_error", message: "Overloaded" } }];

export function clientFor(events: Array<{ type: string }>) {
  const fake = scriptedFetch([{ sse: events }]);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

function runLength(names: string[]): string {
  const out: Array<[string, number]> = [];
  for (const name of names) {
    if (out.length && out[out.length - 1][0] === name) out[out.length - 1][1]++;
    else out.push([name, 1]);
  }
  return out.map(([n, c]) => (c === 1 ? n : `${n} x${c}`)).join(", ");
}

async function main() {
  let { fake, client } = clientFor(EVENTS);
  const pieces: string[] = [];
  const stream = client.messages.stream(PARAMS).on("text", (t) => pieces.push(t));
  const final = await stream.finalMessage();
  console.log("request sets stream:", fake.seen[0].body.stream);
  console.log("text pieces:", JSON.stringify(pieces));
  console.log("final stop_reason:", final.stop_reason, "| usage:", final.usage.input_tokens, "in,", final.usage.output_tokens, "out");
  console.log("blocks:", final.content.map((b) => b.type), "| tool input:", JSON.stringify((final.content[1] as any).input));

  ({ client } = clientFor(EVENTS));
  const types: string[] = [];
  for await (const event of await client.messages.create({ ...PARAMS, stream: true })) types.push(event.type);
  console.log("raw events:", runLength(types));

  ({ client } = clientFor(FAILING));
  try {
    await client.messages.stream(PARAMS).finalMessage();
  } catch (err) {
    if (err instanceof Anthropic.APIError) console.log("mid-stream error:", err.constructor.name, (err.error as any)?.error?.type ?? err.message);
    else throw err;
  }
}

if (import.meta.main) await main();
