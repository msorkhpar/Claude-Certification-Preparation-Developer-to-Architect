// Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract.
export type Block = { id: string; section: "tools" | "system" | "messages"; tokens: number; volatile?: boolean; breakpoint?: boolean; ttl?: "5m" | "1h" };
export type Planned = { id: string; cache: "5m" | "1h" | null };

const SECTIONS = { tools: 0, system: 1, messages: 2 };
const MAX_BREAKPOINTS = 4;

/** The request cannot be cached as asked (the API would answer 400, or the plan can never hit). */
export class PlanError extends Error {}

export function planRequest(blocks: Block[], minTokens = 1024): Planned[] {
  for (const b of blocks) {
    if (b.volatile && b.section === "tools") throw new PlanError(`tool definition ${b.id} cannot be volatile: tools come first`);
  }
  const bySection = [...blocks].sort((a, b) => SECTIONS[a.section] - SECTIONS[b.section]); // sort is stable
  const ordered = [...bySection.filter((b) => !b.volatile), ...bySection.filter((b) => b.volatile)];
  const plan: Planned[] = [];
  let total = 0;
  for (const b of ordered) {
    if (!b.volatile) total += b.tokens;
    const wanted = Boolean(b.breakpoint) && !b.volatile && total >= minTokens;
    plan.push({ id: b.id, cache: wanted ? (b.ttl ?? "5m") : null });
  }
  const marked = plan.filter((p) => p.cache);
  if (marked.length > MAX_BREAKPOINTS) throw new PlanError(`${marked.length} breakpoints: at most ${MAX_BREAKPOINTS}`);
  let seenFive = false;
  for (const p of marked) {
    if (p.cache === "5m") seenFive = true;
    else if (seenFive) throw new PlanError("a 1h breakpoint must come before every 5m breakpoint");
  }
  return plan;
}
