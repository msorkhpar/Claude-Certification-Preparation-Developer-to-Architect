/** Rollout kit: the retirement calendar, the settings a new model refuses, a gate on a regression suite and a staged roll-out. See ../../statement.md. */
export type Case = { id: string; segment: string; mustPass: boolean; oldOk: boolean; newOk: boolean; oldCost: number; newCost: number; newMs: number };
export type Request = { model: string; temperature: number | null; topP: number | null; topK: number | null; thinking: string; toolChoice: string; strict: boolean; prefill: boolean };

export const STAGES = [1, 5, 25, 100];
export const TARGET = "claude-sonnet-5-5";

/** Whole days from one ISO date to another, negative when it has passed (written for you). */
export function daysUntil(today: string, when: string): number {
  const [y1, m1, d1] = today.split("-").map(Number);
  const [y2, m2, d2] = when.split("-").map(Number);
  return Math.round((Date.UTC(y2, m2 - 1, d2) - Date.UTC(y1, m1 - 1, d1)) / 86400000);
}

/** Nearest-rank percentile, 0 for no values (written for you). */
export function percentile(values: number[], p: number): number {
  const ordered = [...values].sort((a, b) => a - b);
  return ordered.length ? ordered[Math.floor((p * ordered.length + 99) / 100) - 1] : 0;
}

export function retirementStatus(models: [string, string, boolean][], today: string): string[] | null {
  // TODO: "<name>: <days> days, <level>" per model, the nearest retirement first, with " (tentative)" when the date may move.
  return null;
}

export function migrateRequest(request: Request, target = TARGET): [Request, string[]] | null {
  // TODO: [new request, list of changes]: drop what the target refuses, replace what it changes, and name each change.
  return null;
}

export function gate(cases: Case[], protectedSegments: string[], maxCostUp: number, maxP95: number): { decision: string; reasons: string[] } | null {
  // TODO: the decision (go or no-go) and the reasons: must-pass, protected segments, net loss, cost and tail, in that order.
  return null;
}

export function rolloutStep(stage: number, requests: number, errors: number, minRequests: number, maxErrorsPer1000: number): string | null {
  // TODO: "hold at S", "rollback to 0", "complete" or "advance to N" for one observed stage.
  return null;
}
