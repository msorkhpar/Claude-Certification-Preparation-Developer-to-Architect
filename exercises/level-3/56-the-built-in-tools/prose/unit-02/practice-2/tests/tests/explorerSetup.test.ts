import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative, resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const ROOT = resolve((import.meta.dirname + "/../project"));
const example = (file: string) => {
  const candidates = [join(import.meta.dirname, "examples", file), `/w/examples/${file}`];
  for (let d = resolve("."), i = 0; i < 9; i++, d = resolve(d, "..")) candidates.push(join(d, "examples", file));
  return pathToFileURL(candidates.find((c) => existsSync(c)) as string).href;
};
const { decide } = await import(example("38-settings-layers/typescript/settingsLayers.ts"));
const { splitFrontmatter } = await import(example("39-hook-gate/typescript/miniyaml.ts"));
const { ruleTool, toolSet } = await import(example("56-builtin-tools/typescript/builtinTools.ts"));

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

function check(table: Array<[string, string, string]>) {
  const s = readJson(".claude/settings.json");
  const got = table.map(([tool, arg]) => decide(s, ruleTool(tool), arg));
  assert.deepEqual(got, table.map((row) => row[2]), JSON.stringify(table.map(([t, a], i) => [t, a, got[i]])));
}

function sections(text: string): [string, string[], string[]] {
  const at = text.indexOf("## When an edit does not apply");
  const head = at < 0 ? text : text.slice(0, at);
  const tail = at < 0 ? "" : text.slice(at);
  const steps = (block: string) => [...block.matchAll(/^\d+\. (.*)$/gm)].map((m) => m[1]);
  return [head, steps(head), steps(tail)];
}

test("m1 the permission rules let an explorer read search and take notes but not change the source", () => {
  check([["Read", "src/inventory/stock.py", "allow"], ["Grep", "src/inventory", "allow"], ["Glob", "src/**/handlers/*.py", "allow"],
    ["Edit", "src/inventory/stock.py", "deny"], ["Write", "src/inventory/new.py", "deny"], ["Edit", "notes/findings.md", "allow"],
    ["Write", "notes/findings.md", "allow"], ["Edit", "docs/readme.md", "ask"], ["Bash", "git log --oneline", "allow"],
    ["Bash", "git diff HEAD~1", "allow"], ["Bash", "git status", "allow"], ["Bash", "rm -rf build", "ask"], ["Bash", "git push origin main", "ask"]]);
});

test("e1 a read rule protects secrets from reading searching and writing", () => {
  check([["Read", "./.env", "deny"], ["Read", "secrets/prod.key", "deny"], ["Grep", "secrets/prod.key", "deny"], ["Glob", "secrets/prod.key", "deny"],
    ["Edit", ".env", "deny"], ["Write", "secrets/new.key", "deny"], ["Edit", "secrets/prod.key", "deny"], ["Read", "vendor/secrets/a.txt", "deny"]]);
});

