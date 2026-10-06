import { test } from "node:test";
import assert from "node:assert/strict";
import { POLICY, cite, citedBlocks, clearToolResults, conversation, footnotes, tokens, verify } from "./contextTrimming.ts";

test("clearing keeps the calls and shrinks the conversation", () => {
  const before = conversation();
  const after = clearToolResults(before, 2);
  assert.ok(tokens(after) < tokens(before) * 0.6);
  assert.deepEqual(after.filter((m) => m.role === "assistant"), before.filter((m) => m.role === "assistant"));
  assert.equal(after.flatMap((m) => (Array.isArray(m.content) ? m.content : [])).filter((b) => b.type === "tool_result" && b.content === "[cleared]").length, 3);
  assert.deepEqual(before, conversation());
});

test("a citation that matches its document passes and a changed one is caught", () => {
  const blocks: any[] = [{ type: "text", text: "x", citations: [cite(0, 19)] }];
  assert.deepEqual(verify(blocks, [POLICY]), []);
  blocks[0].citations[0].cited_text = "The grass is red.";
  assert.deepEqual(verify(blocks, [POLICY]), [{ block: 0, citation: 0, problem: "text_mismatch" }]);
});

test("a source cited twice gets one number", () => {
  const text = footnotes(citedBlocks(), ["Policy"]);
  assert.ok(text.split("[1]").length - 1 === 3 && text.split("[2]").length - 1 === 2 && text.endsWith('[2] Policy: "Water is essential for life."'));
});
