import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative, resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution: the marketplace folder (it holds .claude-plugin/marketplace.json).
const ROOT = resolve(process.env.SOLUTION_DIR ?? "starter");
const PLUGIN = "plugins/standards-kit";
const ID = /^[A-Za-z0-9][A-Za-z0-9._-]*$/;
const SEMVER = /^\d+\.\d+\.\d+$/;
const SHA = /^[0-9a-f]{40}$/;
const TAG = /^v\d+\.\d+\.\d+$/;
const RESERVED = new Set(["claude-code-marketplace", "claude-code-plugins", "claude-plugins-official", "anthropic-marketplace", "anthropic-plugins", "agent-skills",
  "anthropic-agent-skills", "life-sciences", "knowledge-work-plugins", "claude-for-legal", "claude-for-financial-services",
  "financial-services-plugins", "first-party-plugins", "claude-tag-plugins", "claude-community", "claude-plugins-community", "healthcare",
  "anthropic-plugin-directory", "claude-plugin-directory", "inline", "builtin", "skills-dir", "synced", "claude-plugin-test",
  "npm", "pip", "uv", "cargo", "github", "gh"]);
const SOURCE_TYPES = new Set(["github", "url", "git-subdir", "npm", "archive", "command"]);

function read(rel: string): string {
  const path = join(ROOT, rel);
  assert.ok(existsSync(path) && statSync(path).isFile(), `${rel} is missing`);
  return readFileSync(path, "utf8");
}

function readJson(rel: string): any {
  try {
    return JSON.parse(read(rel));
  } catch (error) {
    if (error instanceof SyntaxError) assert.fail(`${rel} is not valid JSON: ${error.message}`);
    throw error;
  }
}

function isObject(value: unknown): boolean {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function market(): any {
  const data = readJson(".claude-plugin/marketplace.json");
  assert.ok(isObject(data), "marketplace.json must hold an object");
  return data;
}

function entries(): any[] {
  const plugins = market().plugins;
  assert.ok(Array.isArray(plugins) && plugins.length > 0, "plugins must be a non-empty list");
  assert.ok(plugins.every(isObject), "every plugin entry is an object");
  return plugins;
}

function entry(name: string): any {
  const found = entries().filter((p) => p.name === name);
  assert.ok(found.length > 0, `no entry named ${name}`);
  return found[0];
}

function manifest(): any {
  const data = readJson(`${PLUGIN}/.claude-plugin/plugin.json`);
  assert.ok(isObject(data), "plugin.json must hold an object");
  return data;
}

function front(rel: string): [Record<string, string>, string] {
  const match = /^---\n([\s\S]*?)\n---\n?([\s\S]*)$/.exec(read(rel));
  assert.ok(match, `${rel} has no front matter`);
  const fields: Record<string, string> = {};
  for (const line of (match as RegExpExecArray)[1].split("\n")) {
    const at = line.indexOf(":");
    if (at >= 0) fields[line.slice(0, at).trim()] = line.slice(at + 1).trim();
  }
  return [fields, (match as RegExpExecArray)[2]];
}

function files(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => (e.isDirectory() ? files(join(dir, e.name)) : [join(dir, e.name)]));
}

test("m1 the marketplace file names an owner and lists each plugin by a source that resolves", () => {
  const data = market();
  assert.ok(typeof data.name === "string" && data.name.trim() !== "", "a marketplace needs a name");
  assert.ok(isObject(data.owner) && String(data.owner.name ?? "").trim() !== "", "the owner needs a name");
  assert.ok(String(data.description ?? "").trim() !== "", "describe the marketplace");
  const names = entries().map((p) => p.name);
  assert.ok(names.every((n) => typeof n === "string" && n !== ""), "every entry needs a name");
  assert.equal(new Set(names).size, names.length, "entry names are unique");
  for (const p of entries()) {
    assert.ok("source" in p, `${p.name} needs a source`);
    const source = p.source;
    if (typeof source === "string") {
      assert.ok(source.startsWith("./") && !source.includes("..") && !source.includes("\\"), `${p.name}: a relative path starts with ./ and stays inside the marketplace`);
      assert.ok(existsSync(join(ROOT, source)) && statSync(join(ROOT, source)).isDirectory(), `${p.name}: ${source} is not a folder of the marketplace`);
      const inner = JSON.parse(readFileSync(join(ROOT, source, ".claude-plugin", "plugin.json"), "utf8"));
      assert.equal(inner.name, p.name, `${p.name}: the entry name and the manifest name must be the same`);
    } else {
      assert.ok(isObject(source) && SOURCE_TYPES.has(source.source), `${p.name}: a source object names a type`);
    }
  }
});

