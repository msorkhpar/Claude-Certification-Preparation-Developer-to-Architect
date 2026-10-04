/** A multi-agent run that survives injected failures: retries with one key, isolation, a breaker, a fallback and a checkpoint. See ../../statement.md. */

/** A failure worth retrying: a timeout, a rate limit, a lost response. */
export class Transient extends Error {}

/** A failure that retrying cannot fix. */
export class Fatal extends Error {}

export type Agent = (key: string, inputs: Record<string, string>) => string;

export function runPlan(plan: any[], agents: Record<string, Agent>, store: Record<string, string>, attempts = 3, breakerThreshold = 3): any {
  // TODO: run the tasks in order and return { done, failed, skipped, degraded, attempts, resumed }.
  return null;
}
