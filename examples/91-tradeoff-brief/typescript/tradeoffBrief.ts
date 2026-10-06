import { logger } from "./logger.ts";
const log = logger("tradeoff_brief");
/**
 * One decision told to two audiences: the figures an engineer needs, the same figures in the words a sponsor decides with, and an honest check of each service level at its exact edge.
 *
 * The figures are invented for a utility's billing-dispute assistant; the break-even rule is the one of module 79. Nothing here calls a model.
 */
export type Sla = { name: string; limit: number; direction: string; unit: string };
export type Segment = { name: string; right: number; total: number; errorCost: number };

const group = (n: number): string => String(n).replace(/\B(?=(\d{3})+(?!\d))/g, ",");

/** Whole percent, halves rounded up, and 0 for no cases. */
export function pct(right: number, total: number): number {
  return total ? Math.floor((200 * right + total) / (2 * total)) : 0;
}

/** The accuracy, in whole percent, at or above which a check no longer pays: (1 - accuracy) x error cost <= review cost. */
export function breakEven(errorCost: number, reviewCost: number): number {
  return 100 - Math.ceil((100 * reviewCost) / errorCost);
}

/** A service level is met at its limit exactly, and a miss says by how much. */
export function slaLine(sla: Sla, measured: number): string {
  log.debug("slaLine input", sla);
  const met = sla.direction === "max" ? measured <= sla.limit : measured >= sla.limit;
  const verdict = met ? "met" : `missed by ${Math.abs(measured - sla.limit)} ${sla.unit}`;
  const word = sla.direction === "max" ? "limit" : "floor";
  return `${sla.name}: ${measured} ${sla.unit} against a ${word} of ${sla.limit} ${sla.unit}: ${verdict}`;
}

/** The costliest segment first, with its accuracy and whether a person checks it. */
export function segmentReport(segments: Segment[], reviewCost: number): string[] {
  const sorted = [...segments].sort((a, b) => b.errorCost - a.errorCost || (a.name < b.name ? -1 : a.name > b.name ? 1 : 0));
  return sorted.map((s) => {
    const floor = breakEven(s.errorCost, reviewCost);
    const handling = pct(s.right, s.total) >= floor ? "auto" : "reviewed";
    return `${s.name}: ${pct(s.right, s.total)} percent right, error cost ${s.errorCost}, ${handling} (break-even ${floor})`;
  });
}

/** The same facts for a sponsor (money, risk and one decision) or for an engineer (the numbers that produced them). */
export function brief(audience: string, design: string, cost: number, baseline: number, weakest: Segment, ask: string): string {
  if (audience === "sponsor") {
    return `${design} costs ${group(cost)} a month against ${group(baseline)} for people alone, a saving of ${group(baseline - cost)}. ` +
      `The weakest answers are ${weakest.name}: ${pct(weakest.right, weakest.total)} in 100 are right and each wrong one costs ${weakest.errorCost}, ` +
      `so a person decides them. Decision asked: ${ask}.`;
  }
  return `design=${design}; cost=${cost}; baseline=${baseline}; saving=${baseline - cost}; weakest=${weakest.name} ${pct(weakest.right, weakest.total)}% at ${weakest.errorCost} an error`;
}

function main(): void {
  const latency: Sla = { name: "p95 latency", limit: 2000, direction: "max", unit: "ms" };
  const availability: Sla = { name: "availability", limit: 995, direction: "min", unit: "per mille" };
  const slas: [Sla, number][] = [[latency, 1800], [latency, 2000], [latency, 2150], [availability, 997], [availability, 990]];
  for (const [sla, measured] of slas) console.log(slaLine(sla, measured));
  const segments: Segment[] = [{ name: "status", right: 98, total: 100, errorCost: 12 }, { name: "credit", right: 63, total: 100, errorCost: 250 }, { name: "complaint", right: 91, total: 100, errorCost: 60 }];
  for (const line of segmentReport(segments, 5)) console.log(line);
  const weakest = segments.reduce((a, b) => (pct(b.right, b.total) < pct(a.right, a.total) ? b : a));
  for (const audience of ["sponsor", "engineer"]) console.log(`${audience}: ${brief(audience, "Routing by confidence", 80000, 315000, weakest, "approve the pilot")}`);
}

if (import.meta.main) main();
