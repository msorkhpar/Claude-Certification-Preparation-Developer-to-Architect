import { test } from "node:test";
import assert from "node:assert/strict";
import { AGENTS, BRIEF, describe } from "./subagentRun.ts";

const assistant = (block: any, parent: string | null = null) => ({ type: "assistant", parent_tool_use_id: parent, message: { content: [block] } });

test("a spawn is described with the length of its brief", () => {
  const message = assistant({ type: "tool_use", id: "t1", name: "Agent", input: { subagent_type: "reviewer", description: "d", prompt: BRIEF } });
  assert.deepEqual(describe(message), ["spawn: Agent -> reviewer, brief of 6 lines"]);
});

test("a message from inside a subagent is marked with its parent", () => {
  const block = { type: "tool_use", id: "r", name: "Read", input: { file_path: "a" } };
  assert.deepEqual(describe(assistant(block, "t1")), ['  inside t1: Read {"file_path":"a"}']);
  assert.deepEqual(describe(assistant(block)), ["coordinator calls Read"]);
});

test("only the report of the subagent itself reaches the coordinator lines", () => {
  const user = (id: string, content: string, parent: string | null) => ({ type: "user", parent_tool_use_id: parent, message: { content: [{ type: "tool_result", tool_use_id: id, content }] } });
  assert.deepEqual(describe(user("r", "code", "t1")), []);
  assert.deepEqual(describe(user("t1", "one finding", null)), ["report to the coordinator: one finding"]);
});

test("the reviewer is limited to two read tools", () => {
  assert.deepEqual(AGENTS.reviewer.tools, ["Read", "Grep"]);
  assert.ok(!AGENTS.finder.tools.includes("Agent"));
});
