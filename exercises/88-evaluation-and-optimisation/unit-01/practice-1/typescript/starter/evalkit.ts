/** Evaluation kit: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits. See ../../statement.md. */

export type Line = [segment: string, cases: number, right: number, percent: number, cost: number];
export type Pair = [segment: string, oldOk: boolean, newOk: boolean];
export type Option = [name: string, accuracy: number, p95: number, cost: number];
export type Gate = { decision: string; lost: number; gained: number; blocked: string[] };

/** Whole percent, half up, integers only (written for you). */
export function pct(part: number, whole: number): number {
  return whole ? Math.floor((200 * part + whole) / (2 * whole)) : 0;
}

export function segmentTable(results: [string, boolean][], costs: Record<string, number>): Line[] | null {
  // TODO: [segment, cases, right, percent, error cost] per segment, the highest error cost first, ties by segment name.
  return null;
}

export function percentile(values: number[], p: number): number | null {
  // TODO: the nearest-rank percentile of the values, 0 for no values.
  return null;
}

export function abVerdict(x1: number, n1: number, x2: number, n2: number, minN = 200): string | null {
  // TODO: too few cases, no clear difference, new is better or old is better, at 95 percent.
  return null;
}

export function shadowGate(pairs: Pair[], protectedSegments: string[]): Gate | null {
  // TODO: the decision, the losses, the gains and the protected segments that lost, for a shadow run of the new version against the old.
  return null;
}

export function diagnose(found: boolean, supported: boolean, formatOk: boolean, passesOnStronger: boolean): string | null {
  // TODO: retrieval or data, ungrounded answer, format instructions, prompt or task, or model mismatch, in that order.
  return null;
}

export function chooseModel(options: Option[], minAccuracy: number, maxP95: number): string | null {
  // TODO: the cheapest option that meets the accuracy floor and the latency limit, or none.
  return null;
}
