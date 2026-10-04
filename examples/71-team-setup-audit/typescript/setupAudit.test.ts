import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { HERE, audit, globRegex, load, rulePaths, rulesFor } from "./setupAudit.ts";

test("a double star crosses folders and a single star does not", () => {
  assert.ok(globRegex("**/*.test.ts").test("a/b/c.test.ts") && globRegex("**/*.test.ts").test("c.test.ts"));
  assert.ok(globRegex("src/*.ts").test("src/a.ts") && !globRegex("src/*.ts").test("src/x/a.ts"));
  assert.ok(!globRegex("src/api/**/*.ts").test("src/models/a.ts"));
});

test("the paths of a rule are read from its front matter and absent means always", () => {
  assert.deepEqual(rulePaths('---\npaths:\n  - "a/**"\n  - b.md\n---\n\nbody\n'), ["a/**", "b.md"]);
  assert.equal(rulePaths("# no front matter\n"), null);
  assert.equal(rulePaths("---\nname: x\n---\nbody\n"), null);
});

test("the flawed project has every finding and the fixed one has none", () => {
  assert.deepEqual(audit(join(HERE, "project-before")), [
    "all-in-root: 5 sections in CLAUDE.md and no rule files", "test-uncovered: src/components/Button.test.tsx", "test-uncovered: src/api/orders.test.ts",
    "test-uncovered: src/models/order.test.ts", "no-shared-command", "env-readable", "bare-bash-allowed"]);
  assert.deepEqual(audit(join(HERE, "project-after")), []);
});

test("test files load the tests rule wherever they sit", () => {
  const { rules } = load(join(HERE, "project-after"));
  assert.deepEqual(rulesFor(rules, "src/components/Button.test.tsx"), ["components.md", "tests.md"]);
  assert.deepEqual(rulesFor(rules, "docs/readme.md"), []);
});

test("a rule without paths and a rule that matches nothing are flagged", () => {
  const dir = mkdtempSync(join(tmpdir(), "audit-"));
  mkdirSync(join(dir, ".claude/rules"), { recursive: true });
  mkdirSync(join(dir, ".claude/commands"));
  writeFileSync(join(dir, ".claude/commands/review.md"), "review\n");
  writeFileSync(join(dir, ".claude/settings.json"), '{"permissions": {"deny": ["Read(./.env)"]}}');
  writeFileSync(join(dir, "files.txt"), "src/a.ts\n");
  writeFileSync(join(dir, "CLAUDE.md"), "# Notes\n");
  writeFileSync(join(dir, ".claude/rules/everywhere.md"), "# Always\n");
  writeFileSync(join(dir, ".claude/rules/typo.md"), '---\npaths:\n  - "source/**/*.ts"\n---\n\nx\n');
  assert.deepEqual(audit(dir), ["rule-loads-always: everywhere.md", "rule-matches-nothing: typo.md"]);
});
