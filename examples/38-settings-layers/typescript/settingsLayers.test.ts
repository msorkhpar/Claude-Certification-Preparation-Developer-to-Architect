import { test } from "node:test";
import assert from "node:assert/strict";
import { decide, effectiveSettings, loadMemory } from "./settingsLayers.ts";

const LAYERS = {
  managed: { permissions: { deny: ["Bash(curl *)"] } },
  user: { model: "sonnet", permissions: { allow: ["Bash(git status *)"] } },
  project: { model: "opus", permissions: { defaultMode: "bypassPermissions", allow: ["Bash(npm run *)", "Bash(curl *)"], ask: ["Bash(git push *)"], deny: ["Read(./.env)"] } },
  local: { model: "haiku", permissions: { allow: ["Bash(git push *)"] } },
};

test("a higher level wins a scalar key and lists combine", () => {
  const s = effectiveSettings(LAYERS);
  assert.equal(s.model, "haiku");
  assert.deepEqual(s.permissions.allow, ["Bash(git status *)", "Bash(npm run *)", "Bash(curl *)", "Bash(git push *)"]);
});

test("a repository file cannot set bypass permissions and its allow rules wait for trust", () => {
  assert.ok(!("defaultMode" in effectiveSettings(LAYERS).permissions));
  assert.equal(decide(effectiveSettings(LAYERS, false), "Bash", "npm run build"), "ask");
  assert.equal(decide(effectiveSettings(LAYERS), "Bash", "npm run build"), "allow");
});

test("deny beats ask beats allow whatever the level", () => {
  const s = effectiveSettings(LAYERS);
  assert.equal(decide(s, "Bash", "curl https://example.com"), "deny");
  assert.equal(decide(s, "Bash", "git push origin main"), "ask");
  assert.equal(decide(s, "Bash", "npm run build && git push origin main"), "ask");
  assert.equal(decide(s, "Bash", "rm -rf build"), "ask");
});

test("wildcards match the bare command but not a longer program name", () => {
  const s = { permissions: { allow: ["Bash(ls *)", "Bash(npm run build)"] } };
  assert.deepEqual(["ls", "ls -la", "lsof", "npm run build", "npm run build --watch"].map((c) => decide(s, "Bash", c)), ["allow", "allow", "ask", "allow", "ask"]);
});

test("path rules follow the rule type and a read deny also blocks edits", () => {
  const s = { permissions: { deny: ["Read(./.env)", "Read(secrets/**)"], allow: ["Edit(src/**)"] } };
  assert.deepEqual(["./.env", "sub/.env", "secrets/a.txt", "vendor/secrets/a.txt", "src/app.ts"].map((p) => decide(s, "Read", p)), ["deny", "deny", "deny", "deny", "allow"]);
  assert.deepEqual([".env", "src/app.ts", "vendor/pkg/src/lib.js"].map((p) => decide(s, "Edit", p)), ["deny", "allow", "ask"]);
});

test("memory files load broad to specific with imports and without backticked ones", () => {
  const files = { "/m/CLAUDE.md": "m", "/u/CLAUDE.md": "u", "/r/CLAUDE.md": "See @docs/a.md and `@README`", "/r/docs/a.md": "A, then @b.md", "/r/docs/b.md": "B",
    "/r/s/CLAUDE.md": "s", "/r/s/CLAUDE.local.md": "l", "/r/o/CLAUDE.md": "o" };
  assert.deepEqual(loadMemory(files, "/r/s", "/m/CLAUDE.md", "/u/CLAUDE.md"), ["/m/CLAUDE.md", "/u/CLAUDE.md", "/r/CLAUDE.md", "/r/docs/a.md", "/r/docs/b.md", "/r/s/CLAUDE.md", "/r/s/CLAUDE.local.md"]);
});

test("accept edits mode accepts an undecided edit but not a deny or a command", () => {
  const s = { permissions: { deny: ["Edit(.env)"] } };
  assert.deepEqual(["src/a.py", ".env"].map((p) => decide(s, "Edit", p, "acceptEdits")), ["allow", "deny"]);
  assert.equal(decide(s, "Bash", "make deploy", "acceptEdits"), "ask");
  assert.equal(decide(s, "Edit", "src/a.py"), "ask");
});
