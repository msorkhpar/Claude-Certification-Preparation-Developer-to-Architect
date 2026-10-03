import { test } from "node:test";
import assert from "node:assert/strict";
import { decide, flags, notice } from "./sessionState.ts";

const SAVED = { "a.py": "d1", "b.py": "d2", "c.py": "d3", "d.py": "d4" };

test("an unchanged session is resumed and a changed one gets a notice", () => {
  assert.deepEqual(decide(SAVED, SAVED, 1), ["resume", []]);
  assert.deepEqual(decide(SAVED, { ...SAVED, "b.py": "x" }, 1), ["resume with a notice", ["b.py"]]);
  const { "c.py": _gone, ...without } = SAVED;
  assert.deepEqual(decide(SAVED, without, 1), ["resume with a notice", []]);
});

test("most files changed or a week idle starts fresh", () => {
  assert.equal(decide(SAVED, { "a.py": "x", "b.py": "x", "c.py": "x", "d.py": "d4" }, 1)[0], "start fresh with a summary");
  assert.equal(decide(SAVED, { ...SAVED, "a.py": "x", "b.py": "x" }, 1)[0], "resume with a notice");
  assert.equal(decide(SAVED, SAVED, 8)[0], "start fresh with a summary");
  assert.equal(decide(SAVED, SAVED, 7)[0], "resume");
});

test("the notice names the changed files and asks for a new read", () => {
  const text = notice(["a.py", "b.py"]);
  assert.ok(text.split("\n")[1] === "- changed: a.py, b.py" && text.includes("Re-read these files"));
});

test("flags lists resume with its id, fork and continue in order", () => {
  assert.equal(flags(["--output-format", "stream-json"]), "none");
  assert.equal(flags(["--resume", "s1", "--fork-session", "--verbose"]), "--resume s1, --fork-session");
  assert.equal(flags(["--continue"]), "--continue");
  assert.equal(flags(["--resume=s1", "--fork-session"]), "--resume s1, --fork-session");
});
