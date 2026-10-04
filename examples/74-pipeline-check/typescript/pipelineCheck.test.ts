import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { HERE, audit, load, words } from "./pipelineCheck.ts";

function pipeline(...jobs: object[]): string {
  const root = mkdtempSync(join(tmpdir(), "pipeline-"));
  mkdirSync(join(root, "ci"));
  writeFileSync(join(root, "ci/pipeline.json"), JSON.stringify({ jobs }));
  return root;
}

const job = (changes: object = {}) => ({ name: "j", kind: "report", audience: "scheduled", api: "batch", passes: [], session: "fresh", context: [], tools: [], command: "python ci/run.py", ...changes });

test("the flawed pipeline has every finding and the fixed one has none", () => {
  assert.deepEqual(audit(join(HERE, "project-before")), ["blocking-batch: pre-merge-review", "single-pass-review: pre-merge-review", "shared-session: pre-merge-review",
    "no-prior-findings: pre-merge-review", "writes: pre-merge-review", "batchable: debt-report", "no-print-flag: test-generation", "no-existing-tests: test-generation"]);
  assert.deepEqual(audit(join(HERE, "project-after")), []);
});

test("who waits decides between batch and real time", () => {
  assert.deepEqual(audit(pipeline(job({ audience: "waiting", api: "batch" }))), ["blocking-batch: j"]);
  assert.deepEqual(audit(pipeline(job({ audience: "scheduled", api: "realtime", command: "claude -p x" }))), ["batchable: j"]);
  assert.deepEqual(audit(pipeline(job({ audience: "waiting", api: "realtime", command: "claude -p x" }))), []);
});

test("a command is headless with either spelling and a script needs no flag", () => {
  assert.deepEqual(audit(pipeline(job({ api: "realtime", audience: "waiting", command: 'claude "x"' }))), ["no-print-flag: j"]);
  assert.deepEqual(audit(pipeline(job({ api: "realtime", audience: "waiting", command: 'claude --print "x"' }))), []);
  assert.deepEqual(audit(pipeline(job())), []);
  assert.deepEqual(words('claude -p "a b" --x'), ["claude", "-p", "a b", "--x"]);
});

test("a review needs a pass per file and then an integration pass in that order", () => {
  const review = { kind: "review", audience: "waiting", api: "realtime", command: "claude -p x", context: ["prior_findings"] };
  assert.deepEqual(audit(pipeline(job({ passes: ["per-file", "integration"], ...review }))), []);
  assert.deepEqual(audit(pipeline(job({ passes: ["integration", "per-file"], ...review }))), ["single-pass-review: j"]);
  assert.deepEqual(audit(pipeline(job({ passes: ["per-file"], ...review }))), ["single-pass-review: j"]);
});

test("only a review is held to read only tools", () => {
  assert.deepEqual(audit(pipeline(job({ kind: "testgen", audience: "waiting", api: "realtime", command: "claude -p x", context: ["existing_tests"], tools: ["Edit"] }))), []);
  assert.deepEqual(load(join(HERE, "project-after"))[2].tools, ["Read", "Glob", "Edit"]);
});