test("e1 names are valid in a plugin id and are not reserved or mistaken for official ones", () => {
  const name = market().name ?? "";
  assert.equal(name, "example-org-tools", "call the marketplace example-org-tools");
  assert.ok(ID.test(name) && !name.includes(".."));
  assert.ok(!RESERVED.has(name) && !name.startsWith("claudeai-"));
  assert.ok(!/claude|anthropic/.test(name.toLowerCase()), "a name that mentions claude or anthropic risks being refused as an impersonation");
  const pluginNames = entries().map((p) => p.name).sort();
  assert.deepEqual(pluginNames, ["db-tools", "lint-helper", "standards-kit"], "list standards-kit, lint-helper and db-tools");
  for (const n of pluginNames) {
    const low = n.toLowerCase();
    assert.ok(ID.test(n), `${n}: letters, digits, dots, underscores and hyphens only`);
    assert.ok(!["claude-", "anthropic-", "anthropics-", "cc-plugin-"].some((p) => low.startsWith(p)) && !["claude", "anthropic", "anthropics", "claude-code", "claude-mods"].includes(low), `${n} passes as an Anthropic plugin`);
    assert.ok(!(low.includes("official") && /claude|anthropic/.test(low)), `${n} passes as an official plugin`);
  }
  assert.equal(manifest().name, "standards-kit");
});

test("e2 the plugin keeps only its manifest in the manifest folder and finds its files through the plugin root", () => {
  assert.deepEqual(readdirSync(join(ROOT, PLUGIN, ".claude-plugin")).sort(), ["plugin.json"], "only plugin.json goes inside .claude-plugin");
  assert.ok(!existsSync(join(ROOT, PLUGIN, "CLAUDE.md")), "an instruction file at the plugin root is not loaded as context");
  const [fields, body] = front(`${PLUGIN}/skills/changelog/SKILL.md`);
  assert.ok(fields.name === "changelog" && (fields.description ?? "").includes("Use when") && body.trim() !== "" && !body.includes("TODO"));
  const hooks = readJson(`${PLUGIN}/hooks/hooks.json`).hooks;
  assert.ok(isObject(hooks), "hooks.json wraps the event map in a top-level hooks key");
  const groups = hooks.PreToolUse ?? [];
  assert.ok(groups.length === 1 && groups[0].matcher === "Edit|Write", "one PreToolUse group for Edit|Write");
  const handler = groups[0].hooks[0];
  assert.ok(handler.type === "command" && (handler.command ?? "").includes("${CLAUDE_PLUGIN_ROOT}"), "reach the script through ${CLAUDE_PLUGIN_ROOT}");
  assert.ok(existsSync(join(ROOT, PLUGIN, "scripts", "protect.sh")));
  const servers = Object.values(readJson(`${PLUGIN}/.mcp.json`).mcpServers ?? {}) as any[];
  assert.ok(servers.length > 0 && servers.every((s) => (s.args ?? []).join(" ").includes("${CLAUDE_PLUGIN_ROOT}")), "every server reaches its code through ${CLAUDE_PLUGIN_ROOT}");
  for (const key of ["skills", "commands", "agents", "hooks", "mcpServers"]) {
    const value = manifest()[key];
    for (const path of typeof value === "string" ? [value] : Array.isArray(value) ? value : []) assert.ok(String(path).startsWith("./"), `a component path in plugin.json starts with ./ (${key})`);
  }
});

test("e3 the version lives in the manifest alone and is semantic", () => {
  const data = manifest();
  assert.ok(SEMVER.test(String(data.version ?? "")), "give the plugin a semantic version in plugin.json");
  assert.ok(String(data.description ?? "").trim() !== "");
  assert.ok(!("version" in entry("standards-kit")), "set the version in plugin.json or in the entry, not both");
});

