import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative, resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution: the project folder that holds CLAUDE.md, .claude/ and docs/.
const ROOT = resolve(process.env.SOLUTION_DIR ?? "starter");

const [UI, UI_TEST] = ["src/ui/Button.tsx", "src/ui/Button.spec.tsx"];
const [HANDLER, HANDLER_TEST] = ["server/handlers/orders.ts", "server/handlers/orders.spec.ts"];
const [DB, DB_TEST] = ["server/db/orderRepo.ts", "server/db/orderRepo.spec.ts"];
const DOC = "docs/readme.md";
// the words each convention is written with, the files it must reach and the files it must not reach
const AREAS: Array<[string, string[], string[]]> = [["hooks", [UI], [HANDLER, DB, DOC]], ["async/await", [HANDLER], [UI, DB, DOC]], ["repository", [DB], [UI, HANDLER, DOC]],
  ["describe", [UI_TEST, HANDLER_TEST, DB_TEST], [UI, HANDLER, DB, DOC]]];
const SAMPLES = [UI, UI_TEST, HANDLER, HANDLER_TEST, DB, DB_TEST, DOC];

function globRegex(glob: string): RegExp {
  let out = "";
  for (let i = 0; i < glob.length; ) {
    if (glob.startsWith("**/", i)) { out += "(?:.*/)?"; i += 3; }
    else if (glob.startsWith("**", i)) { out += ".*"; i += 2; }
    else if (glob[i] === "*") { out += "[^/]*"; i += 1; }
    else if (glob[i] === "?") { out += "[^/]"; i += 1; }
    else { out += glob[i].replace(/[.+^${}()|[\]\\]/g, "\\$&"); i += 1; }
  }
  return new RegExp(`^${out}$`);
}

function read(rel: string): string {
  const path = join(ROOT, rel);
  assert.ok(existsSync(path) && statSync(path).isFile(), `${rel} is missing`);
  return readFileSync(path, "utf8");
}

function frontMatter(text: string): [string, string] {
  const head = text.match(/^---\n([\s\S]*?)\n---\n/);
  return head ? [head[1], text.slice(head[0].length)] : ["", text];
}

function rulePaths(head: string): string[] | null {
  const listed = `${head}\n`.match(/^paths:\s*\n((?:[ \t]+-[ \t]+.*\n?)+)/m);
  return listed ? listed[1].split("\n").filter((l) => l.trim()).map((l) => l.replace(/^\s*-\s+/, "").trim().replace(/^["']|["']$/g, "")) : null;
}

function rules(): Array<[string, string, string[] | null]> {
  const folder = join(ROOT, ".claude/rules");
  if (!existsSync(folder)) return [];
  return readdirSync(folder).filter((n) => n.endsWith(".md")).sort().map((name) => {
    const text = readFileSync(join(folder, name), "utf8");
    return [name, text.toLowerCase(), rulePaths(frontMatter(text)[0])];
  });
}

const reaches = (paths: string[] | null, file: string) => paths === null || paths.some((g) => globRegex(g).test(file));

test("m1 each convention loads for exactly the files of its area", () => {
  for (const [marker, expected, forbidden] of AREAS) {
    const holders = rules().filter(([, text]) => text.includes(marker));
    assert.ok(holders.length > 0, `no rule file holds the '${marker}' convention`);
    for (const file of expected) assert.ok(holders.some(([, , p]) => reaches(p, file)), `the '${marker}' convention does not load for ${file}`);
    for (const file of forbidden) assert.ok(!holders.some(([, , p]) => reaches(p, file)), `the '${marker}' convention loads for ${file}, which is not its area`);
  }
});

test("e1 the root file is short and holds only what every task needs", () => {
  const text = read("CLAUDE.md");
  assert.ok(text.split("\n").filter((l) => l.trim()).length <= 25, "keep the root file to 25 lines that matter");
  assert.deepEqual(AREAS.filter(([m]) => text.toLowerCase().includes(m)).map(([m]) => m), [], "area conventions belong in rule files, not in the root file");
});

test("e2 the review command is shared read only and says what it does", () => {
  const [head, body] = frontMatter(read(".claude/commands/review.md"));
  assert.ok(/^description:\s*\S/m.test(head), "the command needs a description");
  const allowed = head.match(/^allowed-tools:\s*(.*)$/m);
  assert.ok(allowed, "the command lists the tools it pre-approves");
  const tools = allowed![1].match(/[^\s,(]+(?:\([^)]*\))?/g) ?? [];
  assert.ok(tools.includes("Read") && !tools.some((t) => ["Bash", "Edit", "Write", "MultiEdit"].includes(t)), "a review reads: no bare Bash, no edits");
  assert.ok(body.includes("git diff"), "the body says how to get the changes");
});

test("e3 the settings protect the environment file and approve no whole tool", () => {
  let perms: any;
  try {
    perms = JSON.parse(read(".claude/settings.json")).permissions ?? {};
  } catch (error) {
    if (error instanceof SyntaxError) assert.fail(`.claude/settings.json is not valid JSON: ${error.message}`);
    throw error;
  }
  assert.ok((perms.deny ?? []).includes("Read(./.env)"), "deny reading the environment file");
  assert.ok(!(perms.allow ?? []).some((r: string) => ["Bash", "Bash(*)", "Edit", "Write"].includes(r)), "a bare allow rule approves every call of that tool");
});

test("e4 the modes table sends open design work to plan mode and clear small work to direct", () => {
  const rows = new Map<string, string>();
  for (const line of read("docs/working-modes.md").split("\n")) {
    const cells = line.trim().replace(/^\||\|$/g, "").split("|").map((c) => c.trim());
    if (line.startsWith("|") && cells.length >= 2 && ["plan", "direct"].includes(cells[1].toLowerCase())) rows.set(cells[0].toLowerCase(), cells[1].toLowerCase());
  }
  for (const [keyword, mode] of [["typo", "direct"], ["monolith", "plan"], ["validation", "direct"], ["auth library", "plan"], ["rename", "direct"], ["unclear", "plan"]]) {
    const task = [...rows.keys()].find((t) => t.includes(keyword));
    assert.ok(task, `the table has no row for the '${keyword}' task`);
    assert.equal(rows.get(task!), mode, `'${keyword}' should be ${mode}`);
  }
});

test("e5 every rule scopes itself with a glob that matches a file", () => {
  const found = rules();
  assert.ok(found.length > 0, "write the rule files");
  for (const [name, , paths] of found) {
    assert.ok(paths && paths.length > 0, `${name} has no paths list, so it loads in every session`);
    assert.ok(SAMPLES.some((f) => reaches(paths, f)), `${name} matches none of the sample files`);
  }
});

test("e6 no file holds a personal path an address or a key", () => {
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
