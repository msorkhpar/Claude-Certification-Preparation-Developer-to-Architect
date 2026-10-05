// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { audit } from "./audit.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const step = (tool: string, ok = true, right?: string) => ({ tool, ok, ...(right ? { right_tool: right } : {}) });
const session = (steps: any[], outcome = "resolved", needs_human = false, refund = 0) =>
  ({ id: "s", steps, outcome, needs_human, refund_cents: refund, limit_cents: 10000 });

// Sessions of a support agent: a clean one, one that skipped the customer check, and one escalated without need.
const clean = [step("get_customer"), step("lookup_order"), step("process_refund")];
const sessions = [
  session(clean, "resolved", false, 2000),
  session([step("lookup_order"), step("get_customer"), step("process_refund")]),
  session([step("get_customer")], "escalated"),
];
const report = audit(sessions);
for (const [key, value] of Object.entries(report)) console.log(`${key}: ${value}`);
