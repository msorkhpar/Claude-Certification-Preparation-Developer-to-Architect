// A capacity and cost model for one workload: the limits it needs, the tier that gives them, and the monthly bill.
//
// The Claude documentation on rate limits (read on 2026-10-04) says that "for most Claude models, only uncached input tokens count toward
// your ITPM rate limits": `input_tokens` and `cache_creation_input_tokens` count, `cache_read_input_tokens` do not. The limits of the Start,
// Build and Scale tiers below are the documented figures for Claude Sonnet 5.5, and the prices are the documented Sonnet 5.5 prices on the
// Claude API (input 2, output 10, 5-minute cache write 2.50, cache read 0.20 dollars per million tokens, batch at half price). Prices and
// limits change; re-read them before you plan. Money is kept in whole cents so that every language prints the same figures.

import { logger } from "./logger.ts";
const log = logger("capacity_model");

export type Workload = { rpm: number; input: number; cache_write: number; cache_read: number; output: number }; // tokens per request, requests per minute
export type Need = { rpm: number; itpm: number; otpm: number };

export const TIERS: Array<[string, number, number, number]> = [["Start", 1000, 2_000_000, 400_000], ["Build", 5000, 5_000_000, 1_000_000], ["Scale", 10_000, 10_000_000, 2_000_000]]; // name, RPM, ITPM, OTPM
export const CENTS_PER_MTOK = { input: 200, cache_write: 250, cache_read: 20, output: 1000 };

export const CACHED: Workload = { rpm: 800, input: 1500, cache_write: 200, cache_read: 6000, output: 400 };
export const UNCACHED: Workload = { rpm: 800, input: 7700, cache_write: 0, cache_read: 0, output: 400 }; // the same prompts with no caching
export const REQUESTS_PER_MONTH = 2_000_000;

/** RPM, ITPM and OTPM to ask for. Cache reads do not count toward ITPM; every figure is rounded up after the headroom. */
export function requiredCapacity(w: Workload, headroomPercent: number): Need {
  const up = (x: number) => Math.ceil((x * (100 + headroomPercent)) / 100);
  return { rpm: up(w.rpm), itpm: up(w.rpm * (w.input + w.cache_write)), otpm: up(w.rpm * w.output) };
}

export function smallestTier(need: Need, tiers: Array<[string, number, number, number]>): string {
  for (const [name, rpm, itpm, otpm] of tiers) if (need.rpm <= rpm && need.itpm <= itpm && need.otpm <= otpm) return name;
  return "Custom";
}

/** Cents per month. The share of requests sent through the Batch API is billed at half price in every category. */
export function monthlyCents(w: Workload, requests: number, batchPercent: number): number {
  log.debug("monthlyCents input", w);
  const perRequest = w.input * CENTS_PER_MTOK.input + w.cache_write * CENTS_PER_MTOK.cache_write + w.cache_read * CENTS_PER_MTOK.cache_read + w.output * CENTS_PER_MTOK.output;
  return Math.floor((requests * perRequest * (200 - batchPercent)) / (200 * 1_000_000));
}

export function dollars(cents: number): string {
  return `$${Math.floor(cents / 100).toLocaleString("en-US")}.${String(cents % 100).padStart(2, "0")}`;
}

function main() {
  for (const [label, workload] of [["with caching", CACHED], ["without caching", UNCACHED]] as Array<[string, Workload]>) {
    const need = requiredCapacity(workload, 30);
    console.log(`${label}: need ${need.rpm} rpm, ${need.itpm} itpm, ${need.otpm} otpm -> tier ${smallestTier(need, TIERS)}`);
  }
  console.log("monthly bill with caching:", dollars(monthlyCents(CACHED, REQUESTS_PER_MONTH, 0)));
  console.log("monthly bill without caching:", dollars(monthlyCents(UNCACHED, REQUESTS_PER_MONTH, 0)));
  console.log("monthly bill with caching and 30 percent batch:", dollars(monthlyCents(CACHED, REQUESTS_PER_MONTH, 30)));
}

if (import.meta.main) main();
