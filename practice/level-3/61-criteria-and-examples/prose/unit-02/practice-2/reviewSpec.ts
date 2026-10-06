import { logger } from "./logger.ts";
const log = logger("review_spec");
/** A review specification that cuts false positives: the prompt, the trust in each category and the next step when a request is incomplete. See ../../statement.md. */

export const VAGUE = ["be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment"];

const present = (value: unknown): boolean => typeof value === "string" && value.trim() !== "";
const blank = (value: unknown): boolean => value === undefined || value === null || (typeof value === "string" && value.trim() === "");
// TODO 2 of 7 (finish this to pass e1): the vague check. Receives a criterion's report or skip text. Return the first
//   phrase of VAGUE that the lower-cased text contains, or nothing when it contains none. Example: "Be conservative here"
//   -> "be conservative".
const vague = (_text: string): string | undefined => undefined;

export function buildReviewPrompt(spec: any, diff: string): string {
  log.debug("buildReviewPrompt input", spec);
  const criteria: any[] = spec.criteria ?? [];
  const examples: any[] = spec.examples ?? [];
  if (criteria.length === 0) throw new Error("at least one criterion is required");
  const ids: string[] = [];
  const blocks: string[] = [];
  for (const c of criteria) {
    for (const key of ["report", "skip"]) {
      if (!present(c[key])) throw new Error(`criterion ${c.id}: ${key} is required`);
      const phrase = vague(c[key]);
      if (phrase) throw new Error(`criterion ${c.id}: ${key} is vague ('${phrase}'): name the pattern instead`);
    }
    const severity = c.severity ?? {};
    // TODO 3 of 7 (finish this to pass e2): the severity check. For each of the levels high and low, refuse the
    //   criterion when its severity has no concrete example text for that level. Example: severity {high: "..."} with no
    //   low -> refused.
    ids.push(c.id);
    blocks.push(`<criterion id="${c.id}">\nReport: ${c.report}\nSkip: ${c.skip}\nSeverity high: ${severity.high}\nSeverity low: ${severity.low}\n</criterion>`);
  }
  // TODO 4 of 7 (finish this to pass e3): the examples check. Refuse the specification unless it has two to four
  //   examples and their verdicts are exactly report and skip (at least one of each, nothing else). Example: three report
  //   examples and no skip -> refused.
  const shown: string[] = [];
  for (const e of examples) {
    if (!present(e.reason)) throw new Error("every example needs a reason");
    if (e.verdict === "report" && !ids.includes(e.category)) throw new Error("a report example names one of the criteria");
    const tag = e.verdict === "report" ? `verdict="report" category="${e.category}"` : 'verdict="skip"';
    shown.push(`<example ${tag}>\n<code>${e.code}</code>\n<reason>${e.reason}</reason>\n</example>`);
  }
  // TODO 1 of 7 (finish this to pass m1): the end of the prompt. After the criteria and examples blocks, add the diff in
  //   a <diff> block and return the whole prompt joined with newlines, so that the diff comes last. Example: criteria,
  //   examples, then <diff>, the diff, </diff>.
  return diff;
}

export function categoryReport(findings: any[], minReviewed = 5, minPrecision = 0.5): any {
  const byCategory = new Map<string, any[]>();
  for (const f of findings) byCategory.set(f.category, [...(byCategory.get(f.category) ?? []), f]);
  const categories: Record<string, any> = {};
  for (const [name, items] of byCategory) {
    const accepted = items.filter((f) => f.verdict === "accepted").length;
    const counts = new Map<string, number>();
    for (const f of items) if (f.verdict === "dismissed") counts.set(f.detected_pattern, (counts.get(f.detected_pattern) ?? 0) + 1);
    // TODO 6 of 7 (finish this to pass e5): the top dismissed patterns. Receives the count of dismissals per detected
    //   pattern. Return up to three [pattern, count] pairs, ordered by count (highest first) and then by pattern name.
    //   Example: {a: 2, b: 3, c: 2, d: 1} -> [b 3], [a 2], [c 2].
    const top = [...counts.entries()];
    const precision = Math.round((accepted / items.length) * 100) / 100;
    // TODO 5 of 7 (finish this to pass e4): the disable flag of a category. Receives the number reviewed, the precision,
    //   min_reviewed and min_precision. A category is disabled when it has at least min_reviewed reviews and a precision
    //   below min_precision. Example: 5 reviews, precision 0.4, defaults -> disabled; 4 reviews -> not.
    categories[name] = { reviewed: items.length, precision, disable: false, top_dismissed: top.map(([pattern, count]) => [pattern, count]) };
  }
  return { categories, disable: Object.keys(categories).filter((n) => categories[n].disable).sort() };
}

export function nextStep(request: any, required: string[], defaults: Record<string, string>, attended: boolean): any {
  const missing = required.filter((f) => blank(request[f]));
  const assumptions: Record<string, string> = {};
  for (const f of missing) if (f in defaults) assumptions[f] = defaults[f];
  const unresolved = missing.filter((f) => !(f in defaults));
  // TODO 7 of 7 (finish this to pass e6, e7): the next step. `unresolved` holds the missing fields that have no default.
  //   When some are unresolved and the run is attended, return action ask with those fields in `ask`; when unattended,
  //   return action stop with an empty `ask`. Both carry the assumptions. Example: attended, unresolved [repo] -> ask
  //   [repo].
  return { action: "proceed", ask: [], assumptions };
}
