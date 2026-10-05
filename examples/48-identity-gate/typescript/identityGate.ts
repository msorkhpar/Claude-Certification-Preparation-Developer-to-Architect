// The same scripted model, which skips identity verification, run against a loop that trusts the prompt and a loop that enforces the prerequisite in code.
//
// The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The system prompt asks for
// verification first in both runs: what differs is whether the code that runs the tools checks it.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("identity_gate");

export const MODEL = "claude-sonnet-5-5";
export const SYSTEM = "You are a support agent. Verify the customer's identity before any refund.";
const def = (name: string, description: string, properties: Record<string, unknown>): Anthropic.Tool =>
  ({ name, description, input_schema: { type: "object", properties, required: Object.keys(properties) } });
const TOOLS: Anthropic.Tool[] = [
  def("verify_identity", "Check the customer's identity code. Use it before any order or refund action.", { code: { type: "string" } }),
  def("lookup_order", "Look up one order of the verified customer.", { order_id: { type: "string" } }),
  def("process_refund", "Refund an amount in cents on a looked-up order.", { order_id: { type: "string" }, amount_cents: { type: "integer" } }),
];

export class Backend {
  log: string[] = [];
  verified = false;

  call(name: string, args: any): string {
    this.log.push(name);
    if (name === "verify_identity") {
      this.verified = args.code === "1234";
      return this.verified ? "verified=yes" : "verified=no";
    }
    return ({ lookup_order: "order_id=O1; total_cents=5000", process_refund: "refund_id=R1" } as Record<string, string>)[name];
  }
}

/** The prerequisite, in code: nothing but verification runs before the customer is verified. */
export function gate(backend: Backend, name: string, args: any): [string, boolean] {
  if (name !== "verify_identity" && !backend.verified) return ["BLOCKED identity_required: Verify the customer's identity before this action.", true];
  return [backend.call(name, args), false];
}

export async function run(client: Anthropic, backend: Backend, gated: boolean) {
  const messages: Anthropic.MessageParam[] = [{ role: "user", content: "Please refund 20.00 on order O1. My code is 1234." }];
  const results: Array<[string, string, boolean]> = [];
  for (;;) {
    const reply = await client.messages.create({ model: MODEL, max_tokens: 500, system: SYSTEM, tools: TOOLS, messages });
    messages.push({ role: "assistant", content: reply.content });
    if (reply.stop_reason !== "tool_use") return results;
    const out: Anthropic.ToolResultBlockParam[] = [];
    for (const block of reply.content) {
      if (block.type === "tool_use") {
        const [content, isError] = gated ? gate(backend, block.name, block.input) : [backend.call(block.name, block.input), false] as [string, boolean];
        results.push([block.name, content, isError]);
        out.push({ type: "tool_result", tool_use_id: block.id, content, ...(isError ? { is_error: true } : {}) });
      }
    }
    messages.push({ role: "user", content: out });
  }
}

const usage = { input_tokens: 1, output_tokens: 1 };
const call = (id: string, name: string, input: Record<string, unknown>) => ({ type: "tool_use", id, name, input });
export function replies(gated: boolean) {
  const first = { body: message([text("Refunding now."), call("toolu_01", "process_refund", { order_id: "O1", amount_cents: 2000 })], "tool_use", usage, MODEL) };
  if (!gated) return [first, { body: message([text("Refund issued.")], "end_turn", usage, MODEL) }];
  return [first,
    { body: message([call("toolu_02", "verify_identity", { code: "1234" })], "tool_use", usage, MODEL) },
    { body: message([call("toolu_03", "lookup_order", { order_id: "O1" })], "tool_use", usage, MODEL) },
    { body: message([call("toolu_04", "process_refund", { order_id: "O1", amount_cents: 2000 })], "tool_use", usage, MODEL) },
    { body: message([text("Refund issued after verification.")], "end_turn", usage, MODEL) }];
}

export function clientFor(script: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(script as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

const py = (b: boolean) => (b ? "True" : "False");
const pyList = (items: string[]) => `[${items.map((s) => `'${s}'`).join(", ")}]`;

async function main() {
  console.log("system prompt in both runs:", SYSTEM);
  for (const [label, gated] of [["prompt only", false], ["code gate  ", true]] as const) {
    const { fake, client } = clientFor(replies(gated));
    const backend = new Backend();
    const results = await run(client, backend, gated);
    const at = backend.log.indexOf("verify_identity");
    const before = at >= 0 ? backend.log.indexOf("process_refund") < at : backend.log.includes("process_refund");
    console.log(`${label}: backend calls = ${pyList(backend.log)}; refund before verification: ${py(before)}`);
    if (gated) console.log(`${label}: first result sent back to the model: ${results[0][1]} (is_error=${py(results[0][2])})`);
    assert(fake.seen.every((r) => r.body.system === SYSTEM));
  }
}

function assert(condition: boolean) {
  if (!condition) throw new Error("the system prompt differed between requests");
}

if (import.meta.main) await main();
