// A prompt assembled from modules in cache-friendly order, with a token budget and the cache breakpoint.
//
// The Claude documentation on prompt caching (read on 2026-10-04) says cache prefixes are created "in the following order: tools, system,
// then messages" and that a prompt shorter than the model's minimum "cannot be cached, even if marked with cache_control" (512 tokens for
// Claude Sonnet 5.5). The prompting guide says to put long documents "near the top of your prompt, above your query". This file orders
// the modules of a request that way, estimates tokens as one per four characters (a rough rule, not the model's tokenizer), marks the
// breakpoint after the last static module and shows which edits keep the cached prefix and which break it. No model is called.
import { logger } from "./logger.ts";
const log = logger("prompt_budget");

export type Module = { name: string; static: boolean; text: string };
export type Prompt = { blocks: Module[]; tokens: number; prefix_tokens: number; breakpoint: number | null };

export const MIN_CACHEABLE = 512; // tokens, Claude Sonnet 5.5

export const POLICY = "Refunds above 200 are approved by a supervisor. Gift cards are never refunded in cash. ".repeat(26);
export const MODULES: Module[] = [
  { name: "role", static: true, text: "You are the support assistant of Northwind Outfitters. Answer from the policy only." },
  { name: "policy", static: true, text: POLICY },
  { name: "customer", static: false, text: "Customer: {customer}. Tier: {tier}." },
  { name: "question", static: false, text: "Question: {question}" },
];

export const tokens = (text: string): number => Math.ceil(text.length / 4); // characters over four, rounded up

const fill = (text: string, variables: Record<string, string>) => text.replace(/\{(\w+)\}/g, (_m, name) => variables[name]);

/** Static modules first, in the order given, then the dynamic ones with their variables filled in. */
export function assemble(modules: Module[], variables: Record<string, string>): Prompt {
  log.debug("assemble input", modules);
  const ordered = [...modules.filter((m) => m.static), ...modules.filter((m) => !m.static)];
  const blocks = ordered.map((m) => ({ ...m, text: m.static ? m.text : fill(m.text, variables) }));
  const prefix = blocks.filter((b) => b.static).reduce((sum, b) => sum + tokens(b.text), 0);
  const lastStatic = blocks.reduce((last, b, i) => (b.static ? i : last), -1);
  return { blocks, tokens: blocks.reduce((sum, b) => sum + tokens(b.text), 0), prefix_tokens: prefix, breakpoint: lastStatic >= 0 && prefix >= MIN_CACHEABLE ? lastStatic : null };
}

export function cachedPrefix(prompt: Prompt): string {
  return prompt.breakpoint === null ? "" : prompt.blocks.slice(0, prompt.breakpoint + 1).map((b) => b.text).join("");
}

const flag = (b: boolean) => (b ? "True" : "False");

function main() {
  const ana = { customer: "Ana", tier: "gold", question: "Can I return a gift card?" };
  const first = assemble(MODULES, ana);
  console.log("order:", first.blocks.map((b) => b.name).join(" > "));
  console.log(`tokens: ${first.tokens} in all, ${first.prefix_tokens} in the static prefix, minimum ${MIN_CACHEABLE}`);
  console.log("breakpoint after:", first.blocks[first.breakpoint!].name);
  const second = assemble(MODULES, { customer: "Ben", tier: "basic", question: "Where is my parcel?" });
  console.log("next request, other customer: prefix identical:", flag(cachedPrefix(second) === cachedPrefix(first)));
  const edited = MODULES.map((m) => (m.name === "policy" ? { ...m, text: m.text.replaceAll("200", "300") } : m));
  console.log("after a policy edit: prefix identical:", flag(cachedPrefix(assemble(edited, ana)) === cachedPrefix(first)));
  const short = assemble([MODULES[0], ...MODULES.slice(2)], { customer: "Ana", tier: "gold", question: "Hi" });
  console.log("without the policy: breakpoint", short.breakpoint === null ? "None" : short.breakpoint, "because", short.prefix_tokens, "tokens is under", MIN_CACHEABLE);
}

if (import.meta.main) main();
