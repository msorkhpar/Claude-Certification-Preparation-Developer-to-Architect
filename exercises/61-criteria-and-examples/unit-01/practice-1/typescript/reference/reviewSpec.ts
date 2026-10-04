/** A review specification that cuts false positives: the prompt, the trust in each category and the next step when a request is incomplete. See ../../statement.md. */

export const VAGUE = ["be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment"];

const present = (value: unknown): boolean => typeof value === "string" && value.trim() !== "";
const blank = (value: unknown): boolean => value === undefined || value === null || (typeof value === "string" && value.trim() === "");
const vague = (text: string): string | undefined => VAGUE.find((phrase) => text.toLowerCase().includes(phrase));

export function buildReviewPrompt(spec: any, diff: string): string {
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
    for (const level of ["high", "low"]) {
      if (!present(severity[level])) throw new Error(`criterion ${c.id}: severity ${level} needs a concrete example`);
    }
    ids.push(c.id);
    blocks.push(`<criterion id="${c.id}">\nReport: ${c.report}\nSkip: ${c.skip}\nSeverity high: ${severity.high}\nSeverity low: ${severity.low}\n</criterion>`);
  }
  if (examples.length < 2 || examples.length > 4) throw new Error("use two to four examples");
  const verdicts = [...new Set(examples.map((e) => e.verdict))].sort();
  if (verdicts.join() !== "report,skip") throw new Error("the examples need at least one report and one skip, and no other verdict");
  const shown: string[] = [];
  for (const e of examples) {
    if (!present(e.reason)) throw new Error("every example needs a reason");
    if (e.verdict === "report" && !ids.includes(e.category)) throw new Error("a report example names one of the criteria");
    const tag = e.verdict === "report" ? `verdict="report" category="${e.category}"` : 'verdict="skip"';
    shown.push(`<example ${tag}>\n<code>${e.code}</code>\n<reason>${e.reason}</reason>\n</example>`);
  }
  return ["<criteria>", ...blocks, "</criteria>", "<examples>", ...shown, "</examples>", "<diff>", diff, "</diff>"].join("\n");
}

export function categoryReport(findings: any[], minReviewed = 5, minPrecision = 0.5): any {
  const byCategory = new Map<string, any[]>();
  for (const f of findings) byCategory.set(f.category, [...(byCategory.get(f.category) ?? []), f]);
  const categories: Record<string, any> = {};
  for (const [name, items] of byCategory) {
    const accepted = items.filter((f) => f.verdict === "accepted").length;
    const counts = new Map<string, number>();
    for (const f of items) if (f.verdict === "dismissed") counts.set(f.detected_pattern, (counts.get(f.detected_pattern) ?? 0) + 1);
    const top = [...counts.entries()].sort((a, b) => b[1] - a[1] || (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0)).slice(0, 3);
    const precision = Math.round((accepted / items.length) * 100) / 100;
    categories[name] = { reviewed: items.length, precision, disable: items.length >= minReviewed && precision < minPrecision, top_dismissed: top.map(([pattern, count]) => [pattern, count]) };
  }
  return { categories, disable: Object.keys(categories).filter((n) => categories[n].disable).sort() };
}

export function nextStep(request: any, required: string[], defaults: Record<string, string>, attended: boolean): any {
  const missing = required.filter((f) => blank(request[f]));
  const assumptions: Record<string, string> = {};
  for (const f of missing) if (f in defaults) assumptions[f] = defaults[f];
  const unresolved = missing.filter((f) => !(f in defaults));
  if (unresolved.length > 0 && attended) return { action: "ask", ask: unresolved, assumptions };
  if (unresolved.length > 0) return { action: "stop", ask: [], assumptions };
  return { action: "proceed", ask: [], assumptions };
}