test("e2 only rule forms that are consulted are used and no whole tool that changes things is allowed", () => {
  const perms = readJson(".claude/settings.json").permissions ?? {};
  const rules: string[] = ["allow", "ask", "deny"].flatMap((kind) => perms[kind] ?? []);
  assert.ok(rules.length > 0, "write the permission rules");
  assert.ok(!rules.some((r) => /^(Write|NotebookEdit|MultiEdit)\(/.test(r)), "path rules for Write are never matched: write them as Edit(...)");
  assert.ok(!rules.some((r) => /^(Grep|Glob)\(/.test(r)), "path rules for the search tools are written as Read(...)");
  assert.ok(!(perms.allow ?? []).some((r: string) => ["Bash", "Bash(*)", "Edit", "Write"].includes(r)), "a bare allow rule approves every call of that tool");
  assert.ok(Object.keys(perms).every((k) => ["allow", "ask", "deny"].includes(k)));
});

test("e3 the explorer agent reads and searches and says when to use it", () => {
  const [fm, body] = splitFrontmatter(read(".claude/agents/explorer.md"));
  assert.equal(fm.name, "explorer");
  const tools = String(fm.tools ?? "").split(",").map((t: string) => t.trim()).filter(Boolean).sort();
  assert.deepEqual(tools, ["Glob", "Grep", "Read"], "an explorer lists Read, Grep and Glob and nothing that edits or runs commands");
  assert.ok(/(?:^|\. )Use when\b/.test(String(fm.description ?? "")), "the description starts a sentence with Use when");
  assert.ok(Number.isInteger(fm.maxTurns) && fm.maxTurns >= 1 && fm.maxTurns <= 15 && fm.model, "bound the turns and choose a model");
  assert.ok(String(body).trim(), "the body holds the agent's instructions");
});

test("e4 the sdk options make the search tools available and remove the tools that change things", () => {
  const o = readJson("agent-options.json");
  const { tools, allowedTools: allowed, disallowedTools: disallowed } = o;
  assert.deepEqual([...(tools ?? [])].sort(), ["Glob", "Grep", "Read"], "tools lists the three the agent may have; naming Grep and Glob puts them back on macOS, Linux and WSL");
  assert.deepEqual(toolSet("linux", tools, allowed ?? [], disallowed ?? []), ["Read", "Grep", "Glob"]);
  assert.ok(Array.isArray(allowed) && allowed.every((t: string) => tools.includes(t)), "allowedTools pre-approves only listed tools");
  assert.ok(!allowed.some((t: string) => ["Bash", "Edit", "Write"].includes(t)));
  assert.ok(["Bash", "Edit", "Write"].every((t) => (disallowed ?? []).includes(t)), "a bare name in disallowedTools removes the tool from the model's context");
});

test("e5 the exploration plan starts with a search then reads and never reads everything first", () => {
  const [head, steps] = sections(read("docs/exploration-plan.md"));
  assert.ok(/do not read every file/i.test(head), "say that the whole repository is not read first");
  assert.ok(steps.length >= 4, "write the steps as a numbered list");
  const grep = steps.findIndex((s) => /\bGrep\b/.test(s));
  const readAt = steps.findIndex((s) => /\bRead\b/.test(s));
  assert.ok(grep >= 0 && readAt >= 0 && grep < readAt, "search for entry points before reading anything");
  assert.ok(steps.some((s) => /\bGlob\b/.test(s) && /`[^`]*(\*\*\/|\*\.)[^`]*`/.test(s)), "use Glob with a name pattern");
  assert.ok(steps.some((s, i) => i > readAt && /export/.test(s) && /\beach\b/.test(s) && /\bGrep\b/.test(s)), "to trace usage through wrappers, list the exported names and search for each");
  assert.ok(steps.some((s) => s.includes("notes/")), "findings go to notes/, the only place the agent may write");
});

test("e6 the edit fallback widens the anchor then replaces all then rewrites the file", () => {
  const [, , fallback] = sections(read("docs/exploration-plan.md"));
  assert.ok(fallback.length >= 3, "write the three remedies as a numbered list under 'When an edit does not apply'");
  assert.ok(/surrounding|more context|longer/.test(fallback[0]) && fallback[0].includes("unique"));
  assert.ok(fallback[1].includes("replace_all"));
  assert.ok(/\bRead\b/.test(fallback[2]) && /\bWrite\b/.test(fallback[2]) && /whole file/i.test(fallback[2]));
});

test("e7 no file holds a personal path an address or a key", () => {
  const hits: string[] = [];
  const walk = (dir: string) => {
    for (const entry of readdirSync(dir).sort()) {
      const path = join(dir, entry);
      if (statSync(path).isDirectory()) walk(path);
      else {
        const text = readFileSync(path, "utf8");
        for (const [label, pattern] of [["home path", /(\/home\/\w+|\/Users\/\w+|C:\\Users)/], ["email address", /[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+/], ["key", /sk-ant-[\w-]{6,}/]] as const) {
          if (pattern.test(text)) hits.push(`${relative(ROOT, path)}: ${label}`);
        }
      }
    }
  };
  walk(ROOT);
  assert.deepEqual(hits, []);
});
