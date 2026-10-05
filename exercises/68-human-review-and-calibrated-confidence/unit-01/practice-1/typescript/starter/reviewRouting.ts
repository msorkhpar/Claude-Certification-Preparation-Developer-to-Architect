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
  // TODO 1 of 7 (finish this to pass m1, e1): the segment rows. After the overall row, add one row for each segment
  //   ("doc_type/field"), sorted by name, with its correct count, its total and the rounded percent. Example:
  //   invoice/total 8 of 10 -> {segment: invoice/total, correct 8, total 10, percent 80}.
  for (const name of [...groups.keys()].sort()) {
    const [c, t] = groups.get(name)!;
    out.push({ segment: name, correct: c, total: t, percent: out[0].percent });
  }
  return out;
}

export function canAutomate(records: any[], threshold: number, minN: number): { automate: boolean; failing: string[]; undersampled: string[] } {
  const failing: string[] = [];
  const undersampled: string[] = [];
  const segments = accuracyBy(records).slice(1);
  for (const s of segments) {
    // TODO 2 of 7 (finish this to pass e2): the sort of the segments. For each segment (not the overall row): when its
    //   total is below min_n it is undersampled; otherwise when its percent is below the threshold it is failing. Example:
    //   min_n 5, segment with 4 records -> undersampled; 5 records at 70 percent, threshold 90 -> failing.
  }
  return { automate: segments.length > 0 && failing.length === 0 && undersampled.length === 0, failing, undersampled };
}

export function calibrateThreshold(labeled: Array<[number, boolean]>, target: number): number | null {
  // TODO 3 of 7 (finish this to pass e3, e4): the threshold. Receives the labelled items (confidence, correct) and the
  //   target precision in percent. Try each distinct confidence from the lowest; return the first for which the items at
  //   or above it are right at least target percent of the time; return none when no level does. Example: target 90 and no
  //   level reaches it -> none.
  return 0;
}

export function stratifiedSample(items: any[], perStratum: number): string[] {
  const strata: string[] = [];
  for (const item of items) if (!strata.includes(item.stratum)) strata.push(item.stratum);
  const chosen: string[] = [];
  for (const stratum of strata) {
    const members = items.filter((i) => i.stratum === stratum).sort((a, b) => a.rank - b.rank || (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));
    // TODO 4 of 7 (finish this to pass e5): the sample of one stratum. `members` are the stratum's items sorted by rank
    //   then id. Take only the first per_stratum of them and add their ids. Example: 5 items, per_stratum 2 -> the 2 best
    //   ranked.
    chosen.push(...members.map((i) => i.id));
  }
  return chosen;
}

export function route(extractions: any[], threshold: number, capacity: number): { review: string[]; backlog: string[]; auto: string[] } {
  const priority = (e: any) => (e.conflict ? 0 : e.confidence);
  // TODO 5 of 7 (finish this to pass e6): the review queue. Keep the extractions that have a conflict or a confidence
  //   below the threshold, ordered with conflicts first, then by confidence (lowest first), then by id. Example: threshold
  //   80, a conflict, a 60 and a 90 -> the conflict, then the 60.
  const candidates = [...extractions];
  const queue = candidates.map((e) => e.id);
  const flagged = new Set(queue);
  // TODO 6 of 7 (finish this to pass e7): the capacity cut. Split the queue: the first `capacity` ids go to review, the
  //   others to the backlog, both in order. Example: queue [a, b, c], capacity 2 -> review [a, b], backlog [c].
  return { review: queue, backlog: [], auto: extractions.filter((e) => !flagged.has(e.id)).map((e) => e.id) };
}

export function checkpoint(action: string, amount: number, limit = 1000): string {
  // TODO 7 of 7 (finish this to pass e8): the checkpoint. Receives the action, the amount and the limit. Return human
  //   when the action is in IRREVERSIBLE or the amount is above the limit, otherwise auto. Example: close_account for 10
  //   -> human.
  return "auto";
}
