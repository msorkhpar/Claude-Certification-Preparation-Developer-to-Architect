/**
 * Evaluation decisions for a system that changes: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits.
 *
 * The cases, counts and timings are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domain 4) and the Claude documentation pages on defining success, developing tests, reducing hallucinations and reducing latency, read on 2026-10-04. Nothing here calls a model.
 */
export type Row = [segment: string, oldOk: boolean, newOk: boolean];
export type Line = [segment: string, cases: number, right: number, percent: number, cost: number];
export type Option = [name: string, accuracy: number, p95: number, cost: number];

export const COSTS: Record<string, number> = { "order status": 1, refund: 20, policy: 5, complaint: 10 };
const GROUPS: [string, number][] = [["order status", 30], ["refund", 8], ["policy", 10], ["complaint", 4]];

/** One row per case: right in both, right only with the current prompt, right only with the new one, then wrong in both. */
export function buildCases(): Row[] {
  const rows: Row[] = [];
  const both: Record<string, number> = { "order status": 30, refund: 5, policy: 8, complaint: 3 };
  const oldOnly: Record<string, number> = { "order status": 0, refund: 0, policy: 1, complaint: 1 };
  const newOnly: Record<string, number> = { "order status": 0, refund: 2, policy: 0, complaint: 0 };
  for (const [segment, count] of GROUPS) {
    const rest = count - both[segment] - oldOnly[segment] - newOnly[segment];
    for (let i = 0; i < both[segment]; i++) rows.push([segment, true, true]);
    for (let i = 0; i < oldOnly[segment]; i++) rows.push([segment, true, false]);
    for (let i = 0; i < newOnly[segment]; i++) rows.push([segment, false, true]);
    for (let i = 0; i < rest; i++) rows.push([segment, false, false]);
  }
  return rows;
}

/** Whole percent, half up, with integers only so that every language agrees. */
export function pct(part: number, whole: number): number {
  return whole ? Math.floor((200 * part + whole) / (2 * whole)) : 0;
}

/** Per segment: cases, right answers, accuracy and the cost of the wrong ones, worst cost first. */
export function segmentTable(rows: Row[], which: "old" | "new"): Line[] {
  const out = new Map<string, [number, number]>();
  for (const [segment, oldOk, newOk] of rows) {
    const ok = which === "old" ? oldOk : newOk;
    const [n, right] = out.get(segment) ?? [0, 0];
    out.set(segment, [n + 1, right + (ok ? 1 : 0)]);
  }
  const table: Line[] = [...out.entries()].map(([s, [n, r]]) => [s, n, r, pct(r, n), (n - r) * (COSTS[s] ?? 1)]);
  return table.sort((a, b) => b[4] - a[4] || (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0));
}

/** Nearest rank: the value at rank ceil(p * n / 100) of the sorted list. */
export function percentile(values: number[], p: number): number {
  const ordered = [...values].sort((a, b) => a - b);
  return ordered.length ? ordered[Math.floor((p * ordered.length + 99) / 100) - 1] : 0;
}

/** Two-proportion test at 95 percent, done with integers: z squared = D*D*N / (n1*n2*X*(N-X)), compared with 1.96 squared. */
export function abVerdict(x1: number, n1: number, x2: number, n2: number, minN = 200): string {
  if (n1 < minN || n2 < minN) return "too few cases";
  const bigN = n1 + n2;
  const x = x1 + x2;
  if (x === 0 || x === bigN) return "no clear difference";
  const d = BigInt(x2 * n1 - x1 * n2);
  if (d * d * BigInt(bigN) * 10000n < 38416n * BigInt(n1) * BigInt(n2) * BigInt(x) * BigInt(bigN - x)) return "no clear difference";
  return d > 0n ? "new is better" : "old is better";
}

/** Ship only when no protected segment lost a right answer and the new version lost fewer than it gained. */
export function shadowGate(rows: Row[], protectedSegments: Set<string>): { decision: string; lost: number; gained: number; blocked: string[] } {
  const lost = rows.filter(([, o, n]) => o && !n).map((r) => r[0]);
  const gained = rows.filter(([, o, n]) => n && !o).length;
  const blocked = [...new Set(lost.filter((s) => protectedSegments.has(s)))].sort();
  const ship = blocked.length === 0 && lost.length <= gained;
  return { decision: ship ? "ship" : "hold", lost: lost.length, gained, blocked };
}

/** Where to look first: the evidence, then the grounding, then the format, then the task, and the model last. */
export function diagnose(found: boolean, supported: boolean, formatOk: boolean, passesOnStronger: boolean): string {
  if (!found) return "retrieval or data";
  if (!supported) return "ungrounded answer";
  if (!formatOk) return "format instructions";
  if (!passesOnStronger) return "prompt or task";
  return "model mismatch";
}

/** The cheapest option that meets the accuracy floor and the latency limit, ties by name; none when nothing does. */
export function chooseModel(options: Option[], minAccuracy: number, maxP95: number): string {
  const fit = options.filter((o) => o[1] >= minAccuracy && o[2] <= maxP95);
  fit.sort((a, b) => a[3] - b[3] || (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0));
  return fit.length ? fit[0][0] : "none";
}

function main(): void {
  const rows = buildCases();
  for (const [which, label] of [["old", "current prompt"], ["new", "new prompt"]] as const) {
    const table = segmentTable(rows, which);
    const right = table.reduce((a, t) => a + t[2], 0);
    console.log(`${label}: ${right}/${rows.length} right, ${pct(right, rows.length)}% overall, error cost ${table.reduce((a, t) => a + t[4], 0)}`);
    for (const [s, n, r, p, c] of table) console.log(`  ${s.padEnd(13)} ${r}/${n} ${p}% cost ${c}`);
  }
  const latencies = [800, 820, 850, 900, 950, 980, 1000, 1100, 1200, 1500, 2400, 4800];
  console.log(`latency ms: mean ${Math.floor(latencies.reduce((a, b) => a + b, 0) / latencies.length)}, p50 ${percentile(latencies, 50)}, p95 ${percentile(latencies, 95)}`);
  console.log("live test, 500 cases each, 410 right against 438:", abVerdict(410, 500, 438, 500));
  console.log("live test, 500 cases each, 410 right against 425:", abVerdict(410, 500, 425, 500));
  console.log("live test, 100 cases each, 82 right against 90:", abVerdict(82, 100, 90, 100));
  const gate = shadowGate(rows, new Set(["refund", "complaint"]));
  console.log(`shadow run: ${gate.decision}, lost ${gate.lost}, gained ${gate.gained}, protected segments hit: ${gate.blocked.join(", ") || "none"}`);
  const cases: [string, boolean, boolean, boolean, boolean][] = [["no chunk had the answer", false, false, true, true], ["a claim no chunk supports", true, false, true, true], ["a reply in the wrong shape", true, true, false, true],
    ["fails on a stronger model too", true, true, true, false], ["passes only on a stronger model", true, true, true, true]];
  for (const [name, a, b, c, d] of cases) console.log(`diagnose, ${name}: ${diagnose(a, b, c, d)}`);
  const options: Option[] = [["small", 84, 900, 1], ["medium", 91, 1800, 3], ["large", 95, 4200, 9]];
  console.log(`model for 90% accuracy within 2000 ms: ${chooseModel(options, 90, 2000)}; for 94% within 2000 ms: ${chooseModel(options, 94, 2000)}`);
}

if (import.meta.main) main();
