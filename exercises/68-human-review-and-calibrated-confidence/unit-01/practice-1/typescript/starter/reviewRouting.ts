/** Human review without fooling yourself: accuracy by segment, the decision to automate, a calibrated confidence threshold, a stratified sample, review routing within capacity, and checkpoints for irreversible actions. See ../../statement.md. */

export const IRREVERSIBLE = ["delete_records", "send_payment", "close_account"];

export function accuracyBy(records: any[]): any[] | null {
  // TODO: the overall accuracy and the accuracy of every doc_type/field segment, as objects { segment, correct, total, percent }.
  return null;
}

export function canAutomate(records: any[], threshold: number, minN: number): any {
  // TODO: { automate, failing, undersampled }: every segment must have enough records and reach the threshold.
  return null;
}

export function calibrateThreshold(labeled: Array<[number, boolean]>, target: number): number | null {
  // TODO: the lowest confidence whose auto-accepted items (confidence at or above it) reach the target precision, or null.
  return null;
}

export function stratifiedSample(items: any[], perStratum: number): string[] | null {
  // TODO: the ids of the lowest-ranked items of every stratum.
  return null;
}

export function route(extractions: any[], threshold: number, capacity: number): any {
  // TODO: { review, backlog, auto }: low confidence and conflicts go to review, the weakest first, within the capacity.
  return null;
}

export function checkpoint(action: string, amount: number, limit = 1000): string | null {
  // TODO: "human" or "auto" for an action.
  return null;
}
