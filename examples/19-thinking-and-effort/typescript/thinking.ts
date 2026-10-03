// Adaptive thinking steered by effort: the request, the reply's blocks and what the thinking cost.
// The reply is an illustrative, hand-written response in the API's shape (claude-opus-5-5), not a capture. It carries
// an omitted thinking block (the default display on this model: an empty `thinking` field and a signature) and the
// `output_tokens_details.thinking_tokens` breakdown the thinking page documents.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

export const MODEL = "claude-opus-5-5";
const PRICE_OUT = 20.0; // dollars per million output tokens, pricing page 2026-10-02
export const THINKING_BLOCK = { type: "thinking", thinking: "", signature: "illustrative-signature" };
export const USAGE = { input_tokens: 410, output_tokens: 1900, output_tokens_details: { thinking_tokens: 1650 } };

export function clientFor(...bodies: unknown[]) {
  const fake = scriptedFetch(bodies.map((body) => ({ body })));
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

export function request(client: Anthropic, effort: string) {
  return client.messages.create({
    model: MODEL,
    max_tokens: 8000,
    thinking: { type: "adaptive" },
    output_config: { effort } as any,
    messages: [{ role: "user", content: "Which of these two schedules has no conflicts?" }],
  } as any);
}

async function main() {
  const { fake, client } = clientFor(message([THINKING_BLOCK, text("Schedule B has no conflicts.")], "end_turn", USAGE as any), message([text("B.")], "end_turn", { input_tokens: 410, output_tokens: 12 }));
  const reply: any = await request(client, "high");
  const sent = fake.seen[0].body;
  console.log("thinking sent:", JSON.stringify(sent.thinking), "| effort sent:", JSON.stringify(sent.output_config));
  console.log("blocks:", JSON.stringify(reply.content.map((b: any) => b.type)), "| thinking text shown:", JSON.stringify(reply.content[0].thinking));
  const thinking = reply.usage.output_tokens_details.thinking_tokens;
  console.log(`output_tokens ${reply.usage.output_tokens} = thinking ${thinking} + answer ${reply.usage.output_tokens - thinking}`);
  console.log(`output cost: $${((reply.usage.output_tokens * PRICE_OUT) / 1_000_000).toFixed(4)} (thinking is billed as output, shown or not)`);
  const quick: any = await request(client, "low");
  console.log("a low-effort turn may skip thinking:", JSON.stringify(quick.content.map((b: any) => b.type)), "| output_tokens", quick.usage.output_tokens);
}

if (import.meta.main) await main();
