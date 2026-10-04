/** A multi-agent run that survives injected failures: retries with one key, isolation, a breaker, a fallback and a checkpoint. See ../../statement.md. */

/** A failure worth retrying: a timeout, a rate limit, a lost response. */
export class Transient extends Error {}

/** A failure that retrying cannot fix. */
export class Fatal extends Error {}

export type Agent = (key: string, inputs: Record<string, string>) => string;

export function runPlan(plan: any[], agents: Record<string, Agent>, store: Record<string, string>, attempts = 3, breakerThreshold = 3): any {
  const done: Record<string, string> = {};
  const failed: Record<string, string> = {};
  const skipped: Record<string, string> = {};
  const degraded: string[] = [];
  const resumed: string[] = [];
  const calls: Record<string, number> = {};
  const consecutive: Record<string, number> = {};

  /** [result, null] on success or [null, reason]; every call to an agent is counted for the task. */
  function attempt(agentName: string, key: string, inputs: Record<string, string>, taskId: string): [string | null, string | null] {
    for (let n = 1; n <= attempts; n++) {
      if ((consecutive[agentName] ?? 0) >= breakerThreshold) return [null, "circuit open"];
      calls[taskId] += 1;
      try {
        const result = agents[agentName](key, inputs);
        consecutive[agentName] = 0;
        return [result, null];
      } catch (error) {
        if (error instanceof Transient) {
          consecutive[agentName] = (consecutive[agentName] ?? 0) + 1;
          continue;
        }
        if (error instanceof Fatal) {
          consecutive[agentName] = (consecutive[agentName] ?? 0) + 1;
          return [null, `fatal: ${error.message}`];
        }
        throw error;
      }
    }
    return [null, "retries exhausted"];
  }

  for (const task of plan) {
    const tid: string = task.id;
    calls[tid] = 0;
    if (tid in store) {
      done[tid] = store[tid];
      resumed.push(tid);
      continue;
    }
    const needs: string[] = task.needs ?? [];
    const missing = needs.find((n) => !(n in done));
    if (missing !== undefined) {
      skipped[tid] = `dependency failed: ${missing}`;
      continue;
    }
    const inputs = Object.fromEntries(needs.map((n) => [n, done[n]]));
    let [result, reason] = attempt(task.agent, task.key, inputs, tid);
    if (result === null && task.fallback) {
      [result] = attempt(task.fallback, task.key + ":fallback", inputs, tid);
      if (result !== null) {
        done[tid] = result;
        degraded.push(tid);
        continue;
      }
    }
    if (result === null) {
      failed[tid] = reason!;
      continue;
    }
    done[tid] = result;
    store[tid] = result;
  }
  return { done, failed, skipped, degraded, attempts: calls, resumed };
}
