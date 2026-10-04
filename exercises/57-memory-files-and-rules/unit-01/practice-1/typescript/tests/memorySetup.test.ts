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
const { decide } = await import(example("38-settings-layers/typescript/settingsLayers.ts"));
const { splitFrontmatter } = await import(example("39-hook-gate/typescript/miniyaml.ts"));
const { ruleTool } = await import(example("56-builtin-tools/typescript/builtinTools.ts"));
const { globMatch, importsOf, rulesLoaded, unresolvedImports } = await import(example("57-memory-loading/typescript/memoryLoading.ts"));

const T = ["Name test files `<module>.test.ts` or `<Component>.test.tsx`, next to the file they test.",
  "Every test builds its data from a fixture in `tests/fixtures/`; no ad-hoc data inline.",
  "Mock the network with `msw`; a test never calls a live service.",
  "Each test checks one behaviour and its name states that behaviour."];
const A = ["Every handler under `src/api/` validates its input with the shared `parse()` helper before it uses it.",
  "Return errors as `{ code, message }` with the HTTP status from `src/api/status.ts`.",
  "Handlers are `async` and never swallow a rejected promise.",
  "Document each endpoint with an OpenAPI comment above its handler."];
const R = ["Every resource carries the `owner` and `cost-centre` tags.",
  "Run `terraform fmt` and `terraform validate` before proposing a change.",
  "Pin provider versions with `~>` constraints."];
const U = ["Commit messages use the imperative mood and stay under 72 characters in the first line.",
  "Run `npm test` before saying a task is done.",
  "Do not add dependencies without asking."];
const GROUPS: Record<string, string[]> = { U, T, A, R };
const P1 = "I prefer short answers with no preamble.";
const P2 = "My sandbox API is at http://localhost:4010 with the dev token from my shell profile.";
const FILES = ["src/api/users.ts", "src/api/orders.ts", "src/api/status.ts", "src/ui/Button.tsx", "src/ui/Button.test.tsx", "src/api/users.test.ts", "tools/cli/run.test.ts",
  "terraform/prod/main.tf", "terraform/modules/net/vpc.tf", "db/migrations/0001_init.sql", "README.md", "package.json"];
const TESTS = ["src/ui/Button.test.tsx", "src/api/users.test.ts", "tools/cli/run.test.ts"];

function read(rel: string): string {
  const path = join(ROOT, rel);
  assert.ok(existsSync(path) && statSync(path).isFile(), `${rel} is missing`);
  return readFileSync(path, "utf8");
}

function walk(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => (e.isDirectory() ? walk(join(dir, e.name)) : [join(dir, e.name)]));
}

function ruleFiles(): string[] {
  const folder = join(ROOT, ".claude", "rules");
  return existsSync(folder) ? walk(folder).filter((p) => p.endsWith(".md")).map((p) => relative(ROOT, p)).sort() : [];
}

/** The `paths` of a rule as Claude Code reads it: a YAML list or a comma-separated string; null when there are none. */
function pathsOf(text: string): string[] | null {
  const [meta] = splitFrontmatter(text);
  const value = meta.paths;
  if (value === undefined || value === null) return null;
  return typeof value === "string" ? value.split(",").map((p: string) => p.trim()) : [...value];
}

function ruleBodies(): Record<string, [string[] | null, string]> {
  const out: Record<string, [string[] | null, string]> = {};
  for (const rel of ruleFiles()) out[rel] = [pathsOf(read(rel)), splitFrontmatter(read(rel))[1]];
  return out;
}

/** Every Markdown file outside .claude/, by project-relative path: what an @import can reach. */
function projectTexts(): Record<string, string> {
  const out: Record<string, string> = {};
  for (const p of walk(ROOT)) {
    const rel = relative(ROOT, p).split("\\").join("/");
    if (rel.endsWith(".md") && !rel.split("/").includes(".claude")) out[rel] = readFileSync(p, "utf8");
  }
  return out;
}

/** CLAUDE.md and everything it imports: all of it is in context from the first message. */
function launchText(): string {
  const texts = projectTexts();
  return ["CLAUDE.md", ...importsOf("CLAUDE.md", texts)].map((rel) => texts[rel]).join("\n");
}

function contextFor(touched: string[]): string {
  const bodies = ruleBodies();
  const paths: Record<string, string[] | null> = {};
  for (const [rel, [p]] of Object.entries(bodies)) paths[rel] = p;
  return [launchText(), ...rulesLoaded(paths, touched).map((rel: string) => bodies[rel][1])].join("\n");
}

function groupsIn(text: string): string[] {
  const found: string[] = [];
  for (const [name, lines] of Object.entries(GROUPS)) {
    const have = lines.map((line) => text.includes(line));
    assert.ok(have.every(Boolean) || !have.some(Boolean), `group ${name} is only partly there: move whole conventions, not some of them`);
    if (have.every(Boolean)) found.push(name);
  }
  return found;
}

