import { test } from "node:test";
import assert from "node:assert/strict";
import { BAD, GOOD, lint } from "./workflowLint.ts";
import { parseYaml } from "./miniyaml.ts";

const rules = (text: string) => lint(text).map(([, rule]) => rule).sort();

test("the yaml reader handles the workflow shapes", () => {
  const wf = parseYaml(GOOD) as any;
  assert.deepEqual(wf.on.pull_request.types, ["opened", "synchronize"]);
  assert.equal(wf.jobs.review.steps[1].with.claude_args, "--max-turns 8");
  assert.equal(wf.jobs.review["timeout-minutes"], 15);
  assert.equal(wf.concurrency["cancel-in-progress"], true);
});

test("the good workflow has no findings", () => assert.deepEqual(lint(GOOD), []));

test("the bad workflow is flagged for every documented risk", () => {
  assert.deepEqual(rules(BAD), ["action-version", "literal-key", "literal-key", "no-checkout", "no-concurrency", "no-max-turns", "no-timeout", "review-writes", "unguarded-trigger"]);
});

test("each rule is independent", () => {
  assert.deepEqual(rules(GOOD.replace("      - uses: actions/checkout@v6\n        with:\n          fetch-depth: 1\n", "")), ["no-checkout"]);
  assert.deepEqual(rules(GOOD.replace("    timeout-minutes: 15\n", "")), ["no-timeout"]);
  assert.deepEqual(rules(GOOD.replace("          claude_args: --max-turns 8\n", "")), ["no-max-turns"]);
  assert.deepEqual(rules(GOOD.replace("    permissions:\n      contents: read\n      pull-requests: write\n      id-token: write\n", "")), ["no-permissions"]);
  assert.deepEqual(rules(GOOD.replace("contents: read", "contents: write")), ["review-writes"]);
  assert.deepEqual(rules(GOOD.replace("${{ secrets.ANTHROPIC_API_KEY }}", "abc")), ["literal-key"]);
});
