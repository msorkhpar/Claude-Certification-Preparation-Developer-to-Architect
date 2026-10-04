/**
 * What a coordinator is told when one of five sources fails, under four ways of reporting it.
 *
 * The exam guide (task 5.3) calls structured error context (failure type, the query attempted, partial results, alternatives) what lets a coordinator recover intelligently. It names two anti-patterns: a generic status
 * such as "search unavailable", which hides the context, and silent suppression, which reports an empty result as a success; terminating the whole workflow on one failure is the third. The five sources below and
 * their outcomes are invented for the illustration; nothing here calls a model or a search tool.
 */
// source -> [kind, items]; kind is ok, timeout or permission
export type Outcomes = Record<string, [string, string[]]>;
export const OUTCOMES: Outcomes = { news: ["ok", ["n1", "n2"]], papers: ["timeout", ["p1"]], patents: ["ok", []], filings: ["permission", []], blogs: ["ok", ["b1"]] };
const TRY: Record<string, string> = { timeout: "retry later", permission: "request access" };

const found = (outcomes: Outcomes): string[] => Object.values(outcomes).filter(([kind]) => kind === "ok").flatMap(([, items]) => items);

export function generic(outcomes: Outcomes): string {
  const down = Object.entries(outcomes).filter(([, [kind]]) => kind !== "ok").map(([source]) => source);
  return `found ${found(outcomes).join(", ")}; sources unavailable: ${down.join(", ")}`;
}

export function suppress(outcomes: Outcomes): string {
  const nothing = Object.entries(outcomes).filter(([, [kind, items]]) => kind !== "ok" || items.length === 0).map(([source]) => source);
  return `found ${found(outcomes).join(", ")}; nothing found in: ${nothing.join(", ")}`;
}

export function terminate(outcomes: Outcomes): string {
  const kept: string[] = [];
  for (const [source, [kind, items]] of Object.entries(outcomes)) {
    if (kind !== "ok") return `aborted at ${source}; found ${kept.join(", ")}`;
    kept.push(...items);
  }
  return `found ${kept.join(", ")}`;
}

export function structured(outcomes: Outcomes): string {
  const entries = Object.entries(outcomes);
  const good = entries.filter(([, [kind, items]]) => kind === "ok" && items.length > 0).map(([s]) => s);
  const partial = entries.filter(([, [kind, items]]) => kind !== "ok" && items.length > 0).map(([s, [kind, items]]) => `${s} (${kind}, kept ${items.join(", ")})`);
  const empty = entries.filter(([, [kind, items]]) => kind === "ok" && items.length === 0).map(([s]) => s);
  const gaps = entries.filter(([, [kind, items]]) => kind !== "ok" && items.length === 0).map(([s, [kind]]) => `${s} (${kind}, try: ${TRY[kind]})`);
  const parts: Array<[string, string[]]> = [["well supported", good], ["partial", partial], ["no findings", empty], ["gaps", gaps]];
  return parts.filter(([, items]) => items.length > 0).map(([name, items]) => `${name}: ${items.join(", ")}`).join("; ");
}

function main() {
  for (const [name, report] of [["generic status", generic], ["silent empty", suppress], ["abort on failure", terminate], ["structured context", structured]] as const) console.log(`${name}: ${report(OUTCOMES)}`);
}

if (import.meta.main) main();
