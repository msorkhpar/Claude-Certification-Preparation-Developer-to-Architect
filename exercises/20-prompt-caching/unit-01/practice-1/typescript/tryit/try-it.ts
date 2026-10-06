// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { PlanError, planRequest } from "./cacheplan.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A request whose blocks arrive in the wrong order: the volatile date sits first, in the system prompt.
const blocks = [
  { id: "date", section: "system" as const, tokens: 20, volatile: true },
  { id: "tools", section: "tools" as const, tokens: 2000 },
  { id: "rules", section: "system" as const, tokens: 3000, breakpoint: true },
  { id: "manual", section: "messages" as const, tokens: 6000, breakpoint: true },
  { id: "question", section: "messages" as const, tokens: 40 },
];
try {
  const plan = planRequest(blocks, 1024) ?? [];
  console.log("order:", plan.map((p) => p.id).join(", "));
  console.log("cache per block:", plan.map((p) => `${p.id}=${p.cache}`).join(", "));
  console.log("breakpoints at:", plan.filter((p) => p.cache).map((p) => p.id).join(", "));
} catch (err) {
  if (err instanceof PlanError) console.log("plan error:", err.message);
  else throw err;
}
