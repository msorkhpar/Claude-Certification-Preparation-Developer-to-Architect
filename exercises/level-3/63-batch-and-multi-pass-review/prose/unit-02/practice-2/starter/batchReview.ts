import { logger } from "./logger.ts";
const log = logger("batch_review");
/** Batch and multi-pass review decisions: when a batch fits, what to resubmit, how a review is split into passes, and how the passes are combined. See ../../statement.md. */

export const SEVERITIES = ["low", "medium", "high"];

export function submissionInterval(slaHours: number, windowHours = 24, handlingHours = 2): number {
  log.debug("submissionInterval input", slaHours);
  // TODO 1 of 8 (finish this to pass m1): the interval. Receives the SLA, the window and the handling time, all in
  //   hours. Return the SLA minus the window minus the handling time. Example: an SLA of 30 hours, defaults -> 4.
  const interval = slaHours;
  // TODO 2 of 8 (finish this to pass e1): the refusal. When the interval is zero or negative, refuse with an error that
  //   says the SLA leaves no room to wait for a batch to fill. Example: an SLA of 26 hours, defaults -> refused.
  return interval;
}

export function chooseApi(blocking: boolean, needsToolLoop = false): string {
  // TODO 3 of 8 (finish this to pass e2): the API. Receives whether something is blocked on the result and whether the
  //   job needs a tool loop. Return synchronous when either is true, otherwise batch. Example: blocking false, tool loop
  //   true -> synchronous.
  return "batch";
}

export function resubmissionPlan(results: Array<[string, string]>, sizes: Record<string, number>, limit: number): Array<[string, string]> {
  const plan: Array<[string, string]> = [];
  for (const [customId, kind] of results) {
    // TODO 4 of 8 (finish this to pass e3): the items to resubmit. Skip a result whose kind is succeeded; every other
    //   result is planned by its custom id. Example: results [(a, succeeded), (b, errored)] -> a plan for b only.
    let action: string;
    // TODO 5 of 8 (finish this to pass e4): the action for one item. When the item's size is above the limit, the action
    //   is chunk; otherwise fix when its kind is invalid_request; otherwise resubmit. An entry exactly at the limit is not
    //   chunked. Example: size 101, limit 100 -> chunk.
    if (kind === "invalid_request") action = "fix";
    else action = "resubmit";
    plan.push([customId, action]);
  }
  return plan;
}

export function reviewPlan(files: string[]): Array<{ name: string; files: string[] }> {
  const passes = files.map((f) => ({ name: `local:${f}`, files: [f] }));
  // TODO 6 of 8 (finish this to pass e5): the integration pass. After the local passes, when there is more than one file
  //   add one pass named integration over all the files. Example: two files -> local, local, integration; one file ->
  //   local only.
  return passes;
}

export function mergePasses(passes: any[][]): any[] {
  const merged = new Map<string, any>();
  passes.forEach((findings, number) => {
    for (const f of findings) {
      const key = JSON.stringify([f.file, f.line, f.issue]);
      let m = merged.get(key);
      if (!m) {
        m = { file: f.file, line: f.line, issue: f.issue, severity: f.severity, passes: new Set<number>(), confidence: f.confidence };
        merged.set(key, m);
      }
      // TODO 7 of 8 (finish this to pass e6): the merge of one finding seen again. When the same finding (file, line,
      //   issue) is reported by another pass, keep the higher of the two severities (low, medium, high) and the lower of
      //   the two confidences. Example: medium at 90 and high at 70 -> high at 70.
      m.passes.add(number);
    }
  });
  return [...merged.values()].map((m) => {
    const count = m.passes.size;
    // TODO 8 of 8 (finish this to pass e7): the route of a merged finding. Receives the number of passes that reported
    //   it and its confidence. Return accept when at least 2 passes reported it and the confidence is at least 80,
    //   otherwise verify. Example: 2 passes, confidence 79 -> verify.
    return { ...m, passes: count, route: "verify" };
  });
}
