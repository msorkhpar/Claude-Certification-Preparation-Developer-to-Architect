import { logger } from "./logger.ts";
const log = logger("batch_review");
/** Batch and multi-pass review decisions: when a batch fits, what to resubmit, how a review is split into passes, and how the passes are combined. See ../../statement.md. */

export const SEVERITIES = ["low", "medium", "high"];

export function submissionInterval(slaHours: number, windowHours = 24, handlingHours = 2): number {
  log.debug("submissionInterval input", slaHours);
  const interval = slaHours - windowHours - handlingHours;
  if (interval <= 0) throw new Error("the SLA leaves no room to wait for a batch to fill");
  return interval;
}

export function chooseApi(blocking: boolean, needsToolLoop = false): string {
  return blocking || needsToolLoop ? "synchronous" : "batch";
}

export function resubmissionPlan(results: Array<[string, string]>, sizes: Record<string, number>, limit: number): Array<[string, string]> {
  const plan: Array<[string, string]> = [];
  for (const [customId, kind] of results) {
    if (kind === "succeeded") continue;
    let action: string;
    if ((sizes[customId] ?? 0) > limit) action = "chunk";
    else if (kind === "invalid_request") action = "fix";
    else action = "resubmit";
    plan.push([customId, action]);
  }
  return plan;
}

export function reviewPlan(files: string[]): Array<{ name: string; files: string[] }> {
  const passes = files.map((f) => ({ name: `local:${f}`, files: [f] }));
  if (files.length > 1) passes.push({ name: "integration", files: [...files] });
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
      if (SEVERITIES.indexOf(f.severity) > SEVERITIES.indexOf(m.severity)) m.severity = f.severity;
      m.confidence = Math.min(m.confidence, f.confidence);
      m.passes.add(number);
    }
  });
  return [...merged.values()].map((m) => {
    const count = m.passes.size;
    return { ...m, passes: count, route: count >= 2 && m.confidence >= 80 ? "accept" : "verify" };
  });
}
