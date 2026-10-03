import { test } from "node:test";
import assert from "node:assert/strict";
import { add, askMe, divide, noPush } from "./agentOffline.ts";

test("the tools return text and a composed error", async () => {
  assert.equal(((await add.handler({ a: 2, b: 3 }, {})) as any).content[0].text, "Sum: 5");
  const error: any = await divide.handler({ a: 1, b: 0 }, {});
  assert.ok(error.isError === true && error.content[0].text === "Cannot divide by zero");
  assert.equal(((await divide.handler({ a: 7, b: 2 }, {})) as any).content[0].text, "Quotient: 3");
});

test("the hook denies a push and leaves other commands alone", async () => {
  assert.equal((await noPush({ tool_input: { command: "git push origin main" } })).hookSpecificOutput?.permissionDecision, "deny");
  assert.deepEqual(await noPush({ tool_input: { command: "git status" } }), {});
});

test("the permission callback allows listing and denies the rest", async () => {
  assert.equal((await askMe("Bash", { command: "ls -la" })).behavior, "allow");
  assert.equal((await askMe("mcp__calc__add", { a: 1, b: 2 })).behavior, "allow");
  assert.deepEqual(await askMe("Bash", { command: "curl example.invalid" }), { behavior: "deny", message: "Bash is not allowed here" });
});
