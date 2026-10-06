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

function cacheCost(model: Model, usage: Usage): number {
  // TODO 1 of 6 (finish this to pass e1): the cost of the cache parts of a request: the writes and the reads.
  // Receives the model (`input` price, `cache_read_multiplier`) and the usage. Returns the 5-minute write tokens times 1.25 times the input price, plus the 1-hour write
  // tokens times 2 times the input price, plus the cache read tokens times the input price times the model's cache_read_multiplier. writes(usage) gives the two write counts.
  // Example: input price 4, cache_read_input_tokens 1000, multiplier 0.05 -> 200
  return 0;
}

function applyBatch(total: number, batch: boolean): number {
  // TODO 2 of 6 (finish this to pass e2): the cost after the batch discount.
  // Receives the total cost and whether the request is a batch. Returns half of the total when `batch` is true, the total unchanged otherwise.
  // Example: applyBatch(1000, true) -> 500, applyBatch(1000, false) -> 1000
  return total;
}

/** Cost of one request in micro-dollars (a price of $N per million tokens is N micro-dollars per token). */
export function requestCost(model: Model, usage: Usage, batch = false): number {
  const total = (usage.input_tokens ?? 0) * model.input + cacheCost(model, usage) + (usage.output_tokens ?? 0) * model.output;
  return Math.round(applyBatch(total, batch) * 1e6) / 1e6;
}

function inputTokens(usage: Usage): number {
  // TODO 3 of 6 (finish this to pass e4): the tokens the model has to hold as input.
  // Receives the usage. Returns uncached input tokens plus cache read tokens plus every cache write (both kinds; writes(usage) gives them). A missing field counts as 0.
  // Example: { input_tokens: 250000, cache_read_input_tokens: 50000 } -> 300000
  return 0;
}

function canTake(model: Model, task: Task, tokens: number): boolean {
  // TODO 4 of 6 (finish this to pass m1, e4 and e5): whether the model is allowed and big enough for the task.
  // Receives the model, the task (`min_tier` default 1, `max_tokens` default 0) and the task's total input tokens. Returns true when the model is not deprecated,
  // its tier is at least min_tier, its context holds the input tokens and its max_output holds max_tokens.
  // Example: a model with context 200000 and tokens 250000 -> false
  return false;
}

function cheapest(models: Model[], usage: Usage, batch: boolean): string {
  // TODO 5 of 6 (finish this to pass m1, e3 and e6): the id of the cheapest of `models` for this usage.
  // Receives a non-empty array of models, the usage and the batch flag. Prices each with requestCost(model, usage, batch) and returns the id of the lowest; on equal
  // cost the lower tier wins, then the smaller id. The order of the array never matters.
  // Example: Sonnet and Opus costing 200000 each -> the Sonnet id (tier 2 before tier 3)
  return "";
}

function requireChoice(models: Model[]): void {
  // TODO 6 of 6 (finish this to pass e5): an empty list of models is an error.
  // Receives the models that can take the task. Throws new NoModelError("no model can take this task") when the array is empty; returns nothing otherwise.
  // Example: requireChoice([]) throws NoModelError
}

/** The id of the cheapest model that can take the task; throws NoModelError when none can. */
export function route(catalog: Model[], task: Task): string {
  log.debug("route input", task);
  const tokens = inputTokens(task.usage);
  const eligible = catalog.filter((m) => canTake(m, task, tokens));
  requireChoice(eligible);
  return cheapest(eligible, task.usage, task.batch ?? false);
}
