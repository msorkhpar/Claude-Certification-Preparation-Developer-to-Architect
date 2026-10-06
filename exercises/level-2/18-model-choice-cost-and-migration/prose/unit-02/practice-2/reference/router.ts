// A cost model and a model router. See ../../statement.md for the contract.
import { logger } from "./logger.ts";
const log = logger("router");

export type Model = { id: string; tier: number; context: number; max_output: number; input: number; output: number; cache_read_multiplier: number; deprecated?: boolean };
export type Usage = { input_tokens?: number; output_tokens?: number; cache_read_input_tokens?: number; cache_creation_input_tokens?: number; cache_creation?: { ephemeral_5m_input_tokens?: number; ephemeral_1h_input_tokens?: number } };
export type Task = { min_tier?: number; max_tokens?: number; batch?: boolean; usage: Usage };

/** No model in the catalog can take the task. */
export class NoModelError extends Error {}

/** The cache-write tokens as [5-minute kind, 1-hour kind]; an unsplit count is all the 5-minute kind. */
function writes(usage: Usage): [number, number] {
  const created = usage.cache_creation;
  if (!created) return [usage.cache_creation_input_tokens ?? 0, 0];
  return [created.ephemeral_5m_input_tokens ?? 0, created.ephemeral_1h_input_tokens ?? 0];
}

/** The cost of the cache parts of a request: the writes and the reads. */
function cacheCost(model: Model, usage: Usage): number {
  const [five, hour] = writes(usage);
  const price = model.input;
  return five * price * 1.25 + hour * price * 2.0 + (usage.cache_read_input_tokens ?? 0) * price * model.cache_read_multiplier;
}

/** The cost after the batch discount: half of it when `batch` is true. */
function applyBatch(total: number, batch: boolean): number {
  return batch ? total * 0.5 : total;
}

/** Cost of one request in micro-dollars (a price of $N per million tokens is N micro-dollars per token). */
export function requestCost(model: Model, usage: Usage, batch = false): number {
  const total = (usage.input_tokens ?? 0) * model.input + cacheCost(model, usage) + (usage.output_tokens ?? 0) * model.output;
  return Math.round(applyBatch(total, batch) * 1e6) / 1e6;
}

/** The tokens the model has to hold as input: uncached input, cache reads and every cache write. */
function inputTokens(usage: Usage): number {
  const [five, hour] = writes(usage);
  return (usage.input_tokens ?? 0) + (usage.cache_read_input_tokens ?? 0) + five + hour;
}

/** Whether the model is allowed and big enough for the task. */
function canTake(model: Model, task: Task, tokens: number): boolean {
  return !model.deprecated && model.tier >= (task.min_tier ?? 1) && tokens <= model.context && (task.max_tokens ?? 0) <= model.max_output;
}

/** The id of the cheapest of `models` for this usage; on a tie the lower tier, then the smaller id. */
function cheapest(models: Model[], usage: Usage, batch: boolean): string {
  const scored = models.map((m) => ({ m, cost: requestCost(m, usage, batch) }));
  scored.sort((a, b) => a.cost - b.cost || a.m.tier - b.m.tier || (a.m.id < b.m.id ? -1 : a.m.id > b.m.id ? 1 : 0));
  return scored[0].m.id;
}

/** An empty list of models is an error: no model can take the task. */
function requireChoice(models: Model[]): void {
  if (models.length === 0) throw new NoModelError("no model can take this task");
}

/** The id of the cheapest model that can take the task; throws NoModelError when none can. */
export function route(catalog: Model[], task: Task): string {
  log.debug("route input", task);
  const tokens = inputTokens(task.usage);
  const eligible = catalog.filter((m) => canTake(m, task, tokens));
  requireChoice(eligible);
  return cheapest(eligible, task.usage, task.batch ?? false);
}
