import { logger } from "./logger.ts";
const log = logger("criteria_lint");
/**
 * Three checks that keep a review prompt precise: lint a criterion for vague wording, check a set of few-shot examples, and measure the precision of each finding category from the verdicts developers gave.
 *
 * The rules are the exam guide's for tasks 4.1 and 4.2 (explicit categorical criteria instead of "be conservative", two to four targeted examples that include an acceptable pattern, a category with a high false positive rate is switched
 * off while its prompt is improved) and the prompting guide's advice on examples (read on 2026-10-03: relevant, diverse and structured, three to five). No model is called.
 */
export const VAGUE = ["be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "use your judgment"];
export type Criterion = { report?: string; skip?: string; severity?: { high?: string; low?: string } };
export type Example = { verdict: string; reason?: string };

/** Rule ids a review criterion breaks: wording that names no pattern, a missing skip list, a severity level without a concrete example. */
export function lintCriterion(criterion: Criterion): string[] {
  const found: string[] = [];
  for (const key of ["report", "skip"] as const) {
    const text = criterion[key] ?? "";
    if (text.trim() === "") found.push(`no-${key}`);
    else if (VAGUE.some((phrase) => text.toLowerCase().includes(phrase))) found.push(`vague-${key}`);
  }
  for (const level of ["high", "low"] as const) if (!(criterion.severity?.[level] ?? "").includes("`")) found.push(`no-${level}-example`);
  return found;
}

/** Rule ids for a set of few-shot examples: how many, whether both a finding and an acceptable pattern are shown, whether each says why. */
export function lintExamples(examples: Example[]): string[] {
  const found: string[] = [];
  if (examples.length < 2 || examples.length > 4) found.push("two-to-four");
  if ([...new Set(examples.map((e) => e.verdict))].sort().join() !== "report,skip") found.push("both-verdicts");
  if (examples.some((e) => !e.reason)) found.push("reason-missing");
  return found;
}

export type Row = { reviewed: number; accepted: number; precision: number; off: boolean };

/** Per category: how many findings were reviewed, the share developers accepted, and whether to switch the category off while its prompt is improved. */
export function trust(verdicts: Array<[string, string]>, minReviewed = 5, minPrecision = 0.5): Record<string, Row> {
  const table: Record<string, Row> = {};
  for (const [category, verdict] of verdicts) {
    const row = (table[category] ??= { reviewed: 0, accepted: 0, precision: 0, off: false });
    row.reviewed += 1;
    row.accepted += verdict === "accepted" ? 1 : 0;
  }
  for (const row of Object.values(table)) {
    row.precision = Math.round((row.accepted / row.reviewed) * 100) / 100;
    row.off = row.reviewed >= minReviewed && row.precision < minPrecision;
  }
  return table;
}

export const VAGUE_CRITERION: Criterion = { report: "Be conservative and only flag important problems.", skip: "", severity: { high: "Something serious.", low: "A small thing." } };
export const GOOD_CRITERION: Criterion = { report: "A comment whose claimed behaviour contradicts the code.", skip: "Minor style and patterns the codebase already uses.",
  severity: { high: "A null dereference such as `user.profile.name` when `user` may be None.", low: "A misleading name such as `total` for a count." } };

function main() {
  console.log("vague criterion:", JSON.stringify(lintCriterion(VAGUE_CRITERION)));
  console.log("good criterion:", JSON.stringify(lintCriterion(GOOD_CRITERION)));
  const oneSide = [{ verdict: "report", reason: "The comment says sum, the code multiplies." }, { verdict: "report", reason: "" }, { verdict: "report", reason: "Unchecked None." }, { verdict: "report", reason: "Off by one." }, { verdict: "report", reason: "Wrong key." }];
  console.log("five reports, one without a reason:", JSON.stringify(lintExamples(oneSide)));
  console.log("a report and a skip:", JSON.stringify(lintExamples([{ verdict: "report", reason: "r" }, { verdict: "skip", reason: "s" }])));
  const verdicts: Array<[string, string]> = [...Array(9).fill(["bug", "accepted"]), ["bug", "dismissed"], ...Array(2).fill(["style", "accepted"]), ...Array(6).fill(["style", "dismissed"]), ...Array(3).fill(["naming", "dismissed"])];
  for (const [category, row] of Object.entries(trust(verdicts))) console.log(`${category}: reviewed ${row.reviewed}, precision ${row.precision}, switch off: ${row.off}`);
}

if (import.meta.main) main();
