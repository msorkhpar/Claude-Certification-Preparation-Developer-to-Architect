// The coordinator's last step in a research run: merge what the subagents returned into claims with their sources, conflicts, errors and a coverage note.
export type Finding = { claim: string; value: string; source: string; date: string };
export type Failure = { type: string; query: string; partial: Finding[]; alternatives: string[] };
export type Result = { scope: string; status: "ok" | "error"; findings: Finding[]; error: Failure | null };

const same = <T>(items: T[], item: T): T[] => (items.some((x) => JSON.stringify(x) === JSON.stringify(item)) ? items : [...items, item]);

export function synthesize(required: string[], results: Result[]) {
  const okScopes = new Set(results.filter((r) => r.status === "ok" && r.findings.length > 0).map((r) => r.scope));
  const covered = required.filter((s) => okScopes.has(s));
  const gaps = required.filter((s) => !okScopes.has(s));
  const seen: Array<[Finding, boolean]> = [];
  for (const r of results) {
    if (r.status === "ok") r.findings.forEach((f) => seen.push([f, false]));
    else r.error!.partial.forEach((f) => seen.push([f, true]));
  }
  const byClaim = new Map<string, Array<[Finding, boolean]>>();
  for (const [f, partial] of seen) byClaim.set(f.claim, [...(byClaim.get(f.claim) ?? []), [f, partial]]);
  const claims: unknown[] = [];
  const conflicts: unknown[] = [];
  for (const claim of [...byClaim.keys()].sort()) {
    const group = byClaim.get(claim)!;
    let values: string[] = [];
    for (const [f] of group) values = same(values, f.value);
    if (values.length > 1) {
      let observed: unknown[] = [];
      for (const [f] of group) observed = same(observed, { value: f.value, source: f.source, date: f.date });
      conflicts.push({ claim, values: observed });
    } else {
      let sources: unknown[] = [];
      for (const [f] of group) sources = same(sources, { source: f.source, date: f.date });
      claims.push({ claim, value: values[0], sources, partial: group.every(([, p]) => p) });
    }
  }
  const errors = results.filter((r) => r.status === "error" && !covered.includes(r.scope))
    .map((r) => ({ scope: r.scope, type: r.error!.type, query: r.error!.query, alternatives: r.error!.alternatives }));
  const partial = gaps.filter((s) => results.some((r) => r.status === "error" && r.scope === s && r.error!.partial.length > 0));
  const parts = gaps.map((g) => {
    const failed = errors.find((e) => e.scope === g);
    if (failed) return `${g} (${failed.type} on '${failed.query}')`;
    if (!results.some((r) => r.scope === g)) return `${g} (not researched)`;
    return `${g} (no findings)`;
  });
  return { status: gaps.length > 0 ? "partial" : "complete", covered, gaps, partial, claims, conflicts, errors, note: parts.length > 0 ? "not covered: " + parts.join(", ") : "all scopes covered" };
}
