/** Evaluation kit: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits. See ../../statement.md. */

export type Line = [segment: string, cases: number, right: number, percent: number, cost: number];
export type Pair = [segment: string, oldOk: boolean, newOk: boolean];
export type Option = [name: string, accuracy: number, p95: number, cost: number];
export type Gate = { decision: string; lost: number; gained: number; blocked: string[] };

export function pct(part: number, whole: number): number {
  return whole ? Math.floor((200 * part + whole) / (2 * whole)) : 0;
}

export function segmentTable(results: [string, boolean][], costs: Record<string, number>): Line[] {
  const seen = new Map<string, [number, number]>();
  for (const [segment, correct] of results) {
    const [total, right] = seen.get(segment) ?? [0, 0];
    seen.set(segment, [total + 1, right + (correct ? 1 : 0)]);
  }
  const table: Line[] = [...seen.entries()].map(([s, [n, r]]) => [s, n, r, pct(r, n), (n - r) * (costs[s] ?? 1)]);
  return table.sort((a, b) => b[4] - a[4] || (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0));
}

export function percentile(values: number[], p: number): number {
  const ordered = [...values].sort((a, b) => a - b);
  return ordered.length ? ordered[Math.floor((p * ordered.length + 99) / 100) - 1] : 0;
}

export function abVerdict(x1: number, n1: number, x2: number, n2: number, minN = 200): string {
  if (n1 < minN || n2 < minN) return "too few cases";
  const bigN = n1 + n2;
  const x = x1 + x2;
  if (x === 0 || x === bigN) return "no clear difference";
  const d = BigInt(x2 * n1 - x1 * n2);
  if (d * d * BigInt(bigN) * 10000n < 38416n * BigInt(n1) * BigInt(n2) * BigInt(x) * BigInt(bigN - x)) return "no clear difference";
  return d > 0n ? "new is better" : "old is better";
}

export function shadowGate(pairs: Pair[], protectedSegments: string[]): Gate {
  const lost = pairs.filter(([, o, n]) => o && !n).map((p) => p[0]);
  const gained = pairs.filter(([, o, n]) => n && !o).length;
  const blocked = [...new Set(lost.filter((s) => protectedSegments.includes(s)))].sort();
  const ship = blocked.length === 0 && lost.length <= gained;
  return { decision: ship ? "ship" : "hold", lost: lost.length, gained, blocked };
}

export function diagnose(found: boolean, supported: boolean, formatOk: boolean, passesOnStronger: boolean): string {
  if (!found) return "retrieval or data";
  if (!supported) return "ungrounded answer";
  if (!formatOk) return "format instructions";
  if (!passesOnStronger) return "prompt or task";
  return "model mismatch";
}

export function chooseModel(options: Option[], minAccuracy: number, maxP95: number): string {
  const fit = options.filter((o) => o[1] >= minAccuracy && o[2] <= maxP95);
  fit.sort((a, b) => a[3] - b[3] || (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0));
  return fit.length ? fit[0][0] : "none";
}
