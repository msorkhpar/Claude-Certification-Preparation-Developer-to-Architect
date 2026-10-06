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
const { parseYaml, splitFrontmatter } = await import(example("40-workflow-lint/typescript/miniyaml.ts"));
const { lint } = await import(example("40-workflow-lint/typescript/workflowLint.ts"));

function read(rel: string): string {
  const path = join(ROOT, rel);
  assert.ok(existsSync(path) && statSync(path).isFile(), `${rel} is missing`);
  return readFileSync(path, "utf8");
}

function workflow(rel: string): [string, any] {
  const text = read(rel);
  let wf: any;
  try {
    wf = parseYaml(text);
  } catch (error) {
    assert.fail(`${rel} is not readable YAML: ${(error as Error).message}`);
  }
  assert.ok(wf && typeof wf === "object" && wf.jobs && typeof wf.jobs === "object", `${rel} needs a jobs map`);
  return [text, wf];
}

function claudeStep(job: any): any {
  const steps = (job.steps ?? []).filter((s: any) => String(s.uses ?? "").startsWith("anthropics/claude-code-action"));
  assert.equal(steps.length, 1, "one claude-code-action step");
  return steps[0];
}

test("m1 the mention workflow answers only claude comments and holds no key", () => {
  const [text, wf] = workflow(".github/workflows/claude.yml");
  const on = wf.on;
  assert.ok(on && typeof on === "object" && !Array.isArray(on) && JSON.stringify(Object.keys(on).sort()) === JSON.stringify(["issue_comment", "pull_request_review_comment"]), "trigger on issue comments and review comments");
  assert.ok(Object.values<any>(on).every((v) => JSON.stringify(v?.types) === JSON.stringify(["created"])), "only new comments");
  const job = wf.jobs.claude;
  assert.ok(job, "the job is called claude");
  assert.ok(String(job.if ?? "").includes("@claude") && String(job.if).includes("github.event.comment.body"), "start the runner only for comments that mention @claude");
  assert.ok(String(job.steps?.[0]?.uses ?? "").startsWith("actions/checkout@v"), "check the repository out first");
  const step = claudeStep(job);
  assert.equal(step.uses, "anthropics/claude-code-action@v1");
  assert.equal(step.with?.anthropic_api_key, "${{ secrets.ANTHROPIC_API_KEY }}");
  const perms = job.permissions ?? {};
  assert.deepEqual(["contents", "pull-requests", "issues", "id-token"].map((k) => perms[k]), ["write", "write", "write", "write"]);
  assert.deepEqual(lint(text), []);
});

test("e1 the review workflow reads the code and posts the review", () => {
  const [text, wf] = workflow(".github/workflows/review.yml");
  const on = wf.on;
  assert.ok(on && typeof on === "object" && JSON.stringify(Object.keys(on)) === JSON.stringify(["pull_request"]) && JSON.stringify(on.pull_request?.types) === JSON.stringify(["opened", "synchronize", "ready_for_review", "reopened"]));
  const job = wf.jobs.review;
  assert.ok(job, "the job is called review");
  const perms = job.permissions ?? {};
  assert.ok(perms.contents === "read" && perms["id-token"] === "write", "a review reads the code");
  const uses = (job.steps ?? []).map((s: any) => String(s.uses ?? ""));
  assert.ok(uses[0]?.startsWith("actions/checkout@v") && uses[1] === "anthropics/claude-code-action@v1");
  const w = claudeStep(job).with ?? {};
  assert.ok(String(w.prompt).includes("/code-review") && String(w.prompt).includes("--comment"), "run the review skill and post its findings");
  assert.ok(String(w.claude_args).includes("mcp__github_inline_comment__create_inline_comment"), "name the inline comment tool in claude_args");
  assert.ok(/^code-review@[\w-]+$/.test(String(w.plugins)) && w.plugin_marketplaces, "install the review plugin from a marketplace");
  assert.deepEqual(lint(text), []);
});

test("e2 each prompt file carries a version that its changelog explains", () => {
  const dir = join(ROOT, "prompts");
  const prompts = existsSync(dir) ? readdirSync(dir).filter((n) => n.endsWith(".md") && n !== "CHANGELOG.md").sort() : [];
  assert.ok(prompts.length > 0, "prompts/ holds at least one prompt");
  const headings = [...read("prompts/CHANGELOG.md").matchAll(/^## (\S+)\s*$/gm)].map((m) => m[1]);
  assert.ok(headings.length > 0, "the changelog has one ## heading per version, newest first");
  for (const name of prompts) {
    const [fm, body] = splitFrontmatter(readFileSync(join(dir, name), "utf8"));
    const version = String(fm.version ?? "");
    assert.ok(/^\d+\.\d+\.\d+$/.test(version), `${name} needs a semantic version in its frontmatter`);
    assert.equal(headings[0], version, `the newest changelog entry (${headings[0]}) must match ${name} (${version})`);
    assert.ok(body.trim() !== "");
  }
});

test("e3 the review guidance and the git workflow are files the reviewer and claude read", () => {
  const review = read("REVIEW.md");
  for (const heading of ["Always check", "Skip"]) {
    const m = new RegExp(`^## ${heading}\\s*\\n([\\s\\S]*?)(?=^## |$(?![\\s\\S]))`, "m").exec(review);
    assert.ok(m, `REVIEW.md needs a '## ${heading}' section`);
    const section = (m as RegExpExecArray)[1];
    assert.ok((section.match(/^- .+$/gm) ?? []).length >= 2, `'${heading}' needs at least two bullets`);
    assert.ok(/`[\w./*-]+\/`|`[\w./*-]+\.\w+`/.test(section), `'${heading}' names a path in backticks`);
  }
  const memory = read("CLAUDE.md");
  assert.ok(/`feature\/<ticket>`/.test(memory) && /commit message/i.test(memory) && /pull request/i.test(memory), "CLAUDE.md states the branch name, the commit message and the pull request rule");
  assert.ok(/never commit to `main`/i.test(memory));
});

test("e4 every run is bounded by turns time and concurrency", () => {
  for (const [rel, jobName] of [[".github/workflows/claude.yml", "claude"], [".github/workflows/review.yml", "review"]]) {
    const [, wf] = workflow(rel);
    const job = wf.jobs[jobName];
    assert.ok(Number.isInteger(job?.["timeout-minutes"]) && job["timeout-minutes"] >= 1 && job["timeout-minutes"] <= 30, `${rel}: a timeout of 30 minutes or less`);
    const turns = /--max-turns (\d+)/.exec(String(claudeStep(job).with?.claude_args ?? ""));
    assert.ok(turns && Number(turns[1]) >= 1 && Number(turns[1]) <= 10, `${rel}: --max-turns of 10 or fewer`);
    assert.ok(wf.concurrency?.group, `${rel}: a concurrency group`);
  }
  assert.equal(workflow(".github/workflows/review.yml")[1].concurrency["cancel-in-progress"], true, "a new push replaces a running review");
});

test("e5 no file holds a key a personal path or an address", () => {
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
