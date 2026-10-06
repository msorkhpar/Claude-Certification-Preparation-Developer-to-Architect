import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const solution = await import(pathToFileURL(resolve(dir, "recovery.ts")).href);
const got = (fn: (...args: any[]) => any) => (...args: any[]) => {
  const value = fn(...args);
  assert.ok(value !== null && value !== undefined, `${fn.name} returned nothing`);
  return value;
};
const [addFinding, renderScratchpad, buildManifest, resumePlan, resumePrompt, compactCommand] = [solution.addFinding, solution.renderScratchpad, solution.buildManifest, solution.resumePlan, solution.resumePrompt, solution.compactCommand].map(got);

const agent = (name: string, status = "done", state_file?: string) => ({ name, state_file: state_file ?? `state/${name}.md`, status });

test("m1 a finding is recorded once per area and fact in first seen order", () => {
  let found = addFinding([], "auth", "tokens are signed in TokenSigner", "auth/TokenSigner.java:12");
  found = addFinding(found, "billing", "invoices use cents", "billing/Money.java:5");
  const again = addFinding(found, "auth", "tokens are signed in TokenSigner", "auth/Other.java:99");
  assert.deepEqual(again, found);
  assert.deepEqual(again.map((f: any) => f.area), ["auth", "billing"]);
  assert.equal(addFinding(found, "billing", "a different fact", "x:1").length, 3);
  assert.ok(addFinding(found, "auth", "invoices use cents", "x:1").length === 3 && found.length === 2);
});

test("e1 the scratchpad groups findings under their area", () => {
  let found = addFinding([], "auth", "f1", "a:1");
  found = addFinding(found, "billing", "f2", "b:2");
  found = addFinding(found, "auth", "f3", "a:3");
  assert.equal(renderScratchpad(found), "## auth\n- f1 (a:1)\n- f3 (a:3)\n\n## billing\n- f2 (b:2)");
  assert.equal(renderScratchpad([]), "");
});

test("e2 the manifest lists every agent with its state file and status and refuses bad input", () => {
  const manifest = buildManifest([agent("search", "running"), agent("auth"), agent("billing", "failed")]);
  assert.deepEqual(manifest, { version: 1, agents: [
    { name: "auth", state_file: "state/auth.md", status: "done" },
    { name: "billing", state_file: "state/billing.md", status: "failed" },
    { name: "search", state_file: "state/search.md", status: "running" }] });
  assert.throws(() => buildManifest([agent("auth"), agent("auth", "running")]));
  assert.throws(() => buildManifest([agent("auth", "paused")]));
});

test("e3 a finished agent with its state file is reused and not run again", () => {
  assert.deepEqual(resumePlan(buildManifest([agent("auth", "done")]), new Set(["state/auth.md"])), [["auth", "reuse"]]);
});

test("e4 a running or failed agent with a state file is resumed from it", () => {
  const manifest = buildManifest([agent("billing", "failed"), agent("search", "running")]);
  assert.deepEqual(resumePlan(manifest, new Set(["state/billing.md", "state/search.md"])), [["billing", "resume"], ["search", "resume"]]);
});

test("e5 an agent whose state file is missing is restarted from scratch", () => {
  const manifest = buildManifest([agent("auth", "done"), agent("search", "running")]);
  assert.deepEqual(resumePlan(manifest, new Set(["state/auth.md"])), [["auth", "reuse"], ["search", "restart"]]);
  assert.deepEqual(resumePlan(manifest, new Set()), [["auth", "restart"], ["search", "restart"]]);
});

test("e6 the resume prompt carries the task and the state lines and nothing else", () => {
  assert.equal(resumePrompt("Map the search module.", ["indexer.py done", "ranker.py not started"]),
    "Map the search module.\n\nState from the last run:\n- indexer.py done\n- ranker.py not started\nContinue from the first unfinished step.");
  assert.equal(resumePrompt("Map the search module.", []), "Map the search module.");
});

test("e7 the compact command names what to keep", () => {
  assert.equal(compactCommand(["the list of files read", "open questions"]), "/compact Focus on the list of files read, open questions");
  assert.equal(compactCommand([]), "/compact");
});
