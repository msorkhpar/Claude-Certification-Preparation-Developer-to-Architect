import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative, resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution: the project folder that holds CLAUDE.md and .claude/.
const ROOT = resolve(process.env.SOLUTION_DIR ?? "starter");
const example = (file: string) => {
  const candidates = [`/w/examples/${file}`];
  for (let d = resolve("."), i = 0; i < 9; i++, d = resolve(d, "..")) candidates.push(join(d, "examples", file));
  return pathToFileURL(candidates.find((c) => existsSync(c)) as string).href;
};
const { decide, effectiveSettings, loadMemory } = await import(example("38-settings-layers/typescript/settingsLayers.ts"));
const { splitFrontmatter } = await import(example("39-hook-gate/typescript/miniyaml.ts"));

const MANAGED = { permissions: { deny: ["Bash(sudo *)"] } }; // a fictional organisation policy
const USER = { model: "haiku", permissions: { allow: ["Bash(ls *)"] } };

function read(rel: string): string {
  const path = join(ROOT, rel);
  assert.ok(existsSync(path) && statSync(path).isFile(), `${rel} is missing`);
  return readFileSync(path, "utf8");
}

function readJson(rel: string): any {
  let data: any;
  try {
    data = JSON.parse(read(rel));
  } catch (error) {
    if (error instanceof SyntaxError) assert.fail(`${rel} is not valid JSON: ${error.message}`);
    throw error;
  }
  assert.ok(data && typeof data === "object" && !Array.isArray(data), `${rel} must hold a JSON object`);
  return data;
}

function layers(withLocal = true): Record<string, any> {
  const out: Record<string, any> = { managed: MANAGED, user: USER, project: readJson(".claude/settings.json") };
  if (withLocal && existsSync(join(ROOT, ".claude/settings.local.json"))) out["local"] = readJson(".claude/settings.local.json");
  return out;
}