test("e4 each source kind is written in its own form and pinned to a tag and a commit", () => {
  assert.equal(entry("standards-kit").source, "./plugins/standards-kit");
  const helper = entry("lint-helper").source;
  assert.ok(helper.source === "github" && /^[\w.-]+\/[\w.-]+$/.test(helper.repo ?? ""), "a github source takes owner/repo");
  const sub = entry("db-tools").source;
  assert.ok(sub.source === "git-subdir" && sub.path === "tools/db-tools" && sub.url, "a git-subdir source takes a url and a path");
  for (const p of entries()) {
    const source = p.source;
    if (typeof source === "string") continue;
    if (source.source === "url") assert.ok(/^(https?:\/\/|file:\/\/|git@)/.test(source.url ?? ""), `${p.name}: a url source takes a full git url, not owner/repo`);
    if (["github", "url", "git-subdir"].includes(source.source)) {
      assert.ok(TAG.test(String(source.ref ?? "")), `${p.name}: pin ref to a release tag such as v1.2.0`);
      assert.ok(SHA.test(String(source.sha ?? "")), `${p.name}: pin sha to a full 40-character lowercase commit`);
    }
    if (source.source === "archive") assert.ok(/^[0-9a-fA-F]{64}$/.test(String(source.sha256 ?? "")), `${p.name}: pin an archive with sha256`);
  }
});

test("e5 a dependency carries a range and one from another marketplace is allowed by name", () => {
  assert.deepEqual(manifest().dependencies, [{ name: "lint-helper", version: "~1.2.0" }], "depend on lint-helper ~1.2.0");
  assert.deepEqual(entry("db-tools").dependencies, [{ name: "audit-logger", marketplace: "shared-tools" }], "db-tools depends on audit-logger from shared-tools");
  assert.deepEqual(market().allowCrossMarketplaceDependenciesOn, ["shared-tools"], "the root marketplace must allow shared-tools");
});

test("e6 the team settings register the marketplace and enable only what installs from it", () => {
  const settings = readJson(".claude/settings.json");
  const markets = settings.extraKnownMarketplaces ?? {};
  assert.deepEqual(Object.keys(markets), [market().name], "register the marketplace under its own name");
  const source = markets[market().name].source ?? {};
  assert.ok(source.source === "github" && /^[\w.-]+\/[\w.-]+$/.test(source.repo ?? ""), "a github source with an owner/name repo");
  const enabled = settings.enabledPlugins ?? {};
  assert.ok(Object.keys(enabled).length > 0 && Object.values(enabled).every((v) => v === true));
  for (const id of Object.keys(enabled)) {
    const at = id.indexOf("@");
    const [name, marketplace] = at < 0 ? [id, ""] : [id.slice(0, at), id.slice(at + 1)];
    assert.equal(marketplace, market().name, `${id}: enable plugins from the marketplace you register`);
    assert.equal(typeof entry(name).source, "string", `${id}: a plugin from an external source is not installed by the repository settings alone`);
  }
  assert.ok("standards-kit@example-org-tools" in enabled);
});

test("e7 renames lead every former name to a current plugin or to null", () => {
  const renames = market().renames;
  assert.ok(isObject(renames) && renames["std-kit"] === "standards-kit" && "old-linter" in renames && renames["old-linter"] === null);
  const current = new Set(entries().map((p) => p.name));
  for (const old of Object.keys(renames)) {
    assert.ok(!current.has(old), `${old} is still listed as a plugin`);
    const seen = new Set([old]);
    let step = renames[old];
    while (step !== null && !current.has(step)) {
      assert.ok(step in renames && !seen.has(step), `the rename of ${old} does not end at a plugin or at null`);
      seen.add(step);
      step = renames[step];
    }
  }
});

test("e8 no file is left unfinished or holds a personal path an address or a key", () => {
  const all = files(ROOT).sort();
  for (const path of all) {
    const text = readFileSync(path, "utf8");
    const rel = relative(ROOT, path);
    assert.ok(!text.includes("TODO"), `${rel} still has a TODO`);
    assert.ok(!/\/home\/|\/Users\/|[A-Za-z]:\\Users|sk-ant-/.test(text), `${rel} holds a personal path or a key`);
    assert.ok((text.match(/[\w.+-]+@[\w-]+\.[\w.-]+/g) ?? []).every((a) => a.endsWith("@example.com")), `${rel} holds an address that is not a placeholder`);
  }
  assert.ok(all.length >= 8);
});
