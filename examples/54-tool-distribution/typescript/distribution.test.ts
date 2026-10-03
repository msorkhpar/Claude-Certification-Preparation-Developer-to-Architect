import { test } from "node:test";
import assert from "node:assert/strict";
import { ROLES, allowed, cacheCost, madeTheCall, toolsFor, turnFor } from "./distribution.ts";

test("each role gets its own tools and the irreversible tool is nobody's by default", () => {
  assert.deepEqual(toolsFor("searcher"), ["web_search", "fetch_page", "verify_fact"]);
  assert.deepEqual(toolsFor("synthesizer"), ["verify_fact", "summarize_content"]);
  assert.ok(!toolsFor("reporter").includes("send_report") && Object.keys(ROLES).every((r) => toolsFor(r).length <= 5));
});

test("a model that rejects forcing gets auto one tool and a check on the reply", () => {
  assert.deepEqual(turnFor("claude-sonnet-5-5", "a", ["a", "b"]), { tool_choice: "auto", tools: ["a"], check_reply: true });
  assert.deepEqual(turnFor("claude-opus-5", "a", ["a", "b"]), { tool_choice: "tool:a", tools: ["a", "b"], check_reply: false });
});

test("narrowing the tools costs more than changing the choice", () => {
  const base = { tool_choice: "auto", tools: ["a", "b"] };
  assert.ok(cacheCost(base, turnFor("claude-opus-5", "a", ["a", "b"])).startsWith("the cached messages"));
  assert.ok(cacheCost(base, turnFor("claude-sonnet-5-5", "a", ["a", "b"])).startsWith("everything"));
  assert.equal(cacheCost(base, { ...base }), "nothing");
});

test("a reply made the call only when the first tool use is the forced one", () => {
  assert.ok(madeTheCall([["tool_use", "a"]], "a") && !madeTheCall([["text", "x"]], "a") && !madeTheCall([["tool_use", "b"], ["tool_use", "a"]], "a"));
});

test("a refund needs approval and stays under the limit", () => {
  assert.ok(allowed("refund", 150, false).startsWith("wait") && allowed("refund", 150, true) === "run");
  assert.ok(allowed("refund", 400, true).startsWith("refused") && allowed("delete_account", 0, true).startsWith("refused"));
});
