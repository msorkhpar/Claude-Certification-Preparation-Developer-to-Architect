// The coordinator's last step in a research run: merge what the subagents returned into claims with their sources, conflicts, errors and a coverage note.
import { logger } from "../logger.ts";
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

/**
 * TODO 1 of 7 (unlocks e1, e5 and the coverage of every other case): split the required scopes into covered and gaps.
 * Receives the required scopes and the results. Returns [covered, gaps], both in the order of `required`: a scope is covered
 * when an "ok" result for it has at least one finding.
 * Example: required ["a", "b"], one ok result for "a" with a finding -> [["a"], ["b"]]
 */
function coveredScopes(required: string[], results: Result[]): [string[], string[]] {
  return [[], []];
}

/**
 * TODO 2 of 7 (unlocks e6): the sources of one claim.
 * Receives the [finding, partial] pairs of one claim. Returns a list of { source, date } in arrival order, no duplicates.
 * Example: findings from s9, s1, s9 -> [{ source: "s9", ... }, { source: "s1", ... }]
 */
function sourcesOf(group: Seen): unknown[] {
  return [];
}

/**
 * TODO 3 of 7 (unlocks e3): what the sources said about a conflicting claim.
 * Receives the [finding, partial] pairs of one claim. Returns a list of { value, source, date } in arrival order, no duplicates.
 * Example: s1 says "12", s2 says "14" twice -> [{ value: "12", source: "s1", ... }, { value: "14", source: "s2", ... }]
 */
function observedValues(group: Seen): unknown[] {
  return [];
}

/**
 * TODO 4 of 7 (unlocks e4): is every finding of the claim from a partial list?
 * Receives the [finding, partial] pairs of one claim. Returns true only when all of them are partial.
 * Example: [[f, true], [f, false]] -> false
 */
function allPartial(group: Seen): boolean {
  return false;
}

/**
 * TODO 5 of 7 (unlocks e2 and e4): the errors that nothing made up for.
 * Receives the results and the covered scopes. Returns { scope, type, query, alternatives } for each "error" result whose scope is not covered.
 * Example: an error for "b" while only "a" is covered -> [{ scope: "b", type: "timeout", query: "qb", alternatives: [...] }]
 */
function unresolvedErrors(results: Result[], covered: string[]): Array<{ scope: string; type: string; query: string; alternatives: string[] }> {
  return [];
}

/**
 * TODO 6 of 7 (unlocks e4): the gaps for which a failed search still returned findings.
 * Receives the results and the gaps. Returns the gaps that have an "error" result with a non-empty partial list.
 * Example: gap "b" with an error that carries one partial finding -> ["b"]
 */
function partialScopes(results: Result[], gaps: string[]): string[] {
  return [];
}

/**
 * TODO 7 of 7 (unlocks e1, e2 and e5): say what the report cannot cover.
 * Receives the gaps, the unresolved errors and the results. Returns "all scopes covered" for no gaps; otherwise "not covered: " and
 * each gap as "<scope> (<type> on '<query>')" for an unresolved error, "<scope> (not researched)" when no result names the scope,
 * or "<scope> (no findings)", joined by ", ".
 * Example: gaps ["b", "c"], an error for "b" (timeout on "qb"), no result for "c" -> "not covered: b (timeout on 'qb'), c (not researched)"
 */
function coverageNote(gaps: string[], errors: Array<{ scope: string; type: string; query: string }>, results: Result[]): string {
  return "";
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
