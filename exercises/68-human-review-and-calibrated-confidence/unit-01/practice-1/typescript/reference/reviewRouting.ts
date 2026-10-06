import { logger } from "../logger.ts";
const log = logger("review_routing");
/** Human review without fooling yourself: accuracy by segment, the decision to automate, a calibrated confidence threshold, a stratified sample, review routing within capacity, and checkpoints for irreversible actions. See ../../statement.md. */

export const IRREVERSIBLE = ["delete_records", "send_payment", "close_account"];

const percent = (correct: number, total: number): number => Math.floor((200 * correct + total) / (2 * total));

export function accuracyBy(records: any[]): any[] {
  log.debug("accuracyBy input", records);
  const groups = new Map<string, [number, number]>();
  for (const r of records) {
    const key = `${r.doc_type}/${r.field}`;
    const [c, t] = groups.get(key) ?? [0, 0];
    groups.set(key, [c + (r.correct ? 1 : 0), t + 1]);
  }
  let correct = 0;
  let total = 0;
  for (const [c, t] of groups.values()) {
    correct += c;
    total += t;
  }
  const out: any[] = [{ segment: "overall", correct, total, percent: total ? percent(correct, total) : 0 }];
  for (const name of [...groups.keys()].sort()) {
    const [c, t] = groups.get(name)!;
    out.push({ segment: name, correct: c, total: t, percent: percent(c, t) });
  }
  return out;
}

export function canAutomate(records: any[], threshold: number, minN: number): { automate: boolean; failing: string[]; undersampled: string[] } {
  const failing: string[] = [];
  const undersampled: string[] = [];
  const segments = accuracyBy(records).slice(1);
  for (const s of segments) {
    if (s.total < minN) undersampled.push(s.segment);
    else if (s.percent < threshold) failing.push(s.segment);
  }
  return { automate: segments.length > 0 && failing.length === 0 && undersampled.length === 0, failing, undersampled };
}

export function calibrateThreshold(labeled: Array<[number, boolean]>, target: number): number | null {
  for (const t of [...new Set(labeled.map(([c]) => c))].sort((a, b) => a - b)) {
    const kept = labeled.filter(([c]) => c >= t);
    const right = kept.filter(([, ok]) => ok).length;
    if (100 * right >= target * kept.length) return t;
  }
  return null;
}

export function stratifiedSample(items: any[], perStratum: number): string[] {
  const strata: string[] = [];
  for (const item of items) if (!strata.includes(item.stratum)) strata.push(item.stratum);
  const chosen: string[] = [];
  for (const stratum of strata) {
    const members = items.filter((i) => i.stratum === stratum).sort((a, b) => a.rank - b.rank || (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));
    chosen.push(...members.slice(0, perStratum).map((i) => i.id));
  }
  return chosen;
}

export function route(extractions: any[], threshold: number, capacity: number): { review: string[]; backlog: string[]; auto: string[] } {
  const priority = (e: any) => (e.conflict ? 0 : e.confidence);
  const candidates = extractions.filter((e) => e.conflict || e.confidence < threshold).sort((a, b) => priority(a) - priority(b) || (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));
  const queue = candidates.map((e) => e.id);
  const flagged = new Set(queue);
  return { review: queue.slice(0, capacity), backlog: queue.slice(capacity), auto: extractions.filter((e) => !flagged.has(e.id)).map((e) => e.id) };
}

export function checkpoint(action: string, amount: number, limit = 1000): string {
  return IRREVERSIBLE.includes(action) || amount > limit ? "human" : "auto";
}
