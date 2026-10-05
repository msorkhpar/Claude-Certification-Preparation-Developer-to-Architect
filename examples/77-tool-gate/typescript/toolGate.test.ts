import { test } from "node:test";
import assert from "node:assert/strict";
import { checkOutput, decide } from "./toolGate.ts";

test("a denied permission refuses the tool whatever else it asks for", () => {
  assert.deepEqual(decide(["read_files", "network"]), ["refused", ["network"]]);
  assert.deepEqual(decide(["run_process", "network"]), ["refused", ["run_process", "network"]]);
});

test("a write waits for a person and a read runs by itself", () => {
  assert.deepEqual(decide(["read_files", "write_files"]), ["needs_approval", ["write_files"]]);
  assert.deepEqual(decide(["read_files"]), ["auto", []]);
});

test("a result is checked for fields types and size before the agent uses it", () => {
  assert.deepEqual(checkOutput({ headline: "ok", rows: 3 }), []);
  assert.deepEqual(checkOutput({ headline: "ok" }), ["missing: rows"]);
  assert.deepEqual(checkOutput({ headline: "ok", rows: "3" }), ["type: rows"]);
  assert.deepEqual(checkOutput({ headline: "x".repeat(201), rows: 3 }), ["too large"]);
  assert.deepEqual(checkOutput({ headline: "x".repeat(200), rows: 3 }), []);
});
