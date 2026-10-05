import { logger } from "../logger.ts";
const log = logger("context_builder");
/** What a long conversation keeps: trimmed tool output, case facts that newer information replaces, a context that never mixes customers, and a window that keeps tool calls whole. See ../../statement.md. */

export function estimateTokens(text: string): number {
  return Math.ceil(text.length / 4);
}

export function trimRecord(record: Record<string, string>, keep: string[]): Record<string, string> {
  // TODO 1 of 6 (finish this to pass m1, e1): the trim. Receives a tool's record and the list of fields to keep. Return
  //   a new record with only those fields, in the order of the list, with their exact values, skipping a field that the
  //   record does not have. Example: record {id, status, notes}, keep [status, id] -> {status, id}.
  return { ...record };
}

export function updateFacts(facts: Record<string, any>, name: string, value: string, asOf: string): Record<string, any> {
  const next: Record<string, any> = {};
  for (const [n, f] of Object.entries(facts)) next[n] = { ...f, superseded: [...f.superseded] };
  const current = next[name];
  if (current === undefined) {
    next[name] = { value, as_of: asOf, superseded: [] };
  } else {
    // TODO 2 of 6 (finish this to pass e2, e3): the update of a known fact. When the new date is the same as or later than
    //   the stored one, replace the value and the date and append "oldvalue@olddate" to the history; when it is earlier,
    //   keep the current value and append "newvalue@newdate" to the history. Example: stored 5@2026-01-02, new 7@2026-01-05
    //   -> value 7, history [5@2026-01-02].
    current.value = value;
    current.as_of = asOf;
  }
  return next;
}

export function buildContext(customer: string, facts: any[], summary: string, recent: any[]): string {
  log.debug("buildContext input", customer);
  const parts: string[] = [];
  // TODO 3 of 6 (finish this to pass e4): the facts of one customer. Receives the customer and all the fact entries.
  //   Keep only the entries whose customer is that customer. Example: facts of C1 and C2, customer C1 -> the C1 entries
  //   only.
  const mine = [...facts];
  // TODO 4 of 6 (finish this to pass e5): the case facts section. When there are facts for the customer, add first a
  //   section titled "## Case facts" with one line per fact, "name: value (as of date)". Example: one fact -> "## Case
  //   facts\norder: A-7 (as of 2026-01-02)", before the summary.
  parts.push("## Summary so far\n" + summary);
  parts.push("## Recent messages\n" + recent.map((m) => `${m.role}: ${m.text}`).join("\n"));
  return parts.join("\n\n");
}

export function missingFromSummary(summary: string, facts: any[]): string[] {
  // TODO 5 of 6 (finish this to pass e6): the check of a summary. Receives the summary and the fact entries. Return the
  //   names of the facts whose exact value does not appear in the summary text. Example: fact order = A-7, summary
  //   "customer wants a refund" -> [order].
  return [];
}

export function window(messages: any[], budget: number): any[] {
  const units: any[][] = [];
  for (let i = 0; i < messages.length; ) {
    const m = messages[i];
    // TODO 6 of 6 (finish this to pass e7): the units of the window. Walk the messages in order: a tool_use message
    //   followed by the tool_result with the same id forms one unit of two, which is never split; any other message is a
    //   unit of one. Example: [user, tool_use t1, tool_result t1] -> two units.
    units.push([m]);
    i += 1;
  }
  const kept: any[][] = [];
  let used = 0;
  for (const unit of [...units].reverse()) {
    const cost = unit.reduce((sum, x) => sum + estimateTokens(x.text), 0);
    if (used + cost > budget) break;
    kept.unshift(unit);
    used += cost;
  }
  return kept.flat();
}
