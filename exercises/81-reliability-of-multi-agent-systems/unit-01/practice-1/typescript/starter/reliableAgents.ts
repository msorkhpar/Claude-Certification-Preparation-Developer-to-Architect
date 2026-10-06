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

/**
 * TODO 1 of 7 (unlocks e4): is the breaker of this agent open?
 * Receives the map of consecutive failures per agent, the agent name and the threshold. Returns true when the agent's count has reached the threshold
 * (a name that is not in the map has a count of 0). Example: breakerOpen({ w: 3 }, "w", 3) -> true, breakerOpen({}, "w", 3) -> false
 */
export function breakerOpen(consecutive: Record<string, number>, name: string, threshold: number): boolean {
  return false;
}

/**
 * TODO 2 of 7 (unlocks e4): update the consecutive-failure count of an agent after a call.
 * Receives the map of consecutive failures, the agent name and whether the call succeeded. Sets the count to 0 on a success and adds one on a failure.
 * Example: after a failure then a success then a failure, the count is 1
 */
export function recordOutcome(consecutive: Record<string, number>, name: string, ok: boolean): void {}

/**
 * TODO 3 of 7 (unlocks m1, e1, e2 and e4): call an agent for a task, retrying a transient failure.
 * Receives the agents, the agent name, the task's key and inputs, the per-task call counts, the task id, the breaker counts, the attempt limit and the breaker
 * threshold. Up to `attempts` times: if the breaker is open return [null, "circuit open"] without calling; otherwise add one to `calls[taskId]`, make the
 * call with `callOnce` (always the same key), record the outcome and return [result, null] on "ok"; return [null, "fatal: <message>"] on "fatal" without
 * retrying; retry on "transient". After the last attempt return [null, "retries exhausted"].
 * Example: an agent that fails once with Transient and then answers "ok" -> ["ok", null] with two calls counted
 */
export function attempt(
  agents: Record<string, Agent>, name: string, key: string, inputs: Record<string, string>, calls: Record<string, number>, taskId: string,
  consecutive: Record<string, number>, attempts: number, threshold: number,
): [string | null, string | null] {
  return [null, "retries exhausted"];
}

/**
 * TODO 4 of 7 (unlocks e3): the first dependency that did not finish.
 * Receives the ids a task needs and the object of finished results. Returns the first id in `needs` (in order) that is not in `done`, or undefined when all are.
 * Example: missingDependency(["a", "b"], { a: "x" }) -> "b"
 */
export function missingDependency(needs: string[], done: Record<string, string>): string | undefined {
  return undefined;
}

/**
 * TODO 5 of 7 (unlocks e5): the idempotency key for the fallback agent.
 * Receives the task's key. Returns it followed by `:fallback`, so that a repeat of the fallback is recognised without colliding with the primary.
 * Example: fallbackKey("key-s") -> "key-s:fallback"
 */
export function fallbackKey(key: string): string {
  return key;
}

/**
 * TODO 6 of 7 (unlocks e6): is this task already checkpointed?
 * Receives a task id and the store of checkpointed results. Returns true when the store holds the task, so that no agent is called for it.
 * Example: shouldResume("a", { a: "ok" }) -> true
 */
export function shouldResume(tid: string, store: Record<string, string>): boolean {
  return false;
}

/**
 * TODO 7 of 7 (unlocks m1, e5, e6 and e7): write a finished task's result to the store.
 * Receives the store, the task id, the result and whether the result is degraded (it came from the fallback). Writes the result to the store unless it is
 * degraded, so that a later run tries the primary again. It is called as soon as each task finishes, so a crash later in the run loses nothing finished.
 * Example: checkpoint(store, "a", "ok", false) -> store is { a: "ok" }; with degraded true the store is unchanged
 */
export function checkpoint(store: Record<string, string>, tid: string, result: string, degraded: boolean): void {}

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
