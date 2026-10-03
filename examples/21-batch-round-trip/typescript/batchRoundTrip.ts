// A Message Batch from submission to results, against a scripted server.
// The replies are illustrative, hand-written bodies in the shapes of the batch processing page (claude-haiku-4-5), not
// captures. The results arrive out of order, as the page warns they may, and one request of each non-success kind is in
// them. Waiting between polls is recorded, not slept.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

export const MODEL = "claude-haiku-4-5-20251001";
export const TICKETS: Record<string, string> = { "t-1": "My parcel never arrived.", "t-2": "How do I change my address?", "t-3": "Charge me twice? Refund please.", "t-4": "x".repeat(10) };

const batch = (status: string, counts: object, resultsUrl: string | null = null) => ({
  id: "msgbatch_illustrative", type: "message_batch", processing_status: status, request_counts: counts,
  ended_at: status === "ended" ? "2026-10-02T10:40:00Z" : null, created_at: "2026-10-02T10:00:00Z",
  expires_at: "2026-10-03T10:00:00Z", cancel_initiated_at: null, results_url: resultsUrl,
});
const counts = (c: Partial<Record<"processing" | "succeeded" | "errored" | "canceled" | "expired", number>>) => ({ processing: 0, succeeded: 0, errored: 0, canceled: 0, expired: 0, ...c });
const line = (customId: string, result: unknown) => JSON.stringify({ custom_id: customId, result });
const succeeded = (label: string) => ({ type: "succeeded", message: message([text(label)], "end_turn", { input_tokens: 30, output_tokens: 3 }, MODEL) });

export const RESULTS =
  [
    line("t-3", succeeded("billing")),
    line("t-1", succeeded("shipping")),
    line("t-4", { type: "errored", error: { type: "error", error: { type: "invalid_request_error", message: "messages: at least one message is required" } } }),
    line("t-2", { type: "expired" }),
  ].join("\n") + "\n";

export const script = () => [
  { body: batch("in_progress", counts({ processing: 4 })) },
  { body: batch("in_progress", counts({ processing: 2, succeeded: 2 })) },
  { body: batch("ended", counts({ succeeded: 2, errored: 1, expired: 1 }), "https://api.anthropic.com/v1/messages/batches/msgbatch_illustrative/results") },
  { body: batch("ended", counts({ succeeded: 2, errored: 1, expired: 1 }), "https://api.anthropic.com/v1/messages/batches/msgbatch_illustrative/results") }, // results() looks the batch up first
  { text: RESULTS },
];

export function clientFor(replies: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(replies as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

async function main() {
  const { fake, client } = clientFor(script());
  const requests = Object.entries(TICKETS).map(([custom_id, body]) => ({ custom_id, params: { model: MODEL, max_tokens: 50, messages: [{ role: "user" as const, content: `Label this ticket: ${body}` }] } }));
  const created = await client.messages.batches.create({ requests });
  console.log("created:", created.id, created.processing_status, "| request ids sent:", JSON.stringify(fake.seen[0].body.requests.map((r: any) => r.custom_id)));
  const waits: number[] = [];
  let status = created;
  while (status.processing_status !== "ended") {
    status = await client.messages.batches.retrieve(created.id);
    waits.push(60);
    console.log("poll:", status.processing_status, JSON.stringify(status.request_counts));
  }
  console.log("waited between polls (recorded, not slept):", JSON.stringify(waits), "seconds");
  const outcomes: Record<string, [string, string]> = {};
  for await (const item of await client.messages.batches.results(created.id)) {
    const r: any = item.result;
    const kind = r.type as string;
    const detail = kind === "succeeded" ? r.message.content[0].text : kind === "errored" ? r.error.error.type : "";
    outcomes[item.custom_id] = [kind, detail];
    console.log(`result: ${item.custom_id} ${kind} ${detail}`.trimEnd());
  }
  console.log("in request order:", JSON.stringify(Object.keys(TICKETS).map((cid) => [cid, outcomes[cid][0]])));
  const ids = Object.keys(outcomes);
  const fix = ids.filter((cid) => outcomes[cid][0] === "errored" && outcomes[cid][1] === "invalid_request_error");
  const retry = ids.filter((cid) => ["expired", "canceled"].includes(outcomes[cid][0]) || (outcomes[cid][0] === "errored" && !fix.includes(cid)));
  console.log("fix before resubmitting:", JSON.stringify(fix), "| resubmit unchanged:", JSON.stringify(retry));
}

if (import.meta.main) await main();
