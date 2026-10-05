import { logger } from "./logger.ts";
const log = logger("provenance_loss");
/**
 * What a summary loses, and what a ledger keeps: sources, dates and disagreement.
 *
 * The exam guide (task 5.6) says that source attribution is lost when findings are compressed without their claim-source mappings, that conflicting statistics from credible sources are annotated with their sources and not
 * settled by choosing one, and that dates are required so that a difference over time is not read as a contradiction. Below, seven findings from five invented sources are compressed twice (nothing here calls a model): once into
 * a plain summary that keeps one value per claim, and once into a ledger line per claim that keeps every value with its source and date. The names and figures are invented for the illustration.
 */
export type Row = [string, string, string, string];

export const FINDINGS: Row[] = [
  ["market growth 2024", "12%", "Firm A report", "2024-05-01"],
  ["market growth 2024", "9%", "Firm B survey", "2024-05-01"],
  ["growth forecast", "7%", "Firm C yearbook", "2022-04-01"],
  ["growth forecast", "9%", "Firm B survey", "2024-05-01"],
  ["inflation 2023", "4%", "Firm A report", "2024-05-01"],
  ["inflation 2023", "4%", "Trade paper", "2024-06-10"],
  ["headcount", "910", "Press release", "2024-03-01"],
];

const claimsInOrder = (findings: Row[]): string[] => [...new Set(findings.map((f) => f[0]))];

/** agreed: one value; conflict: different values on the same date; changed: different values on different dates. */
export function status(rows: Row[]): string {
  if (new Set(rows.map((r) => r[1])).size === 1) return "agreed";
  if (rows.some((a) => rows.some((b) => a[1] !== b[1] && a[3] === b[3]))) return "conflict";
  return "changed";
}

/** One line per claim with the first value seen: short, and the sources are gone. */
export function plainSummary(findings: Row[]): string {
  return claimsInOrder(findings).map((claim) => `${claim}: ${findings.find((f) => f[0] === claim)![1]}`).join("\n");
}

/** One line per claim: its status, then every value with its source and date, the oldest date first for a change. */
export function ledgerLines(findings: Row[]): string {
  return claimsInOrder(findings).map((claim) => {
    let rows = findings.filter((f) => f[0] === claim);
    if (status(rows) === "changed") rows = [...rows].sort((a, b) => (a[3] < b[3] ? -1 : a[3] > b[3] ? 1 : 0));
    return `${claim} [${status(rows)}]: ` + rows.map(([, value, source, date]) => `${value} (${source}, ${date})`).join("; ");
  }).join("\n");
}

export function sourcesNamed(text: string, findings: Row[]): number {
  return new Set(findings.filter((f) => text.includes(f[2])).map((f) => f[2])).size;
}

export function main(): void {
  const total = new Set(FINDINGS.map((f) => f[2])).size;
  console.log(`findings: ${FINDINGS.length} from ${total} sources`);
  const summary = plainSummary(FINDINGS);
  console.log("plain summary:");
  console.log(summary);
  console.log(`sources named by the plain summary: ${sourcesNamed(summary, FINDINGS)} of ${total}`);
  const ledger = ledgerLines(FINDINGS);
  console.log("ledger:");
  console.log(ledger);
  console.log(`sources named by the ledger: ${sourcesNamed(ledger, FINDINGS)} of ${total}`);
}

if (import.meta.main) main();
