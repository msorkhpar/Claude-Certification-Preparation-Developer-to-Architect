// A cost model and a model router. See ../../statement.md for the contract.
export type Model = { id: string; tier: number; context: number; max_output: number; input: number; output: number; cache_read_multiplier: number; deprecated?: boolean };
export type Usage = { input_tokens?: number; output_tokens?: number; cache_read_input_tokens?: number; cache_creation_input_tokens?: number; cache_creation?: { ephemeral_5m_input_tokens?: number; ephemeral_1h_input_tokens?: number } };
export type Task = { min_tier?: number; max_tokens?: number; batch?: boolean; usage: Usage };

/** No model in the catalog can take the task. */
export class NoModelError extends Error {}

export function requestCost(model: Model, usage: Usage, batch = false): number {
  // TODO: the cost of one request in micro-dollars, rounded to 6 decimals.
  return undefined as unknown as number;
}

export function route(catalog: Model[], task: Task): string {
  // TODO: the id of the cheapest model that can take the task; throw NoModelError when none can.
  return undefined as unknown as string;
}
