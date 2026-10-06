import { test } from "node:test";
import assert from "node:assert/strict";
import { finish, injectedState, readManifest, recoveryPlan, start, tokens } from "./stateManifest.ts";

test("the manifest is written at the start and updated when the agent finishes", () => {
  const fs: Record<string, string> = {};
  start(fs, "auth");
  assert.deepEqual(readManifest(fs), { auth: ["running", "state/auth.md"] });
  assert.ok(!("state/auth.md" in fs));
  finish(fs, "auth", [["a fact", "x.py:1"]]);
  assert.deepEqual(readManifest(fs), { auth: ["done", "state/auth.md"] });
  assert.equal(fs["state/auth.md"], "- a fact (x.py:1)");
});

test("a crash before the state file is written means a restart", () => {
  const fs: Record<string, string> = {};
  start(fs, "auth");
  finish(fs, "auth", [["a fact", "x.py:1"]]);
  start(fs, "search");
  assert.deepEqual(recoveryPlan(fs, ["auth", "search", "never_started"]), [["auth", "reuse"], ["search", "restart"], ["never_started", "restart"]]);
});

test("a state file without a finished manifest entry is resumed", () => {
  const fs = { "state/manifest.txt": "billing|running|state/billing.md", "state/billing.md": "- half done (b.py:2)" };
  assert.deepEqual(recoveryPlan(fs, ["billing"]), [["billing", "resume"]]);
});

test("the injected state holds only what need not run again", () => {
  const fs: Record<string, string> = {};
  start(fs, "auth");
  finish(fs, "auth", [["a fact", "x.py:1"]]);
  start(fs, "search");
  assert.equal(injectedState(fs, recoveryPlan(fs, ["auth", "search"])), "auth:\n- a fact (x.py:1)");
});

test("tokens round up by four characters", () => {
  assert.deepEqual([tokens(0), tokens(4), tokens(5), tokens("abcde")], [0, 1, 2, 2]);
});
