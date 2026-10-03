import { test } from "node:test";
import assert from "node:assert/strict";
import { readableOrder, runGuard, tier } from "./hookGates.ts";

test("each limit belongs to the lower tier", () => {
  assert.deepEqual([200, 200.01, 500, 500.01].map((a) => tier(a)[0]), ["allow", "ask", "ask", "deny"]);
});

test("an amount that cannot be read is denied and not allowed", () => {
  assert.deepEqual([undefined, 0, -1, "50", true].map((a) => tier(a)[0]), ["deny", "deny", "deny", "deny", "deny"]);
});

test("the order gets a date and a status word", async () => {
  const out = await readableOrder({ tool_name: "get_order", tool_response: JSON.stringify({ created: 1700000000, status: 2 }) });
  assert.deepEqual(JSON.parse(out.hookSpecificOutput.updatedToolOutput), { created: "2023-11-14", status: "declined" });
});

test("the command hook blocks with exit two and passes other commands", () => {
  assert.deepEqual(runGuard(JSON.stringify({ tool_name: "Bash", tool_input: { command: "git push" } })), [2, "guard: nothing is pushed from an agent session"]);
  assert.deepEqual(runGuard(JSON.stringify({ tool_name: "Bash", tool_input: { command: "ls" } })), [0, ""]);
  assert.equal(runGuard("{nope")[0], 2);
});
