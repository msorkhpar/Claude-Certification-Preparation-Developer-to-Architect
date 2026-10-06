/** A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md. */
import { logger } from "../logger.ts";
const log = logger("prompt_plan");

export const MIN_CACHEABLE = 512; // tokens: a shorter prefix cannot be cached

export const tokens = (text: string): number => Math.ceil(text.length / 4); // one token per four characters, rounded up

type Kept = { name: string; text: string; priority: number };

/**
 * TODO 1 of 7 (unlocks e2): fill the variables of a dynamic module's text.
 * Receives the text and an object of variables. Returns the text with every `{name}` replaced by that variable's value (extra variables are ignored).
 * Throws `Error("missing variable: <name>")` when a variable has no value.
 * Example: fill("Q: {q}", { q: "hello" }) -> "Q: hello"
 */
export function fill(text: string, variables: Record<string, string>): string {
  return text;
}

/**
 * TODO 2 of 7 (unlocks e1): refuse a variable in a static module.
 * Receives the list of static modules. Throws an `Error` with a message that says `static` (for example `static module <name> holds a variable, which
 * would break the cache`) when any module's text holds a `{variable}`; otherwise returns nothing.
 * Example: a static module with the text "policy for {customer}" -> Error
 */
export function checkStatic(stat: any[]): void {}

/**
 * TODO 3 of 7 (unlocks e3): which dynamic module is dropped first?
 * Receives the list of kept dynamic modules, each `{ name, text, priority }`. Returns the index of the one with the lowest priority; of two with the same
 * priority, the later one (the higher index).
 * Example: priorities [1, 9, 1] -> 2
 */
export function pickVictim(kept: Kept[]): number {
  return 0;
}

/**
 * TODO 4 of 7 (unlocks e3 and e4): drop dynamic modules until the prompt fits the budget.
 * Receives the tokens of the static prefix, the array `kept` of dynamic modules (change it in place) and the budget. While `prefix` plus the tokens of the
 * kept modules exceeds the budget, remove the module `pickVictim` chooses and note its name. Returns the dropped names in the order they were dropped.
 * When nothing is left to drop and the budget is still exceeded, throws `Error("over budget: ...")`: static modules are never dropped.
 * Example: kept priorities [1, 1] and a budget that fits one of them -> the later name is dropped
 */
export function fitBudget(prefix: number, kept: Kept[], budget: number): string[] {
  return [];
}

/**
 * TODO 5 of 7 (unlocks m1 and e5): where the cache breakpoint goes.
 * Receives the number of static blocks and the tokens of the static prefix. Returns the index of the last static block when there is at least one static
 * block and the prefix has at least `MIN_CACHEABLE` tokens; otherwise `null`.
 * Example: breakpointOf(2, 512) -> 1, breakpointOf(2, 511) -> null, breakpointOf(0, 900) -> null
 */
export function breakpointOf(staticCount: number, prefix: number): number | null {
  return null;
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

/**
 * TODO 6 of 7 (unlocks e6): choose the model for a workload.
 * Receives the workload `{ tier, max_latency_ms }` and a list of models `{ name, tier, latency_ms, price_out }`. Returns the name of the model with the lowest
 * `price_out` among those whose `tier` is at least the workload's and whose `latency_ms` is within the limit; equal prices go to the lower name; `null` when no
 * model fits. Example: two models at the same price named "mid" and "mid2", both fitting -> "mid"
 */
export function chooseModel(workload: any, models: any[]): string | null {
  return null;
}

/**
 * TODO 7 of 7 (unlocks e7): how many tokens of cached prefix can be reused?
 * Receives two assembled prompts (each with `blocks` and `breakpoint`). When both have a breakpoint, the breakpoints are equal and every block up to and
 * including it is identical (name and text) in both, returns the tokens of those blocks; otherwise returns 0.
 * Example: two prompts that differ only in their dynamic blocks -> the tokens of the static prefix; one edited static block -> 0
 */
export function reusablePrefix(a: any, b: any): number {
  return 0;
}
