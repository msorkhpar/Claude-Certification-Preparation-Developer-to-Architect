// The coordinator's last step in a research run: merge what the subagents returned into claims with their sources, conflicts, errors and a coverage note.
import { logger } from "./logger.ts";
const log = logger("synthesis");

export type Finding = { claim: string; value: string; source: string; date: string };
export type Failure = { type: string; query: string; partial: Finding[]; alternatives: string[] };
export type Result = { scope: string; status: "ok" | "error"; findings: Finding[]; error: Failure | null };
type Seen = Array<[Finding, boolean]>;

const same = <T>(items: T[], item: T): T[] => (items.some((x) => JSON.stringify(x) === JSON.stringify(item)) ? items : [...items, item]);

/** Every finding as [finding, cameFromAPartialList], in arrival order. */
function findings(results: Result[]): Seen {
  const seen: Seen = [];
  for (const r of results) {
    if (r.status === "ok") r.findings.forEach((f) => seen.push([f, false]));
    else r.error!.partial.forEach((f) => seen.push([f, true]));
  }
  return seen;
}

function coveredScopes(required: string[], results: Result[]): [string[], string[]] {
  const okScopes = new Set(results.filter((r) => r.status === "ok" && r.findings.length > 0).map((r) => r.scope));
  return [required.filter((s) => okScopes.has(s)), required.filter((s) => !okScopes.has(s))];
}

function sourcesOf(group: Seen): unknown[] {
  let sources: unknown[] = [];
  for (const [f] of group) sources = same(sources, { source: f.source, date: f.date });
  return sources;
}

function observedValues(group: Seen): unknown[] {
  let observed: unknown[] = [];
  for (const [f] of group) observed = same(observed, { value: f.value, source: f.source, date: f.date });
  return observed;
}

function allPartial(group: Seen): boolean {
  return group.every(([, p]) => p);
}

function unresolvedErrors(results: Result[], covered: string[]): Array<{ scope: string; type: string; query: string; alternatives: string[] }> {
  return results.filter((r) => r.status === "error" && !covered.includes(r.scope))
    .map((r) => ({ scope: r.scope, type: r.error!.type, query: r.error!.query, alternatives: r.error!.alternatives }));
}

function partialScopes(results: Result[], gaps: string[]): string[] {
  return gaps.filter((s) => results.some((r) => r.status === "error" && r.scope === s && r.error!.partial.length > 0));
}

function coverageNote(gaps: string[], errors: Array<{ scope: string; type: string; query: string }>, results: Result[]): string {
  const parts = gaps.map((g) => {
    const failed = errors.find((e) => e.scope === g);
    if (failed) return `${g} (${failed.type} on '${failed.query}')`;
    if (!results.some((r) => r.scope === g)) return `${g} (not researched)`;
    return `${g} (no findings)`;
  });
  return parts.length > 0 ? "not covered: " + parts.join(", ") : "all scopes covered";
}

export function synthesize(required: string[], results: Result[]) {
  log.debug("synthesize input", results);
  const [covered, gaps] = coveredScopes(required, results);
  const byClaim = new Map<string, Seen>();
  for (const [f, partial] of findings(results)) byClaim.set(f.claim, [...(byClaim.get(f.claim) ?? []), [f, partial]]);
  const claims: unknown[] = [];
  const conflicts: unknown[] = [];
  for (const claim of [...byClaim.keys()].sort()) {
    const group = byClaim.get(claim)!;
    let values: string[] = [];
    for (const [f] of group) values = same(values, f.value);
    if (values.length > 1) conflicts.push({ claim, values: observedValues(group) });
    else claims.push({ claim, value: values[0], sources: sourcesOf(group), partial: allPartial(group) });
  }
  const errors = unresolvedErrors(results, covered);
  return { status: gaps.length > 0 ? "partial" : "complete", covered, gaps, partial: partialScopes(results, gaps), claims, conflicts, errors, note: coverageNote(gaps, errors, results) };
}
