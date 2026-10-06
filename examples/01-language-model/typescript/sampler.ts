// A toy next-token sampler. It is not Claude: it only shows what temperature does.
import { logger } from "./logger.ts";
const log = logger("sampler");

export const TOKENS = ["blue", " clear", " falling", "green"];
export const LOGITS = [4.0, 2.5, 1.0, -1.0];

export function softmax(logits: number[], temperature: number): number[] {
  const scaled = logits.map((x) => x / temperature);
  const top = Math.max(...scaled);
  const exps = scaled.map((x) => Math.exp(x - top));
  const total = exps.reduce((a, b) => a + b, 0);
  return exps.map((e) => e / total);
}

// A tiny seeded random generator, the same in every language of this course.
export class Lcg {
  state: number;
  constructor(seed: number) {
    this.state = seed >>> 0;
  }
  next(): number {
    this.state = (Math.imul(this.state, 1664525) + 1013904223) >>> 0;
    return this.state / 2 ** 32;
  }
}

export function sample(probs: number[], rng: Lcg): number {
  const u = rng.next();
  let acc = 0;
  for (let i = 0; i < probs.length; i++) {
    acc += probs[i];
    if (u < acc) return i;
  }
  return probs.length - 1;
}

export function greedy(probs: number[]): number {
  return probs.indexOf(Math.max(...probs));
}

export function main(): void {
  for (const t of [0.5, 1.0, 2.0]) {
    const probs = softmax(LOGITS, t);
    const cells = TOKENS.map((tok, i) => `${tok.trim()}=${probs[i].toFixed(3)}`);
    console.log(`T=${t.toFixed(1)}: ` + cells.join("  "));
  }
  console.log("greedy:", TOKENS[greedy(softmax(LOGITS, 1.0))]);
  for (const t of [0.2, 1.0, 2.0]) {
    const probs = softmax(LOGITS, t);
    const rng = new Lcg(7);
    const picks = Array.from({ length: 10 }, () => TOKENS[sample(probs, rng)].trim());
    console.log(`T=${t.toFixed(1)} ten draws:`, picks.join(" "));
  }
}

if (process.argv[1] && import.meta.url.endsWith(process.argv[1].split("/").pop()!)) main();
