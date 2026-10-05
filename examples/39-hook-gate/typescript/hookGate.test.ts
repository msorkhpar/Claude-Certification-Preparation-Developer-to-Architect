import { test } from "node:test";
import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { dangerous, lintAgent, lintSkill, preToolUse } from "./hookGate.ts";

const bash = (command: string) => preToolUse({ hook_event_name: "PreToolUse", tool_name: "Bash", tool_input: { command } });

test("other forms of a forbidden command are still refused", () => {
  for (const command of ["git push origin main", "git -C . push", "git -c push.default=current push", "FOO=1 git push", "ls; git push", "bash -c 'git push'", "/usr/bin/git push"]) {
    const [code, out] = bash(command);
    assert.equal(code, 0, command);
    assert.equal(JSON.parse(out).hookSpecificOutput.permissionDecision, "deny", command);
  }
  assert.equal(dangerous("git status"), null);
  assert.equal(dangerous("git log --oneline"), null);
  assert.equal(dangerous("echo push"), null);
});

test("a recursive forced delete is refused in every spelling and a plain delete is not", () => {
  for (const c of ["rm -rf build", "rm -fr build", "rm -r -f build", "/bin/rm -rf x", "sh -c 'rm -rf x'", "rm --recursive --force x"]) assert.ok(dangerous(c), c);
  for (const c of ["rm build/old.txt", "rm -r build", "rm -f build/a"]) assert.equal(dangerous(c), null, c);
});

test("a protected path is blocked with exit two and a reason on standard error", () => {
  const [code, out, err] = preToolUse({ tool_name: "Write", tool_input: { file_path: "C:\\work\\.git\\config" } });
  assert.deepEqual([code, out], [2, ""]);
  assert.match(err, /protected pattern '\.git\/'/);
  assert.deepEqual(preToolUse({ tool_name: "Edit", tool_input: { file_path: "/w/src/main.py" } }), [0, "", ""]);
  assert.deepEqual(preToolUse({ tool_name: "Read", tool_input: { file_path: "/w/.env" } }), [0, "", ""]);
});

test("the file works as a hook process", () => {
  const childEnv = { ...process.env };
  delete childEnv.NODE_TEST_CONTEXT; // the hook process is a plain program, not a test run
  const file = new URL("./hookGate.ts", import.meta.url).pathname;
  const run = spawnSync("node", [file, "--hook"], { input: JSON.stringify({ tool_name: "Bash", tool_input: { command: "git push" } }), encoding: "utf8", env: childEnv });
  assert.equal(run.status, 0);
  assert.equal(JSON.parse(run.stdout).hookSpecificOutput.permissionDecision, "deny");
  const blocked = spawnSync("node", [file, "--hook"], { input: JSON.stringify({ tool_name: "Edit", tool_input: { file_path: ".env" } }), encoding: "utf8", env: childEnv });
  assert.equal(blocked.status, 2);
  assert.match(blocked.stderr, /Blocked/);
});

test("skill and agent files are linted", () => {
  assert.equal(lintSkill("---\nname: deploy\ndescription: Deploy it\nallowed-tools: Bash\n---\nx").length, 2);
  assert.deepEqual(lintSkill("---\nname: deploy\ndescription: Deploy it\ndisable-model-invocation: true\nallowed-tools: Bash(git add *) Read\n---\nx"), []);
  assert.deepEqual(lintSkill("---\nname: x\n---\nbody"), ["description is missing: Claude uses it to decide when to load the skill"]);
  assert.deepEqual(lintAgent("---\nname: r\ndescription: d\ntools: Read, Grep\nmemory: project\n---\nx"), []);
  assert.equal(lintAgent("---\nname: r\ndescription: d\nmemory: team\npermissionMode: bypassPermissions\n---\nx").length, 3);
});
