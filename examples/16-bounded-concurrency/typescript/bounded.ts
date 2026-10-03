// Twelve classification calls with the SDK: unbounded, then bounded by a small worker pool.
// The fetch is scripted: every request takes 50 ms inside it, the reply is a label built from the
// ticket in the request, and ticket 7 is answered with a 429. The labels are illustrative.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text, type Reply } from "../../../harness/ts/scriptedFetch.ts";

const MODEL = "claude-sonnet-5-5";
export const TICKETS = Array.from({ length: 12 }, (_, i) => `ticket ${i + 1}`);

function responder(body: any): Reply {
  const ticket = body.messages[0].content as string;
  if (ticket === "ticket 7") return { status: 429, body: { type: "error", error: { type: "rate_limit_error", message: "slow down" } } };
  return { body: message([text("label for " + ticket)]) };
}

async function classify(client: Anthropic, ticket: string): Promise<string> {
  const reply = await client.messages.create({ model: MODEL, max_tokens: 16, messages: [{ role: "user", content: ticket }] });
  return (reply.content[0] as any).text;
}

// A pool of `limit` workers that pull the next ticket when they are free.
export async function runAll(limit: number | null) {
  const fake = scriptedFetch(TICKETS.map(() => responder), { delayMs: 50 });
  const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch });
  const results: PromiseSettledResult<string>[] = new Array(TICKETS.length);
  let next = 0;
  const worker = async () => {
    while (next < TICKETS.length) {
      const i = next++;
      results[i] = await classify(client, TICKETS[i]).then(
        (value) => ({ status: "fulfilled" as const, value }),
        (reason) => ({ status: "rejected" as const, reason }),
      );
    }
  };
  await Promise.all(Array.from({ length: limit ?? TICKETS.length }, worker));
  return { results, peak: fake.state.maxInFlight };
}

async function main() {
  let last: PromiseSettledResult<string>[] = [];
  for (const [label, limit] of [["unbounded", null], ["bounded by 4", 4]] as const) {
    const { results, peak } = await runAll(limit);
    last = results;
    const failed = results.flatMap((r, i) => (r.status === "rejected" ? [i + 1] : []));
    console.log(`${label}: peak in flight ${peak}, ${results.length - failed.length} answered, failed tickets [${failed.join(", ")}]`);
  }
  const v = (r: PromiseSettledResult<string>) => (r.status === "fulfilled" ? r.value : (r.reason as Error).constructor.name);
  console.log("results keep input order:", v(last[0]), "|", v(last[5]), "|", v(last[6]), "|", v(last[7]));
}

if (import.meta.main) await main();
