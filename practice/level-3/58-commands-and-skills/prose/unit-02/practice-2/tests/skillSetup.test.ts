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
const { commandName, forkAgent, invocation, parse, preApproved, render, toolStatus } = await import(example("58-skill-model/typescript/skillModel.ts"));

const REVIEW = ".claude/skills/review-pr/SKILL.md";
const TAG = ".claude/skills/release-tag/SKILL.md";
const STANDUP = ".claude/commands/standup.md";
const MINE = "personal/review-pr-mine/SKILL.md";
const ALL = [REVIEW, TAG, STANDUP, MINE];

function read(rel: string): string {
  const path = join(ROOT, rel);
  assert.ok(existsSync(path) && statSync(path).isFile(), `${rel} is missing`);
  return readFileSync(path, "utf8");
}

const load = (rel: string): [any, string] => parse(read(rel));

function walk(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => (e.isDirectory() ? walk(join(dir, e.name)) : [join(dir, e.name)]));
}

test("m1 the review skill is a forked skill with an explicit task and an argument", () => {
  const [meta, body] = load(REVIEW);
  assert.ok(forkAgent(meta), "a review that prints a lot belongs in a forked context: set context: fork");
  assert.ok(String(meta.agent ?? "").trim(), "name the subagent type in agent");
  assert.match(body, /^1\. /m, "a forked skill is given its content as the task: write numbered steps, not guidelines");
  assert.ok(String(meta["argument-hint"] ?? "").trim(), "show what to type with argument-hint");
  assert.match(body, /\$0|\$ARGUMENTS/, "use the pull request number in the steps with $0");
});

test("e1 the release skill is started only by a person and pre approves patterns", () => {
  const [meta] = load(TAG);
  assert.deepEqual(invocation(meta), { you: true, claude: false, description_in_context: false }, "a skill with side effects sets disable-model-invocation: true");
  const items = String(meta["allowed-tools"] ?? "").match(/[^\s,(]+(?:\([^)]*\))?/g) ?? [];
  assert.ok(items.length > 0 && items.every((i) => i.includes("(")), "pre-approve patterns such as Bash(git tag *), never a whole tool");
  assert.ok(preApproved(meta, "Bash", "git tag -a v1.2.0 -m x") && preApproved(meta, "Bash", "git push origin v1.2.0"));
  assert.ok(!preApproved(meta, "Bash", "git push --force origin main") && !preApproved(meta, "Bash", "rm -rf build"));
});

test("e2 tools are taken away with disallowed tools and allowed tools only pre approves", () => {
  const [meta] = load(REVIEW);
  assert.ok(toolStatus(meta, "Edit") === "removed" && toolStatus(meta, "Write") === "removed", "list Edit and Write by bare name in disallowed-tools");
  assert.notEqual(toolStatus(meta, "Read"), "removed", "only the tools that change things are removed");
  assert.ok(preApproved(meta, "Bash", "gh pr diff 12") && preApproved(meta, "Bash", "gh pr view 12"));
  assert.ok(!preApproved(meta, "Bash", "gh pr merge 12") && !preApproved(meta, "Bash", "rm -rf build"), "allowed-tools pre-approves the read-only gh commands and no more");
});

test("e3 arguments fill the placeholders of every file", () => {
  let [, body] = load(REVIEW);
  let out = render(body, "123", []);
  assert.ok(!out.includes("$0") && out.includes("gh pr diff 123") && !out.includes("ARGUMENTS:"));
  let meta: any;
  [meta, body] = load(TAG);
  assert.deepEqual(meta.arguments, ["version"], "declare the named argument in arguments");
  out = render(body, "v1.4.0", meta.arguments);
  assert.ok(!out.includes("$version") && out.includes('git tag -a v1.4.0 -m "Release v1.4.0"') && !out.includes("ARGUMENTS:"));
  [, body] = load(STANDUP);
  out = render(body, "ana");
  assert.ok(!out.includes("$ARGUMENTS") && out.includes("by ana since") && !out.includes("ARGUMENTS:"));
});

test("e4 every file creates its own slash command and the personal variant has a new name", () => {
  const names: Record<string, string> = {};
  for (const rel of ALL) names[rel] = commandName(rel, load(rel)[0]);
  assert.ok(names[STANDUP] === "standup" && names[REVIEW] === "review-pr" && names[TAG] === "release-tag");
  assert.equal(new Set(Object.values(names)).size, 4, `two files create one command: ${JSON.stringify(names)}`);
  assert.notEqual(names[MINE], "review-pr", "a personal skill with the team's name replaces it for you: give the variant its own name");
  const [meta] = load(STANDUP);
  assert.ok(String(meta.description ?? "").trim() && String(meta["argument-hint"] ?? "").trim(), "the old command file keeps working: give it a description and a hint");
});

test("e5 each piece of guidance lives where it loads the way it is used", () => {
  const rows: Record<string, string> = {};
  for (const line of read("docs/placement.md").split("\n")) {
    const cells = line.trim().replace(/^\||\|$/g, "").split("|").map((c) => c.trim().replace(/^`|`$/g, ""));
    if (cells.length === 2 && !["Guidance", "---"].includes(cells[0])) rows[cells[0]] = cells[1];
  }
  const want = {
    "The team's pull request review checklist, run on demand": ".claude/skills/review-pr/SKILL.md",
    "My own variant of that review with extra style notes": "~/.claude/skills/review-pr-mine/SKILL.md",
    "Coding standards that apply to every task in the repository": "CLAUDE.md",
    "Test file conventions for test files in many folders": ".claude/rules/testing.md",
    "A release procedure with side effects that only a person starts": ".claude/skills/release-tag/SKILL.md",
  };
  assert.deepEqual(rows, want);
});

test("e6 every skill says when to use it and stays inside the listing budget", () => {
  for (const rel of [REVIEW, TAG, MINE]) {
    const [meta] = load(rel);
    const description = String(meta.description ?? "");
    assert.match(description, /(?:^|\. )Use when\b/, `${rel}: the description starts a sentence with Use when`);
    assert.ok(description.length + String(meta.when_to_use ?? "").length <= 1536, `${rel}: the listing keeps 1,536 characters of description and when_to_use`);
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
