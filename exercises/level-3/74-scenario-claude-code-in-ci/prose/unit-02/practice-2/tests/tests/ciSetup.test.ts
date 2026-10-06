import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative, resolve } from "node:path";

// The solution is the file beside this test.
const ROOT = resolve((import.meta.dirname + "/../project"));

const VAGUE = ["be conservative", "be careful", "only report important", "high confidence", "use good judgement", "use your judgment"];

function words(command: string): string[] {
  const out: string[] = [];
  let word = "", quoted = false, started = false;
  for (let i = 0; i < command.length; i++) {
    const c = command[i];
    if (c === "\\" && i + 1 < command.length) { word += command[++i]; started = true; }
    else if (c === '"') { quoted = !quoted; started = true; }
    else if (c === " " && !quoted) { if (started) out.push(word); word = ""; started = false; }
    else { word += c; started = true; }
  }
  if (started) out.push(word);
  return out;
}

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

const jobs = (): any[] => loadJson("ci/pipeline.json").jobs ?? [];

function job(name: string): any {
  const found = jobs().find((j) => j.name === name);
  assert.ok(found, `ci/pipeline.json has no job named ${name}`);
  return found;
}

function section(text: string, title: string): string[] {
  const match = text.match(new RegExp(`^## ${title}\\s*\\n([\\s\\S]*?)(?=^## |$(?![\\s\\S]))`, "m"));
  return match ? match[1].split("\n").filter((l) => l.startsWith("- ")).map((l) => l.slice(2).trim()) : [];
}

test("m1 who waits decides between real time and batch", () => {
  assert.equal(job("pre-merge-review").api, "realtime", "a developer waits for the merge check, so it runs in real time");
  assert.equal(job("debt-report").api, "batch", "nobody waits for the overnight report, so it runs as a batch");
  for (const j of jobs()) assert.equal(j.api, j.audience === "waiting" ? "realtime" : "batch", `${j.name}: the audience is ${j.audience}`);
});

test("e1 a review has a pass for each file and then an integration pass", () => {
  assert.deepEqual(job("pre-merge-review").passes, ["per-file", "integration"], "one pass per file for local issues, then one pass across files, in that order");
});

test("e2 a review runs in a fresh session and is given the earlier findings", () => {
  const review = job("pre-merge-review");
  assert.equal(review.session, "fresh", "the session that wrote the code is biased toward it");
  assert.ok(review.context.includes("prior_findings"), "a re-run needs the earlier findings so that it reports only what is new");
});

test("e3 every claude command is headless json and bounded", () => {
  for (const j of jobs()) {
    const tokens = words(j.command);
    if (tokens[0] !== "claude") continue;
    assert.ok(tokens.includes("-p") || tokens.includes("--print"), `${j.name}: without -p the run waits for input`);
    assert.ok(tokens.includes("--output-format") && tokens[tokens.indexOf("--output-format") + 1] === "json", `${j.name}: ask for json output`);
    assert.ok(tokens.includes("--max-turns") && /^\d+$/.test(tokens[tokens.indexOf("--max-turns") + 1] ?? ""), `${j.name}: bound the run with --max-turns`);
  }
  const tokens = words(job("pre-merge-review").command);
  assert.ok(tokens.includes("--json-schema"), "the review answers in a schema");
  const item = loadJson(tokens[tokens.indexOf("--json-schema") + 1]).properties.findings.items;
  assert.ok(item.properties.severity.enum, "severity is a closed list in the schema");
  for (const field of ["file", "line", "severity", "issue", "suggestion"]) assert.ok((item.required ?? []).includes(field), "a finding is required to say where, how bad, what and what to do");
});

test("e4 the review can only read", () => {
  const review = job("pre-merge-review");
  const allowed = ["Read", "Grep", "Glob"];
  assert.ok(review.tools.length > 0 && review.tools.every((t: string) => allowed.includes(t)), `read-only tools only, found ${review.tools}`);
  const tokens = words(review.command);
  assert.ok(tokens.includes("--allowedTools"), "list the allowed tools in the command");
  assert.ok(tokens[tokens.indexOf("--allowedTools") + 1].split(",").every((t) => allowed.includes(t)), "the command approves read tools only");
});

test("e5 the criteria name what to report what to skip and an example for each severity", () => {
  const text = read("ci/review-criteria.md");
  assert.ok(section(text, "Report").length >= 2, "list at least two kinds of issue to report");
  assert.ok(section(text, "Skip").length >= 2, "list at least two kinds of issue to skip");
  const levels = new Map(section(text, "Severity").map((l) => [l.split(":")[0], l]));
  for (const level of ["high", "medium", "low"]) assert.ok(levels.has(level) && levels.get(level)!.includes("Example:"), `${level} needs a description and an Example:`);
  assert.deepEqual(VAGUE.filter((p) => text.toLowerCase().includes(p)), [], "a vague instruction does not make a review more precise: name the cases");
});

test("e6 the test prompt passes the existing tests and says what a useful test is", () => {
  const text = read("ci/testgen-prompt.md");
  assert.ok(text.includes("{{existing_tests}}") && text.includes("{{changed_files}}"), "the prompt carries the changed files and the existing tests");
  assert.ok(section(text, "A useful test").length >= 3, "say what a useful test is, in at least three points");
  assert.ok(section(text, "Do not write").length >= 2, "say what not to write, in at least two points");
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
