import { test } from "node:test";
import assert from "node:assert/strict";
import { agentsMdRead, contextLines, expandBraces, globMatch, importsOf, launchFiles, onDemandFiles, rulesLoaded, unresolvedImports } from "./memoryLoading.ts";

const TREE = new Set(["CLAUDE.md", "CLAUDE.local.md", "web/CLAUDE.md", "web/ui/CLAUDE.md", "api/CLAUDE.md", "AGENTS.md"]);

test("globs stay in one folder unless they say otherwise", () => {
  assert.ok(globMatch("*.md", "README.md") && !globMatch("*.md", "docs/guide.md"));
  assert.ok(globMatch("**/*.ts", "a.ts") && globMatch("**/*.ts", "a/b/c.ts") && !globMatch("**/*.ts", "a/b/c.tsx"));
  assert.ok(globMatch("src/**/*", "src/a/b.py") && !globMatch("src/**/*", "lib/a.py"));
  assert.ok(globMatch("src/components/*.tsx", "src/components/A.tsx") && !globMatch("src/components/*.tsx", "src/components/x/A.tsx"));
});

test("braces expand and multiply", () => {
  assert.deepEqual(expandBraces("src/*.{ts,tsx}"), ["src/*.ts", "src/*.tsx"]);
  assert.equal(expandBraces("{a,b}/{c,d}/*.{ts,tsx}").length, 8);
  assert.ok(globMatch("src/**/*.{ts,tsx}", "src/ui/x.tsx"));
});

test("a rule without paths is always loaded and a scoped one waits for a match", () => {
  const rules = { "commit.md": null, "testing.md": ["**/*.test.tsx"], "terraform.md": ["terraform/**/*"] };
  assert.deepEqual(rulesLoaded(rules, []), ["commit.md"]);
  assert.deepEqual(rulesLoaded(rules, ["a/b/Button.test.tsx"]), ["commit.md", "testing.md"]);
  assert.deepEqual(rulesLoaded(rules, ["terraform/prod/main.tf", "x/Y.test.tsx"]), ["commit.md", "testing.md", "terraform.md"]);
});

test("launch loads the folders above and on demand loads the folders below", () => {
  assert.deepEqual(launchFiles(TREE, ""), ["CLAUDE.md", "CLAUDE.local.md"]);
  assert.deepEqual(launchFiles(TREE, "web"), ["CLAUDE.md", "CLAUDE.local.md", "web/CLAUDE.md"]);
  assert.deepEqual(onDemandFiles(TREE, "", ["web/ui/Button.tsx"]), ["web/CLAUDE.md", "web/ui/CLAUDE.md"]);
  assert.deepEqual(onDemandFiles(TREE, "web", ["web/ui/Button.tsx", "api/x.py"]), ["web/ui/CLAUDE.md"]);
});

test("agents md is read only when there is no claude md", () => {
  assert.equal(agentsMdRead(new Set(["AGENTS.md"])), true);
  assert.equal(agentsMdRead(TREE), false);
  assert.equal(agentsMdRead(new Set(["AGENTS.md", "CLAUDE.local.md"])), false);
});

test("imports follow four hops and skip code spans", () => {
  const texts = { "CLAUDE.md": "See @docs/a.md and `@code` and @missing.md", "docs/a.md": "@b.md", "docs/b.md": "@c.md", "docs/c.md": "@d.md", "docs/d.md": "@e.md", "docs/e.md": "end" };
  assert.deepEqual(importsOf("CLAUDE.md", texts), ["docs/a.md", "docs/b.md", "docs/c.md", "docs/d.md"]);
  assert.deepEqual(importsOf("CLAUDE.md", { "CLAUDE.md": "```\n@docs/a.md\n```", "docs/a.md": "x" }), []);
});

test("an import saves no context", () => {
  const texts = { "CLAUDE.md": "line\n@docs/a.md", "docs/a.md": "1\n2\n3" };
  assert.equal(contextLines(["CLAUDE.md", ...importsOf("CLAUDE.md", texts)], texts), 5);
});

test("an import that names no file is reported", () => {
  const texts = { "CLAUDE.md": "@docs/a.md and @docs/typo.md and `@code`", "docs/a.md": "x" };
  assert.deepEqual(unresolvedImports("CLAUDE.md", texts), ["docs/typo.md"]);
});
