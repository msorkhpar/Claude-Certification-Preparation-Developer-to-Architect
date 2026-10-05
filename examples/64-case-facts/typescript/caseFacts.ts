import { logger } from "./logger.ts";
const log = logger("case_facts");
/**
 * What a long support conversation should keep, and where it should sit.
 *
 * The exam guide (task 5.1) names four risks: a progressive summary turns numbers, dates and the customer's stated expectations into vague prose; models attend well to the start and the end of a long input and may miss the middle;
 * tool results pile up in the context out of proportion to their use (40 fields in an order lookup, five of them wanted); and the whole history must be sent on each request. The Claude documentation on long-context prompts
 * (read 2026-10-04) says to put long documents at the top and the question at the end, which can improve quality in tests by up to 30 percent, and to structure documents with tags. The functions below show the bookkeeping;
 * nothing here calls a model, and the numbers come from the sample data, not from a measurement.
 */
const TOOL_FIELDS: Record<string, string[]> = {
  lookup_order: ["order_id", "purchase_date", "items", "return_window", "refund_amount"],
  lookup_customer: ["customer_id", "tier"],
};

export function tokens(text: string): number {
  return Math.ceil(text.length / 4);
}

export function render(record: Record<string, string>): string {
  return Object.entries(record).map(([k, v]) => `${k}=${v}`).join(";");
}

/** Keep what the next decision needs from a tool result, with its exact values. */
export function shrink(tool: string, result: Record<string, string>): Record<string, string> {
  const out: Record<string, string> = {};
  for (const k of TOOL_FIELDS[tool]) if (k in result) out[k] = result[k];
  return out;
}

/** Facts the conversation must not lose, as a block that goes into every request outside the summarised history. */
export function caseFactsBlock(facts: Array<[string, string, number]>): string {
  return "## Case facts\n" + facts.map(([name, value, day]) => `${name}: ${value} (as of day ${day})`).join("\n");
}

/** Key facts first, a short findings summary, the long documents under headers, the question last. */
export function assemble(block: string, findings: string[], documents: Array<[string, string]>, question: string): string {
  const parts = [block, "## Key findings\n" + findings.map((f) => `- ${f}`).join("\n"), "## Documents\n" + documents.map(([title, text]) => `### ${title}\n${text}`).join("\n"), "## Question\n" + question];
  return parts.join("\n\n");
}

/** A value read days ago is re-read before it is acted on. */
export function stale(seenDay: number, today: number, maxAgeDays: number): boolean {
  return today - seenDay > maxAgeDays;
}

function main() {
  const order: Record<string, string> = { order_id: "A-1042", purchase_date: "2026-09-02", items: "2 x kettle", return_window: "30 days", refund_amount: "$129.50" };
  for (let n = 1; n < 36; n++) order[`internal_${String(n).padStart(2, "0")}`] = `backend-value-${String(n).padStart(2, "0")}`;
  const small = shrink("lookup_order", order);
  console.log(`lookup_order result: ${Object.keys(order).length} fields, ${render(order).length} characters, about ${tokens(render(order))} tokens`);
  console.log(`after shrinking: ${Object.keys(small).length} fields, ${render(small).length} characters, about ${tokens(render(small))} tokens`);
  console.log(`twenty lookups kept whole: ${20 * tokens(render(order))} tokens; shrunk: ${20 * tokens(render(small))} tokens`);
  const block = caseFactsBlock([["refund_amount", "$129.50", 118], ["return_deadline", "2026-09-30", 118]]);
  console.log(block);
  const prompt = assemble(block, ["The order qualifies for a refund", "The deadline is the binding fact"], [["Policy", "..."], ["Order history", "..."]], "What should the customer be told?");
  console.log("section order: " + prompt.split("\n").filter((line) => line.startsWith("#")).join(" | "));
  for (const seen of [118, 124]) console.log(`refund_amount seen on day ${seen}, today day 125, limit 3 days: ` + (stale(seen, 125, 3) ? "read it again before acting" : "still fresh"));
}

if (import.meta.main) main();
