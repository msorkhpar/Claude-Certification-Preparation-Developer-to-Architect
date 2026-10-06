import { logger } from "./logger.ts";
const log = logger("context_builder");
/** What a long conversation keeps: trimmed tool output, case facts that newer information replaces, a context that never mixes customers, and a window that keeps tool calls whole. See ../../statement.md. */

export function estimateTokens(text: string): number {
  return Math.ceil(text.length / 4);
}

export function trimRecord(record: Record<string, string>, keep: string[]): Record<string, string> {
  const out: Record<string, string> = {};
  for (const k of keep) if (k in record) out[k] = record[k];
  return out;
}

export function updateFacts(facts: Record<string, any>, name: string, value: string, asOf: string): Record<string, any> {
  const next: Record<string, any> = {};
  for (const [n, f] of Object.entries(facts)) next[n] = { ...f, superseded: [...f.superseded] };
  const current = next[name];
  if (current === undefined) {
    next[name] = { value, as_of: asOf, superseded: [] };
  } else if (asOf >= current.as_of) {
    current.superseded.push(`${current.value}@${current.as_of}`);
    current.value = value;
    current.as_of = asOf;
  } else {
    current.superseded.push(`${value}@${asOf}`);
  }
  return next;
}

export function buildContext(customer: string, facts: any[], summary: string, recent: any[]): string {
  log.debug("buildContext input", customer);
  const parts: string[] = [];
  const mine = facts.filter((f) => f.customer === customer);
  if (mine.length > 0) parts.push("## Case facts\n" + mine.map((f) => `${f.name}: ${f.value} (as of ${f.as_of})`).join("\n"));
  parts.push("## Summary so far\n" + summary);
  parts.push("## Recent messages\n" + recent.map((m) => `${m.role}: ${m.text}`).join("\n"));
  return parts.join("\n\n");
}

export function missingFromSummary(summary: string, facts: any[]): string[] {
  return facts.filter((f) => !summary.includes(f.value)).map((f) => f.name);
}

export function window(messages: any[], budget: number): any[] {
  const units: any[][] = [];
  for (let i = 0; i < messages.length; ) {
    const m = messages[i];
    if (m.kind === "tool_use" && i + 1 < messages.length && messages[i + 1].kind === "tool_result" && messages[i + 1].id === m.id) {
      units.push([m, messages[i + 1]]);
      i += 2;
    } else {
      units.push([m]);
      i += 1;
    }
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
