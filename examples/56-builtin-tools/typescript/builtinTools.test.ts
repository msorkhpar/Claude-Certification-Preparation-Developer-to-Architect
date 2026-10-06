import { test } from "node:test";
import assert from "node:assert/strict";
import { coveredBy, edit, planEdit, ruleTool, toolSet } from "./builtinTools.ts";

const TEXT = "a = 1\nb = 1\n";

test("edit is an exact replacement that needs one match", () => {
  assert.deepEqual(edit(TEXT, "a = 1", "a = 2"), { ok: true, text: "a = 2\nb = 1\n", replaced: 1 });
  assert.deepEqual(edit(TEXT, "= 1", "= 2"), { ok: false, error: "old_string appears 2 times" });
  assert.deepEqual(edit(TEXT, "z", "y"), { ok: false, error: "old_string not found" });
  assert.equal(edit(TEXT, "a = .", "x").ok, false); // no regex
});

test("replace all changes every match", () => {
  assert.deepEqual(edit(TEXT, "= 1", "= 2", true), { ok: true, text: "a = 2\nb = 2\n", replaced: 2 });
});

test("the plan widens the anchor before it rewrites the file", () => {
  assert.deepEqual(planEdit(TEXT, "a = 1"), ["edit", "a = 1"]);
  assert.deepEqual(planEdit(TEXT, "= 1", false, ["b = 1"]), ["edit", "b = 1"]);
  assert.deepEqual(planEdit(TEXT, "= 1", true), ["replace_all", "= 1"]);
  assert.deepEqual(planEdit(TEXT, "= 1", false, ["= 1"]), ["read_write", null]);
  assert.deepEqual(planEdit(TEXT, "z"), ["read_again", null]);
});

test("search tools are default on windows only and named back elsewhere", () => {
  assert.deepEqual(toolSet("windows"), ["Read", "Write", "Edit", "Bash", "Grep", "Glob"]);
  assert.deepEqual(toolSet("linux"), ["Read", "Write", "Edit", "Bash"]);
  assert.deepEqual(toolSet("linux", null, ["Glob"]), ["Read", "Write", "Edit", "Bash", "Grep", "Glob"]);
  assert.deepEqual(toolSet("macos", ["Read", "Grep"]), ["Read", "Grep"]);
  assert.deepEqual(toolSet("wsl", null, [], ["Bash"]), ["Read", "Write", "Edit", "Grep", "Glob"]);
});

test("a rule covers the tools its name implies", () => {
  assert.deepEqual(coveredBy("Read(x)"), ["Read", "Grep", "Glob"]);
  assert.deepEqual(coveredBy("Edit(x)"), ["Edit", "Write"]);
  assert.deepEqual(coveredBy("Write(x)"), []);
  assert.deepEqual(coveredBy("Bash(ls *)"), ["Bash"]);
});

test("a call is checked under the rule name of its family", () => {
  assert.deepEqual(["Read", "Grep", "Glob", "Edit", "Write", "Bash"].map(ruleTool), ["Read", "Read", "Read", "Edit", "Edit", "Bash"]);
});
