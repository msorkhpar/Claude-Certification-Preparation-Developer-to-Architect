import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative, resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution: the project folder that holds managed/, .claude/ and docs/.
const ROOT = resolve(process.env.SOLUTION_DIR ?? "starter");

const MANAGED_ONLY = ["allowManagedPermissionRulesOnly", "allowManagedHooksOnly", "allowManagedMcpServersOnly", "strictKnownMarketplaces", "disableSideloadFlags"];
const EFFORT = ["low", "medium", "high", "xhigh", "max"];
const PRECEDENCE = ["managed settings", "command line", "project local", "shared project", "user"];

function read(rel: string): string {
  const path = join(ROOT, rel);
  assert.ok(existsSync(path) && statSync(path).isFile(), `${rel} is missing`);
  return readFileSync(path, "utf8");
}

function loadJson(rel: string): any {
  try {
    return JSON.parse(read(rel));
  } catch (error) {
    if (error instanceof SyntaxError) assert.fail(`${rel} is not valid JSON: ${error.message}`);
    throw error;
  }
}

const managed = (): any => loadJson("managed/managed-settings.json");

function section(title: string): string {
  const parts = read("docs/rollout.md").split(/^## (.*)$/m);
  for (let i = 1; i < parts.length - 1; i += 2) if (parts[i].trim() === title) return parts[i + 1].trim();
  assert.fail(`the section '${title}' is missing`);
}

function table(text: string): string[][] {
  const rows: string[][] = [];
  for (const line of text.split("\n")) {
    if (line.startsWith("|")) {
      const cells = line.trim().replace(/^\||\|$/g, "").split("|").map((c) => c.trim());
      if (!cells.every((c) => /^[-: ]*$/.test(c))) rows.push(cells);
    }
  }
  return rows.slice(1);
}

function walk(dir: string): string[] {
  return readdirSync(dir).sort().flatMap((name) => {
    const full = join(dir, name);
    return statSync(full).isDirectory() ? walk(full) : [full];
  });
}

test("m1 the managed file locks permissions and protects the environment file", () => {
  const settings = managed();
  assert.equal(settings.allowManagedPermissionRulesOnly, true, "make managed settings the only source of permission rules");
  assert.equal(settings.permissions?.disableBypassPermissionsMode, "disable", "turn off bypass mode");
  assert.ok((settings.permissions?.deny ?? []).includes("Read(./.env)"), "deny reading the environment file");
});

test("e1 keys only an organisation can set live in the managed file and never in the project file", () => {
  const project = loadJson(".claude/settings.json");
  for (const key of MANAGED_ONLY) assert.ok(!(key in project), `${key} has no effect in the project file: move it to the managed file`);
  assert.equal(managed().allowManagedHooksOnly, true, "only managed hooks run");
});

test("e2 plugins come only from the companys marketplace and cannot be sideloaded", () => {
  const settings = managed();
  const sources = settings.strictKnownMarketplaces;
  assert.ok(Array.isArray(sources) && sources.length > 0, "allow the company's marketplace; an empty list blocks every source, the official one too");
  for (const entry of sources) {
    const ok = (entry.source === "github" && String(entry.repo ?? "").startsWith("example-org/")) || (entry.source === "url" && String(entry.url ?? "").startsWith("https://plugins.example.com/"));
    assert.ok(ok, `${JSON.stringify(entry)} is not a source the company owns`);
  }
  assert.equal(settings.disableSideloadFlags, true, "reject the flags that load plugins from a folder or an address");
});

test("e3 only the managed mcp allowlist applies and a server is never allowed and denied", () => {
  const settings = managed();
  assert.equal(settings.allowManagedMcpServersOnly, true, "ignore allowlists in user, project and local files");
  const allowed: any[] = settings.allowedMcpServers ?? [];
  assert.ok(allowed.length > 0, "allow at least one server");
  for (const entry of allowed) {
    const keys = Object.keys(entry);
    assert.ok(keys.length === 1 && ["serverName", "serverCommand", "serverUrl"].includes(keys[0]), `${JSON.stringify(entry)}: exactly one of serverName, serverCommand or serverUrl`);
    if ("serverName" in entry) assert.ok(/^[A-Za-z0-9_-]+$/.test(entry.serverName), `${entry.serverName}: letters, numbers, hyphens and underscores only`);
  }
  const names = allowed.filter((e) => "serverName" in e).map((e) => e.serverName);
  const denied: string[] = (settings.deniedMcpServers ?? []).filter((e: any) => "serverName" in e).map((e: any) => e.serverName);
  const both = names.filter((n) => denied.includes(n));
  assert.deepEqual(both, [], `${both} is allowed and denied: the denial wins`);
});

test("e4 the model choice is locked by a list and the effort cap is at most high", () => {
  const settings = managed();
  const models = settings.availableModels;
  assert.ok(Array.isArray(models) && models.length > 0, "a managed model is only a default: list availableModels to lock the choice");
  if ("model" in settings) assert.ok(models.includes(settings.model), "the default model is one of the available models");
  const cap = settings.maxEffortLevel;
  assert.ok(EFFORT.includes(cap) && EFFORT.indexOf(cap) <= EFFORT.indexOf("high"), "cap the effort at high or lower; max sets no cap");
});

test("e5 the group spend limits add up to the organisation limit and no more", () => {
  const text = section("Spend limits");
  assert.ok(text.toLowerCase().includes("usage credits"), "say that usage credits are on");
  const rows = table(text);
  const limit = (row: string[]) => parseInt(row[2].replace(/,/g, ""), 10);
  const org = rows.filter((r) => r[0] === "organization").map(limit);
  const groups = rows.filter((r) => r[0] === "group").map(limit);
  const members = rows.filter((r) => r[0] === "member").map(limit);
  assert.ok(org.length === 1 && groups.length > 0 && members.length > 0, "list the organisation, its groups and the member default");
  assert.ok(groups.reduce((a, b) => a + b, 0) <= org[0], "the group limits add up to the organisation limit or less");
  assert.ok(members[0] <= Math.min(...groups), "a member's limit is within the smallest group's");
});

test("e6 adoption is measured by outcomes against a baseline of at least 4 weeks", () => {
  const text = section("Adoption");
  const baseline = text.match(/Baseline:\s*(\d+) weeks/);
  assert.ok(baseline && parseInt(baseline[1], 10) >= 4, "take a baseline of at least 4 weeks before the rollout");
  const block = (text + "\n").match(/Outcome targets:\n((?:- .*\n?)+)/);
  assert.ok(block, "list the outcome targets");
  const targets = block[1].split("\n").filter(Boolean).map((line) => line.slice(2).toLowerCase());
  assert.ok(targets.length >= 2, "set at least two outcome targets");
  for (const target of targets) {
    assert.ok(["pull requests", "time to merge", "review", "defects"].some((w) => target.includes(w)), `'${target}' is not an outcome`);
    assert.ok(!["lines accepted", "prompts", "suggestions accepted"].some((w) => target.includes(w)), `'${target}' measures activity`);
  }
});

test("e7 the precedence table is in the documented order and no file holds personal data", () => {
  const rows = table(section("Precedence"));
  assert.deepEqual(rows.map((r) => r[1].toLowerCase()), PRECEDENCE, "the levels are managed, command line, project local, shared project and user, in that order");
  const doc = read("docs/rollout.md");
  assert.ok(doc.includes("per-group") && doc.includes("not yet supported"), "say that server-managed settings cannot target a group yet");
  const hits: string[] = [];
  for (const path of walk(ROOT)) {
    const text = readFileSync(path, "utf8");
    if (/(\/home\/\w+|\/Users\/\w+|C:\\Users)/.test(text)) hits.push(`${relative(ROOT, path)}: home path`);
    if (/[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+/.test(text)) hits.push(`${relative(ROOT, path)}: email address`);
  }
  assert.deepEqual(hits, []);
});
