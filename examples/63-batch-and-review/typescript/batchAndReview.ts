/**
 * What a batch asks of its caller, and what an independent review is given.
 *
 * Read on 2026-10-04 in the Claude API documentation ("Batch processing"): a batch is processed asynchronously, results are available when every request has finished or after 24 hours, whichever comes first,
 * a request is identified by its `custom_id` (1 to 64 letters, digits, hyphens and underscores), results can come back in any order, and `stream`, `speed` and a `max_tokens` of 0 are refused. The exam guide's wording for
 * task 4.5 (a batch has no latency guarantee and cannot run a tool mid-request) and 4.6 (an independent instance reviews better than the generator) is what the functions below make visible. Nothing here calls a model.
 */
const CUSTOM_ID = /^[a-zA-Z0-9_-]{1,64}$/;

/** An item that arrives just after a submission waits one interval for the next batch, then the processing window, then the handling. */
export function worstCaseWait(intervalHours: number, windowHours = 24, handlingHours = 2): number {
  return intervalHours + windowHours + handlingHours;
}

/** One entry of a batch request, refused for the same reasons the API refuses it. */
export function batchEntry(customId: string, params: Record<string, unknown>): { custom_id: string; params: Record<string, unknown> } {
  if (!CUSTOM_ID.test(customId)) throw new Error(`custom_id '${customId}' must be 1 to 64 letters, digits, hyphens or underscores`);
  if (params.stream === true) throw new Error("stream is not supported in a batch");
  if ("speed" in params) throw new Error("speed is not supported in a batch");
  if (params.max_tokens === 0) throw new Error("a max_tokens of 0 is not supported in a batch");
  return { custom_id: customId, params };
}

/** Results come back in any order: pair them with the requests by custom_id, and report a result nobody asked for. */
export function matchResults(requests: string[], results: Array<[string, string]>): [Array<[string, string]>, string[]] {
  const byId = new Map(results);
  const asked = new Set(requests);
  return [requests.map((r): [string, string] => [r, byId.get(r) ?? "missing"]), results.map(([i]) => i).filter((i) => !asked.has(i))];
}

/** What the reviewing instance receives: an independent one gets the code alone, a self-review also gets the reasoning that produced it. */
export function reviewRequest(code: string, reasoning: string, independent: boolean): string {
  const parts = ["Review this code for defects.", `<code>${code}</code>`];
  if (!independent) parts.push(`<your_earlier_reasoning>${reasoning}</your_earlier_reasoning>`);
  return parts.join("\n");
}

function main() {
  for (const interval of [4, 6]) console.log(`worst-case wait with a ${interval} hour interval: ${worstCaseWait(interval)} hours (SLA 30 hours)`);
  for (const customId of ["invoice-0042", "invoice 0042"]) {
    try {
      batchEntry(customId, { max_tokens: 1024 });
      console.log(`custom_id ${customId}: accepted`);
    } catch (e) {
      console.log(`custom_id ${customId}: refused, ${(e as Error).message}`);
    }
  }
  for (const params of [{ stream: true }, { speed: "fast" }, { max_tokens: 0 }]) {
    try {
      batchEntry("a1", params);
    } catch (e) {
      console.log(`refused: ${(e as Error).message}`);
    }
  }
  const [matched, orphans] = matchResults(["a1", "a2", "a3"], [["a2", "expired"], ["z9", "succeeded"], ["a1", "succeeded"]]);
  console.log("matched: " + matched.map(([i, kind]) => `${i}=${kind}`).join(", ") + "; unrequested: " + orphans.join(", "));
  const code = "total = price * qty";
  const reasoning = "qty is always positive, so no check";
  for (const independent of [false, true]) {
    const request = reviewRequest(code, reasoning, independent);
    console.log(`independent=${independent ? "yes" : "no"}: carries the reasoning: ${request.includes(reasoning) ? "yes" : "no"}`);
  }
}

if (import.meta.main) main();
