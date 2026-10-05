// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { preRefund } from "./hooks.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// Run the refund gate the way the Agent SDK does: with the event of one tool call about to run.
async function check(amount: number) {
  const event = { hook_event_name: "PreToolUse", tool_name: "process_refund", tool_input: { amount } };
  const out = (await preRefund(event, "t1", undefined)) ?? {};
  return out.hookSpecificOutput ?? {};
}

// A small refund goes through, a middle one asks a person, a large one is denied.
for (const amount of [50, 350, 900]) {
  const verdict = await check(amount);
  console.log(`refund of ${amount}:`, verdict.permissionDecision, "-", verdict.permissionDecisionReason ?? "");
}
