// Count tokens before sending, then price the reply from its usage object.
// Both calls go through a scripted fetch, so nothing leaves the container. The replies are illustrative,
// hand-written responses in the API's shapes (claude-sonnet-5-5), not captures. Prices are dollars per million
// tokens, read from the Claude pricing page on 2026-10-02.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("cost");

export const MODEL = "claude-sonnet-5-5";
export const PRICES: Record<string, [number, number, number]> = {
  // input, output, cache-read multiplier
  "claude-haiku-4-5-20251001": [1.0, 5.0, 0.1],
  "claude-sonnet-5-5": [2.0, 10.0, 0.1],
  "claude-opus-5-5": [4.0, 20.0, 0.05],
  "claude-fable-5-1": [10.0, 50.0, 0.025],
};
export const PARAMS = {
  model: MODEL,
  max_tokens: 300,
  system: "You answer from the policy document.",
  messages: [{ role: "user" as const, content: "Summarise the refund policy in two sentences." }],
};
export const USAGE = {
  input_tokens: 120, output_tokens: 340, cache_read_input_tokens: 0, cache_creation_input_tokens: 4000,
  cache_creation: { ephemeral_5m_input_tokens: 4000, ephemeral_1h_input_tokens: 0 },
};

/** Dollars for one request: cache writes cost 1.25x input (5 minutes) or 2x (1 hour), reads a model-specific fraction. */
export function cost(model: string, usage: any, batch = false): number {
  const [priceIn, priceOut, read] = PRICES[model];
  const micro =
    usage.input_tokens * priceIn +
    usage.cache_creation.ephemeral_5m_input_tokens * priceIn * 1.25 +
    usage.cache_creation.ephemeral_1h_input_tokens * priceIn * 2.0 +
    usage.cache_read_input_tokens * priceIn * read +
    usage.output_tokens * priceOut;
  return (micro / 1_000_000) * (batch ? 0.5 : 1.0);
}

export function clientFor(...bodies: unknown[]) {
  const fake = scriptedFetch(bodies.map((body) => ({ body })));
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

async function main() {
  const { fake, client } = clientFor({ input_tokens: 4821 }, message([text("Refunds take 14 days. Opened items are excluded.")], "end_turn", USAGE as any));
  const counted = await client.messages.countTokens({ model: MODEL, system: PARAMS.system, messages: PARAMS.messages });
  console.log(`count_tokens -> ${counted.input_tokens} input tokens (POST ${new URL(fake.seen[0].url).pathname})`);
  console.log(`estimate before sending, input only: $${((counted.input_tokens * PRICES[MODEL][0]) / 1_000_000).toFixed(6)} on ${MODEL}`);
  const reply = await client.messages.create(PARAMS);
  const u: any = reply.usage;
  console.log(`usage: input ${u.input_tokens}, cache write ${u.cache_creation_input_tokens}, cache read ${u.cache_read_input_tokens}, output ${u.output_tokens}`);
  console.log(`cost of this request: $${cost(MODEL, u).toFixed(6)}  (batch: $${cost(MODEL, u, true).toFixed(6)})`);
  console.log("the same usage on each model:");
  for (const model of Object.keys(PRICES)) console.log(`  ${model.padEnd(28)} $${cost(model, u).toFixed(6)}`);
}

if (import.meta.main) await main();
