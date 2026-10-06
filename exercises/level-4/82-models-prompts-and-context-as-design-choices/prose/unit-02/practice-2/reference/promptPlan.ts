/** A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md. */
import { logger } from "./logger.ts";
const log = logger("prompt_plan");

export const MIN_CACHEABLE = 512; // tokens: a shorter prefix cannot be cached

export const tokens = (text: string): number => Math.ceil(text.length / 4); // one token per four characters, rounded up

type Kept = { name: string; text: string; priority: number };

/** Replace every {variable} in the text from the variables; a variable with no value is refused. */
export function fill(text: string, variables: Record<string, string>): string {
  return text.replace(/\{(\w+)\}/g, (_match, name: string) => {
    if (!(name in variables)) throw new Error(`missing variable: ${name}`);
    return String(variables[name]);
  });
}

/** A static module whose text holds a {variable} is refused: a value that changes in the prefix breaks the cache. */
export function checkStatic(stat: any[]): void {
  for (const m of stat) {
    if (/\{\w+\}/.test(m.text)) throw new Error(`static module ${m.name} holds a variable, which would break the cache`);
  }
}

/** The index of the dynamic module to drop first: the lowest priority, and of a tie the later one. */
export function pickVictim(kept: Kept[]): number {
  let victim = 0;
  kept.forEach((k, i) => {
    if (k.priority < kept[victim].priority || (k.priority === kept[victim].priority && i > victim)) victim = i;
  });
  return victim;
}

/** Drop dynamic modules until prefix + the kept tokens fit the budget; returns the dropped names in order. */
export function fitBudget(prefix: number, kept: Kept[], budget: number): string[] {
  const dropped: string[] = [];
  while (prefix + kept.reduce((sum, k) => sum + tokens(k.text), 0) > budget) {
    if (kept.length === 0) throw new Error("over budget: the static modules alone exceed it");
    dropped.push(kept.splice(pickVictim(kept), 1)[0].name);
  }
  return dropped;
}

/** The index of the last static block when there is one and the prefix has at least MIN_CACHEABLE tokens; otherwise null. */
export function breakpointOf(staticCount: number, prefix: number): number | null {
  return staticCount > 0 && prefix >= MIN_CACHEABLE ? staticCount - 1 : null;
}

export function assemble(modules: any[], variables: Record<string, string>, budget: number): any {
  log.debug("assemble input", modules);
  const stat = modules.filter((m) => m.static);
  const dynamic = modules.filter((m) => !m.static);
  checkStatic(stat);
  const kept: Kept[] = dynamic.map((m) => ({ name: m.name, text: fill(m.text, variables), priority: m.priority ?? 0 }));
  const blocks = stat.map((m) => ({ name: m.name, text: m.text }));
  const prefix = blocks.reduce((sum, b) => sum + tokens(b.text), 0);
  const dropped = fitBudget(prefix, kept, budget);
  const used = prefix + kept.reduce((sum, k) => sum + tokens(k.text), 0);
  blocks.push(...kept.map((k) => ({ name: k.name, text: k.text })));
  return { blocks, tokens: used, dropped, breakpoint: breakpointOf(stat.length, prefix) };
}

/** The name of the cheapest model that meets the tier and the latency; ties go to the lower name; null when none fits. */
export function chooseModel(workload: any, models: any[]): string | null {
  const fit = models.filter((m) => m.tier >= workload.tier && m.latency_ms <= workload.max_latency_ms);
  if (fit.length === 0) return null;
  return [...fit].sort((a, b) => a.price_out - b.price_out || (a.name < b.name ? -1 : a.name > b.name ? 1 : 0))[0].name;
}

/** The tokens of the cached prefix two assembled prompts share, or 0. */
export function reusablePrefix(a: any, b: any): number {
  const ia = a.breakpoint;
  const ib = b.breakpoint;
  if (ia === null || ib === null || ia !== ib) return 0;
  const same = a.blocks.slice(0, ia + 1).every((block: any, i: number) => block.name === b.blocks[i].name && block.text === b.blocks[i].text);
  return same ? a.blocks.slice(0, ia + 1).reduce((sum: number, block: any) => sum + tokens(block.text), 0) : 0;
}
