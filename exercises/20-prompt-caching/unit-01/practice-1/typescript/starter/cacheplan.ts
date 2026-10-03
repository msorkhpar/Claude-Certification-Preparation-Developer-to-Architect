// Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract.
export type Block = { id: string; section: "tools" | "system" | "messages"; tokens: number; volatile?: boolean; breakpoint?: boolean; ttl?: "5m" | "1h" };
export type Planned = { id: string; cache: "5m" | "1h" | null };

/** The request cannot be cached as asked. */
export class PlanError extends Error {}

export function planRequest(blocks: Block[], minTokens = 1024): Planned[] {
  // TODO: return the blocks in cache-friendly order, each as { id, cache: null | "5m" | "1h" }.
  return undefined as unknown as Planned[];
}