test("m1 the memory file is short concrete and pulls in the architecture notes", () => {
  const text = read("CLAUDE.md");
  assert.ok(text.split("\n").length - (text.endsWith("\n") ? 1 : 0) <= 200, "keep CLAUDE.md under 200 lines");
  assert.ok(text.includes("`make test`") && text.includes("`make lint`"), "name the test and lint commands in backticks");
  assert.ok(/^[^`\n]*@docs\/architecture\.md/m.test(text), "import docs/architecture.md with an @ line");
  const emphasis = text.split("\n").filter((line) => /\b(IMPORTANT|MUST|NEVER|ALWAYS)\b/.test(line));
  assert.ok(emphasis.length <= 2, "emphasis on many lines makes none of them stand out");
  const files = { "/p/CLAUDE.md": text, "/p/docs/architecture.md": read("docs/architecture.md") };
  assert.deepEqual(loadMemory(files, "/p"), ["/p/CLAUDE.md", "/p/docs/architecture.md"], "the import must resolve to the architecture file");
});

test("e1 the permission rules allow the daily commands ask before commits and deny secrets and pushes", () => {
  const s = effectiveSettings(layers());
  const mode = s.permissions?.defaultMode ?? "default";
  const table: Array<[string, string, string]> = [["Bash", "make test", "allow"], ["Bash", "make lint", "allow"], ["Bash", "make deploy", "ask"], ["Bash", "git status", "allow"],
    ["Bash", "git diff HEAD~1", "allow"], ["Bash", "git commit -m 'AB-1 fix'", "ask"], ["Bash", "git push origin main", "deny"],
    ["Bash", "make test && git push origin main", "deny"], ["Bash", "curl https://example.com", "deny"], ["Bash", "sudo make test", "deny"],
    ["Bash", "ls -la", "allow"], ["Read", "./.env", "deny"], ["Read", "secrets/key.pem", "deny"], ["Edit", ".env", "deny"],
    ["Read", "src/api/app.py", "allow"], ["Edit", "src/api/app.py", "allow"]];
  const wrong = table.filter(([tool, arg, want]) => decide(s, tool, arg, mode) !== want).map(([tool, arg, want]) => `${tool}(${arg}) should be ${want}, is ${decide(s, tool, arg, mode)}`);
  assert.deepEqual(wrong, []);
});

test("e2 the shared file sets a mode it may set and uses only rules that are consulted", () => {
  const shared = readJson(".claude/settings.json");
  const perms = shared.permissions ?? {};
  assert.equal(perms.defaultMode, "acceptEdits", "set defaultMode to acceptEdits; auto and bypassPermissions are ignored in a repository file");
  assert.equal(effectiveSettings({ project: shared }).permissions.defaultMode, "acceptEdits");
  const rules: string[] = ["allow", "ask", "deny"].flatMap((kind) => perms[kind] ?? []);
  assert.deepEqual(rules.filter((r) => /^(Write|NotebookEdit|MultiEdit)\(/.test(r)), [], "path rules for Write are never consulted: use Edit or Read");
  assert.deepEqual((perms.allow ?? []).filter((r: string) => r === "Bash" || r === "Bash(*)"), [], "a bare Bash allow rule approves every command");
  assert.ok(Object.keys(perms).every((k) => ["defaultMode", "allow", "ask", "deny"].includes(k)));
});

test("e3 personal settings stay local and the local file wins", () => {
  const ignore = read(".gitignore").split("\n").map((l) => l.trim());
  assert.ok(ignore.includes(".claude/settings.local.json") && ignore.includes("CLAUDE.local.md"), "git must ignore both personal files");
  assert.equal(readJson(".claude/settings.json").model, "opus", "the team default model is opus");
  assert.equal(effectiveSettings(layers()).model, "sonnet", "the local file overrides the team model");
  assert.equal(effectiveSettings(layers(false)).model, "opus");
  const local = readJson(".claude/settings.local.json");
  assert.ok(!("permissions" in local) && !("env" in local), "keep the local file to the model override");
});

test("e4 the custom command is a skill that only a person can start", () => {
  const [fm, body] = splitFrontmatter(read(".claude/skills/fix-issue/SKILL.md"));
  assert.ok(fm.name === "fix-issue" && String(fm.description ?? "").trim() !== "", "name and description are required");
  assert.equal(fm["disable-model-invocation"], true, "a command that edits code and calls gh is started by a person");
  assert.ok(fm["argument-hint"], "show the argument in the menu");
  assert.ok(body.includes("$ARGUMENTS") && body.includes("`make test`"));
});

test("e5 the headless script is bounded and does not skip permissions", () => {
  const flat = read("scripts/ci-review.sh").replace(/\\\n/g, " ");
  assert.ok(/\bclaude -p\b/.test(flat) && flat.includes("--output-format json") && flat.includes("--bare"));
  const turns = /--max-turns (\d+)/.exec(flat);
  assert.ok(turns && Number(turns[1]) >= 1 && Number(turns[1]) <= 10, "cap the turns at 10 or fewer");
  assert.ok(/--max-budget-usd \d/.test(flat), "cap the spend");
  assert.ok(/--permission-mode (dontAsk|plan)\b/.test(flat), "start from a mode that never prompts and never auto-approves");
  const allowed = /--allowedTools "([^"]*)"/.exec(flat);
  assert.ok(allowed, "list the pre-approved tools");
  const tools = (allowed as RegExpExecArray)[1].split(/,(?![^()]*\))/).map((t) => t.trim());
  assert.ok(tools.length > 0 && !["Bash", "Edit", "Write"].some((t) => tools.includes(t)), "pre-approve patterns, not whole tools that change things");
  assert.ok(!flat.includes("dangerously-skip-permissions") && !flat.includes("bypassPermissions"));
});

test("e6 no file holds a personal path an address or a key", () => {
  const hits: string[] = [];
  const walk = (dir: string) => {
    for (const name of readdirSync(dir).sort()) {
      const path = join(dir, name);
      if (statSync(path).isDirectory()) walk(path);
      else {
        const text = readFileSync(path, "utf8");
        for (const [label, pattern] of [["home path", /(\/home\/\w+|\/Users\/\w+|C:\\Users)/], ["email address", /[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+/], ["key", /sk-ant-[\w-]{6,}/]] as Array<[string, RegExp]>) {
          if (pattern.test(text)) hits.push(`${relative(ROOT, path)}: ${label}`);
        }
      }
    }
  };
  walk(ROOT);
  assert.deepEqual(hits, []);
});
