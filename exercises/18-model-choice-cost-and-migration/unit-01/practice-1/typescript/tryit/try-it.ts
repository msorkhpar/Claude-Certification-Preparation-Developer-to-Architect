// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { NoModelError, requestCost, route } from "./router.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// Two of the course models, with prices in dollars per million tokens, as the tests use them.
const HAIKU = { id: "claude-haiku-4-5-20251001", tier: 1, context: 200_000, max_output: 64_000, input: 1, output: 5, cache_read_multiplier: 0.1 };
const SONNET = { id: "claude-sonnet-5-5", tier: 2, context: 1_000_000, max_output: 128_000, input: 2, output: 10, cache_read_multiplier: 0.1 };
const catalog = [HAIKU, SONNET];
const usage = { input_tokens: 1200, output_tokens: 300 };

console.log("sonnet cost (micro-dollars):", requestCost(SONNET, usage));
console.log("haiku cost (micro-dollars):", requestCost(HAIKU, usage));
try {
  console.log("tier 1 task goes to:", route(catalog, { min_tier: 1, usage }));
  console.log("tier 2 task goes to:", route(catalog, { min_tier: 2, usage }));
} catch (err) {
  if (err instanceof NoModelError) console.log("no model:", err.message);
  else throw err;
}
