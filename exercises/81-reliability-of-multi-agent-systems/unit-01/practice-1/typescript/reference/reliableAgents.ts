/** A multi-agent run that survives injected failures: retries with one key, isolation, a breaker, a fallback and a checkpoint. See ../../statement.md. */
import { logger } from "../logger.ts";
const log = logger("reliable_agents");

/** A failure worth retrying: a timeout, a rate limit, a lost response. */
export class Transient extends Error {}

/** A failure that retrying cannot fix. */
export class Fatal extends Error {}

export type Agent = (key: string, inputs: Record<string, string>) => string;

/** One call to an agent: ["ok", result], ["transient", null] or ["fatal", message]. Any other exception is a crash and is not caught. */
function callOnce(agents: Record<string, Agent>, name: string, key: string, inputs: Record<string, string>): [string, string | null] {
  try {
    return ["ok", agents[name](key, inputs)];
  } catch (error) {
    if (error instanceof Transient) return ["transient", null];
    if (error instanceof Fatal) return ["fatal", error.message];
    throw error;
  }
}

/** True when the agent has failed `threshold` calls in a row. */
export function breakerOpen(consecutive: Record<string, number>, name: string, threshold: number): boolean {
  return (consecutive[name] ?? 0) >= threshold;
}

/** A success resets the agent's count to zero; a failure adds one. */
export function recordOutcome(consecutive: Record<string, number>, name: string, ok: boolean): void {
  consecutive[name] = ok ? 0 : (consecutive[name] ?? 0) + 1;
}

/** [result, null] on success or [null, reason]; every call to an agent is counted for the task. */
export function attempt(
  agents: Record<string, Agent>, name: string, key: string, inputs: Record<string, string>, calls: Record<string, number>, taskId: string,
  consecutive: Record<string, number>, attempts: number, threshold: number,
): [string | null, string | null] {
  for (let n = 1; n <= attempts; n++) {
    if (breakerOpen(consecutive, name, threshold)) return [null, "circuit open"];
    calls[taskId] += 1;
    const [status, value] = callOnce(agents, name, key, inputs);
    recordOutcome(consecutive, name, status === "ok");
    if (status === "ok") return [value, null];
    if (status === "fatal") return [null, `fatal: ${value}`];
  }
  return [null, "retries exhausted"];
}

/** The first id in `needs` that is not done, or undefined. */
export function missingDependency(needs: string[], done: Record<string, string>): string | undefined {
  return needs.find((n) => !(n in done));
}

/** The key the fallback agent receives. */
export function fallbackKey(key: string): string {
  return key + ":fallback";
}

/** True when the store already holds this task's result. */
export function shouldResume(tid: string, store: Record<string, string>): boolean {
  return tid in store;
}

/** Write a final result to the store as soon as the task finishes; a degraded result is not final. */
export function checkpoint(store: Record<string, string>, tid: string, result: string, degraded: boolean): void {
  if (!degraded) store[tid] = result;
}

export function runPlan(plan: any[], agents: Record<string, Agent>, store: Record<string, string>, attempts = 3, breakerThreshold = 3): any {
  log.debug("runPlan input", plan);
  const done: Record<string, string> = {};
  const failed: Record<string, string> = {};
  const skipped: Record<string, string> = {};
  const degraded: string[] = [];
  const resumed: string[] = [];
  const calls: Record<string, number> = {};
  const consecutive: Record<string, number> = {};

  for (const task of plan) {
    const tid: string = task.id;
    calls[tid] = 0;
    if (shouldResume(tid, store)) {
      done[tid] = store[tid];
      resumed.push(tid);
      continue;
    }
    const needs: string[] = task.needs ?? [];
    const missing = missingDependency(needs, done);
    if (missing !== undefined) {
      skipped[tid] = `dependency failed: ${missing}`;
      continue;
    }
    const inputs = Object.fromEntries(needs.map((n) => [n, done[n]]));
    let [result, reason] = attempt(agents, task.agent, task.key, inputs, calls, tid, consecutive, attempts, breakerThreshold);
    if (result === null && task.fallback) {
      [result] = attempt(agents, task.fallback, fallbackKey(task.key), inputs, calls, tid, consecutive, attempts, breakerThreshold);
      if (result !== null) {
        done[tid] = result;
        degraded.push(tid);
        checkpoint(store, tid, result, true);
        continue;
      }
    }
    if (result === null) {
      failed[tid] = reason!;
      continue;
    }
    done[tid] = result;
    checkpoint(store, tid, result, false);
  }
  return { done, failed, skipped, degraded, attempts: calls, resumed };
}
