// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { assemble, chooseModel } from "./promptPlan.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// Prompt modules: static ones first (they can be cached), then the changing ones by priority.
const role = { name: "role", static: true, text: "r".repeat(400) };
const policy = { name: "policy", static: true, text: "p".repeat(1648) };
const history = { name: "history", static: false, priority: 1, text: "h".repeat(200) };
const question = { name: "question", static: false, priority: 9, text: "Q: {q}" };

const prompt = assemble([question, role, history, policy], { q: "hello" }, 10_000);
console.log("block order:", prompt.blocks.map((b: any) => b.name));
console.log("cache breakpoint after block:", prompt.breakpoint, "| tokens:", prompt.tokens, "| dropped:", prompt.dropped);

// With a small budget the lowest-priority changing module is dropped.
const small = assemble([question, role, history, policy], { q: "hello" }, 520);
console.log("dropped with a budget of 520:", small.dropped);

// The cheapest model that meets the tier and the latency.
const models = [{ name: "haiku", tier: 1, latency_ms: 300, price_out: 5 }, { name: "sonnet", tier: 2, latency_ms: 900, price_out: 15 }];
console.log("model:", chooseModel({ tier: 2, max_latency_ms: 1000 }, models));
