/** A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md. */

export const MIN_CACHEABLE = 512; // tokens: a shorter prefix cannot be cached

export const tokens = (text: string): number => Math.ceil(text.length / 4); // one token per four characters, rounded up

export function assemble(modules: any[], variables: Record<string, string>, budget: number): any {
  // TODO: static modules first, then the dynamic ones with their variables filled; drop dynamic modules to fit the budget; mark the breakpoint.
  return null;
}

export function chooseModel(workload: any, models: any[]): string | null {
  // TODO: the name of the cheapest model that meets the tier and the latency, or null.
  return null;
}

export function reusablePrefix(a: any, b: any): number | null {
  // TODO: the tokens of the cached prefix that two assembled prompts share, or 0.
  return null;
}
