/** Batch and multi-pass review decisions: when a batch fits, what to resubmit, how a review is split into passes, and how the passes are combined. See ../../statement.md. */

export const SEVERITIES = ["low", "medium", "high"];

export function submissionInterval(slaHours: number, windowHours = 24, handlingHours = 2): number | null {
  // TODO: the hours between submissions that still keep every item inside the SLA; refuse an SLA with no room.
  return null;
}

export function chooseApi(blocking: boolean, needsToolLoop = false): string | null {
  // TODO: "synchronous" or "batch" for a workload.
  return null;
}

export function resubmissionPlan(results: Array<[string, string]>, sizes: Record<string, number>, limit: number): Array<[string, string]> | null {
  // TODO: from [customId, kind] results, the [customId, action] pairs for the items that did not succeed.
  return null;
}

export function reviewPlan(files: string[]): Array<{ name: string; files: string[] }> | null {
  // TODO: the passes of a multi-file review, each { name, files }.
  return null;
}

export function mergePasses(passes: any[][]): any[] | null {
  // TODO: combine the findings of independent passes; each result has file, line, issue, severity, passes, confidence and route.
  return null;
}
