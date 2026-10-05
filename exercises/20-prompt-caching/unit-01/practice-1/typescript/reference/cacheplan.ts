// Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract.
import { logger } from "../logger.ts";
const log = logger("cacheplan");

export type Block = { id: string; section: "tools" | "system" | "messages"; tokens: number; volatile?: boolean; breakpoint?: boolean; ttl?: "5m" | "1h" };
export type Planned = { id: string; cache: "5m" | "1h" | null };

const SECTIONS = { tools: 0, system: 1, messages: 2 };
const MAX_BREAKPOINTS = 4;

/** The request cannot be cached as asked (the API would answer 400, or the plan can never hit). */
export class PlanError extends Error {}

function checkTools(blocks: Block[]): void {
  for (const b of blocks) {
    if (b.volatile && b.section === "tools") throw new PlanError(`tool definition ${b.id} cannot be volatile: tools come first`);
  }
}

function ordered(blocks: Block[]): Block[] {
  const bySection = [...blocks].sort((a, b) => SECTIONS[a.section] - SECTIONS[b.section]); // sort is stable
  return [...bySection.filter((b) => !b.volatile), ...bySection.filter((b) => b.volatile)];
}

function wantsBreakpoint(block: Block, total: number, minTokens: number): boolean {
  return Boolean(block.breakpoint) && !block.volatile && total >= minTokens;
}

function marker(block: Block): "5m" | "1h" {
  return block.ttl ?? "5m";
}

function checkCount(marked: string[]): void {
  if (marked.length > MAX_BREAKPOINTS) throw new PlanError(`${marked.length} breakpoints: at most ${MAX_BREAKPOINTS}`);
}

function checkLifetimes(marked: string[]): void {
  let seenFive = false;
  for (const cache of marked) {
    if (cache === "5m") seenFive = true;
    else if (seenFive) throw new PlanError("a 1h breakpoint must come before every 5m breakpoint");
  }
}

export function planRequest(blocks: Block[], minTokens = 1024): Planned[] {
  log.debug("planRequest input", blocks);
  checkTools(blocks);
  const plan: Planned[] = [];
  let total = 0;
  for (const b of ordered(blocks)) {
    if (!b.volatile) total += b.tokens;
    plan.push({ id: b.id, cache: wantsBreakpoint(b, total, minTokens) ? marker(b) : null });
  }
  const marked = plan.filter((p) => p.cache).map((p) => p.cache as string);
  checkCount(marked);
  checkLifetimes(marked);
  return plan;
}
