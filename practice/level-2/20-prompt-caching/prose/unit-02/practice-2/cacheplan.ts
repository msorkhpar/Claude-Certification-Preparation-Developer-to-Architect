// Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract.
import { logger } from "./logger.ts";
const log = logger("cacheplan");

export type Block = { id: string; section: "tools" | "system" | "messages"; tokens: number; volatile?: boolean; breakpoint?: boolean; ttl?: "5m" | "1h" };
export type Planned = { id: string; cache: "5m" | "1h" | null };

const SECTIONS = { tools: 0, system: 1, messages: 2 };
const MAX_BREAKPOINTS = 4;

/** The request cannot be cached as asked (the API would answer 400, or the plan can never hit). */
export class PlanError extends Error {}

function checkTools(blocks: Block[]): void {
  // TODO 1 of 6 (finish this to pass e5): refuse a volatile tool definition.
  // Receives the blocks. Throws PlanError when a block in the "tools" section is volatile (tools come first, nothing volatile may sit
  // in the prefix); returns nothing otherwise. Example: checkTools([{ id: "t", section: "tools", tokens: 9, volatile: true }]) throws
}

function ordered(blocks: Block[]): Block[] {
  // TODO 2 of 6 (finish this to pass m1, e1 and e6): the blocks in cache-friendly order, as a new array.
  // Receives the blocks. Returns a new array: sections in the order of SECTIONS (tools, system, messages), blocks of one section in the
  // order given, then every volatile block moved to the very end in its given order. The input is left unchanged.
  // Example: ids of [m (messages), s (system, volatile), t (tools)] come out as t, m, s
  return [];
}

function wantsBreakpoint(block: Block, total: number, minTokens: number): boolean {
  // TODO 3 of 6 (finish this to pass e2 and e5): does this block carry a breakpoint?
  // Receives the block, total (the stable tokens from the start up to and including this block) and minTokens. Returns true when the
  // block asks for a breakpoint, is not volatile and total is at least minTokens. Example: a breakpoint block at total 500, min 1024 -> false
  return false;
}

function marker(block: Block): "5m" | "1h" {
  // TODO 4 of 6 (finish this to pass m1): the cache lifetime a breakpoint carries.
  // Receives the block. Returns its ttl ("5m" or "1h"), "5m" when it has none. Example: marker({ ttl: "1h" }) -> "1h"
  return "5m";
}

function checkCount(marked: string[]): void {
  // TODO 5 of 6 (finish this to pass e3): at most MAX_BREAKPOINTS breakpoints.
  // Receives the lifetimes of the blocks that kept a breakpoint. Throws PlanError when there are more than MAX_BREAKPOINTS.
  // Example: checkCount(["5m", "5m", "5m", "5m", "5m"]) throws
}

function checkLifetimes(marked: string[]): void {
  // TODO 6 of 6 (finish this to pass e4): a 1h breakpoint must come before every 5m one.
  // Receives the lifetimes of the kept breakpoints in request order. Throws PlanError when a "1h" follows a "5m".
  // Example: checkLifetimes(["5m", "1h"]) throws, checkLifetimes(["1h", "5m"]) does not
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