test("m1 the conventions of each area load for exactly the files that area governs", () => {
  const table: Array<[string, string[]]> = [["src/api/users.ts", ["U", "A"]], ["src/ui/Button.tsx", ["U"]], ["src/ui/Button.test.tsx", ["U", "T"]],
    ["src/api/users.test.ts", ["U", "T", "A"]], ["terraform/prod/main.tf", ["U", "R"]], ["README.md", ["U"]], ["tools/cli/run.test.ts", ["U", "T"]]];
  const got = table.map(([file]) => groupsIn(contextFor([file])).sort());
  assert.deepEqual(got, table.map(([, want]) => [...want].sort()), JSON.stringify(table.map(([file], i) => [file, got[i]])));
});

test("e1 the root file is short and holds only what every task needs", () => {
  const root = read("CLAUDE.md");
  assert.ok(root.split("\n").length - (root.endsWith("\n") ? 1 : 0) <= 50, "the root file is long: it is read in every session, so only what every task needs belongs in it");
  assert.ok(U.every((line) => root.includes(line)), "the three rules that every task needs stay in the root file");
  for (const name of ["T", "A", "R"]) assert.ok(!GROUPS[name].some((line) => launchText().includes(line)), `the ${name} conventions are in the launch context: an import loads at launch too`);
});

test("e2 the testing rule follows the file type and not the folder", () => {
  const testing = Object.values(ruleBodies()).filter(([, body]) => body.includes(T[0])).map(([paths]) => paths);
  assert.ok(testing.length === 1 && testing[0] && testing[0].length > 0, "put the testing conventions in one rule file with paths");
  const paths = testing[0] as string[];
  for (const file of TESTS) assert.ok(paths.some((p) => globMatch(p, file)), `${file} is a test file and the rule must cover it`);
  for (const file of FILES.filter((f) => !TESTS.includes(f))) assert.ok(!paths.some((p) => globMatch(p, file)), `${file} is not a test file`);
});

test("e3 the import names a file that exists and loads at launch", () => {
  read("CLAUDE.md");
  const texts = projectTexts();
  assert.deepEqual(unresolvedImports("CLAUDE.md", texts), [], "an import that names no file imports nothing: check the spelling");
  assert.deepEqual(importsOf("CLAUDE.md", texts), ["docs/standards/architecture.md"], "import the architecture notes with @docs/standards/architecture.md outside a code span");
});

test("e4 personal lines sit in personal files and the local file is ignored", () => {
  const user = read("user-memory.example.md");
  const local = read("CLAUDE.local.example.md");
  assert.ok(user.includes(P1) && !user.includes(P2) && local.includes(P2) && !local.includes(P1), "a preference of yours goes in the user file, a sandbox note in the local file");
  const shared = ["CLAUDE.md", "docs/standards/architecture.md", ...ruleFiles()].map(read);
  assert.ok(!shared.some((text) => text.includes(P1) || text.includes(P2)), "a teammate would load your personal lines from the shared files");
  assert.ok(read(".gitignore").split("\n").map((l) => l.trim()).includes("CLAUDE.local.md"), "CLAUDE.local.md must be ignored by git");
});

test("e5 a rule that must always hold is a permission rule and not a sentence", () => {
  const settings = JSON.parse(read(".claude/settings.json"));
  assert.equal(decide(settings, ruleTool("Edit"), "db/migrations/0001_init.sql"), "deny", "memory is context, not enforcement: deny the edit in settings");
  assert.notEqual(decide(settings, ruleTool("Edit"), "src/api/users.ts"), "deny", "only the migrations are denied");
});

test("e6 every rule scopes itself with paths that are valid and match something", () => {
  const bodies = Object.entries(ruleBodies());
  assert.ok(bodies.length >= 3, "write the three rule files: testing, API and Terraform");
  for (const [rel, [paths]] of bodies) {
    assert.ok(paths && paths.length > 0, `${rel} has no usable paths: without them (or with frontmatter that does not parse) the rule loads in every session`);
    for (const pattern of paths as string[]) {
      const count = (c: string) => pattern.split(c).length - 1;
      assert.ok(count("{") === count("}") && count("[") === count("]"), `${rel}: the pattern '${pattern}' is not balanced`);
      assert.ok(FILES.some((f) => globMatch(pattern, f)), `${rel}: the pattern '${pattern}' matches no file of the project: a bare folder name is not a glob`);
    }
  }
});

test("e7 no file holds a personal path an address or a key", () => {
  const hits: string[] = [];
  for (const path of walk(ROOT).sort()) {
    const text = readFileSync(path, "utf8");
    for (const [label, pattern] of [["home path", /(\/home\/\w+|\/Users\/\w+|C:\\Users)/], ["email address", /[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+/], ["key", /sk-ant-[\w-]{6,}/]] as Array<[string, RegExp]>) {
      if (pattern.test(text)) hits.push(`${relative(ROOT, path)}: ${label}`);
    }
  }
  assert.deepEqual(hits, []);
});
