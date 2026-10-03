// A cost model and a model router. See ../../statement.md for the contract.
export type Model = { id: string; tier: number; context: number; max_output: number; input: number; output: number; cache_read_multiplier: number; deprecated?: boolean };
export type Usage = { input_tokens?: number; output_tokens?: number; cache_read_input_tokens?: number; cache_creation_input_tokens?: number; cache_creation?: { ephemeral_5m_input_tokens?: number; ephemeral_1h_input_tokens?: number } };
export type Task = { min_tier?: number; max_tokens?: number; batch?: boolean; usage: Usage };

/** No model in the catalog can take the task. */
export class NoModelError extends Error {}

/** Cost of one request in micro-dollars (a price of $N per million tokens is N micro-dollars per token). */
export function requestCost(model: Model, usage: Usage, batch = false): number {
  const price = model.input;
  const created = usage.cache_creation;
  const five = created ? (created.ephemeral_5m_input_tokens ?? 0) : (usage.cache_creation_input_tokens ?? 0);
  const hour = created ? (created.ephemeral_1h_input_tokens ?? 0) : 0;
  let total =
    (usage.input_tokens ?? 0) * price +
    five * price * 1.25 +
    hour * price * 2.0 +
    (usage.cache_read_input_tokens ?? 0) * price * model.cache_read_multiplier +
    (usage.output_tokens ?? 0) * model.output * (batch ? 0.5 : 1);
  return Math.round(total * 1e6) / 1e6;
}

function inputTokens(usage: Usage): number {
  const created = usage.cache_creation;
  const written = created ? (created.ephemeral_5m_input_tokens ?? 0) + (created.ephemeral_1h_input_tokens ?? 0) : (usage.cache_creation_input_tokens ?? 0);
  return (usage.input_tokens ?? 0) + (usage.cache_read_input_tokens ?? 0) + written;
}

/** The id of the cheapest model that can take the task; throws NoModelError when none can. */
export function route(catalog: Model[], task: Task): string {
  const wanted = task.max_tokens ?? 0;
  const eligible = catalog.filter(
    (m) => !m.deprecated && m.tier >= (task.min_tier ?? 1) && inputTokens(task.usage) <= m.context && wanted <= m.max_output,
  );
  if (eligible.length === 0) throw new NoModelError("no model can take this task");
  const scored = eligible.map((m) => ({ m, cost: requestCost(m, task.usage, task.batch ?? false) }));
  scored.sort((a, b) => a.cost - b.cost || a.m.tier - b.m.tier || (a.m.id < b.m.id ? -1 : a.m.id > b.m.id ? 1 : 0));
  return scored[0].m.id;
}
