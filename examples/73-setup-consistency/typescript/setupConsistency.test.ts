import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { HERE, agents, audit, literalSecrets, load } from "./setupConsistency.ts";

function project(mcp: object, settings: object, agentFiles: Record<string, string>): string {
  const dir = mkdtempSync(join(tmpdir(), "consistency-"));
  mkdirSync(join(dir, ".claude/agents"), { recursive: true });
  writeFileSync(join(dir, ".mcp.json"), JSON.stringify({ mcpServers: mcp }));
  writeFileSync(join(dir, ".claude/settings.json"), JSON.stringify({ permissions: settings }));
  for (const [name, text] of Object.entries(agentFiles)) writeFileSync(join(dir, ".claude/agents", `${name}.md`), text);
  return dir;
}

test("the flawed project has every finding and the fixed one has none", () => {
  assert.deepEqual(audit(join(HERE, "project-before")), ["literal-secret: tickets headers.Authorization", "unknown-server: wiki (explorer)", "agent-bare-bash: explorer",
    "agent-inherits-all: scaffolder", "env-readable", "bare-write-allowed"]);
  assert.deepEqual(audit(join(HERE, "project-after")), []);
});

test("a subagent without a tools line inherits everything and is not an empty list", () => {
  const found = agents(join(HERE, "project-before"));
  assert.equal(found.scaffolder, null);
  assert.deepEqual(found.explorer, ["Read", "Grep", "Bash", "mcp__wiki__search"]);
});

test("only a value with no environment reference is a literal secret", () => {
  const servers = { a: { headers: { Authorization: "Bearer ${TOKEN}", "X-Trace": "abc" } }, b: { env: { API_KEY: "abc", REGION: "eu" } } };
  assert.deepEqual(literalSecrets(servers), ["b env.API_KEY"]);
});

test("a permission rule for a server that is not configured is found", () => {
  const root = project({ docs: { type: "stdio", command: "x" } }, { allow: ["mcp__ghost__read"], deny: ["Read(./.env)"] }, { a: "---\nname: a\ndescription: Use when.\ntools: Read\n---\nbody\n" });
  assert.deepEqual(audit(root), ["unknown-server: ghost (settings)"]);
  assert.deepEqual(load(root).subagents, { a: ["Read"] });
});
