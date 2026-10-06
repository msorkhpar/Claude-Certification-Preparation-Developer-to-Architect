import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative, resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution: the project folder that holds the workflow, CLAUDE.md, the schema and the gate.
const ROOT = resolve(process.env.SOLUTION_DIR ?? "starter");
const example = (file: string) => {
  const candidates = [`/w/examples/${file}`];
  for (let d = resolve("."), i = 0; i < 9; i++, d = resolve(d, "..")) candidates.push(join(d, "examples", file));
  return pathToFileURL(candidates.find((c) => existsSync(c)) as string).href;
};
const { REVIEW_SCHEMA, envelope, lintCommand } = await import(example("60-ci-gate/typescript/ciGate.ts"));
const { parseYaml } = await import(example("40-workflow-lint/typescript/miniyaml.ts"));
const { gate, reviewPrompt } = await import(pathToFileURL(resolve(ROOT, "reviewGate.ts")).href);
const { schemaCheck } = await import(pathToFileURL(resolve(ROOT, "schemaCheck.ts")).href);

const POLICY = { min_severity: "medium", disabled_categories: ["style"], fail_on: ["high"] };
const VAGUE = ["be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment"];

function read(rel: string): string {
  const path = join(ROOT, rel);
  assert.ok(existsSync(path) && statSync(path).isFile(), `${rel} is missing`);
  return readFileSync(path, "utf8");
}

function walk(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => (e.isDirectory() ? walk(join(dir, e.name)) : [join(dir, e.name)]));
}

const finding = (over: Record<string, unknown> = {}): any => ({ file: "api.py", line: 12, category: "bug", severity: "medium", issue: "Unchecked None.", suggested_fix: "Return early.", detected_pattern: "missing-none-check", ...over });

function run(findings: unknown[], code = 0, policy: any = POLICY, over: Record<string, unknown> = {}): any {
  const result = gate(envelope({ structured_output: { findings }, ...over }), code, REVIEW_SCHEMA, policy);
  assert.ok(result && typeof result === "object", "gate returned nothing");
  return result;
}

test("m1 a valid run posts the findings above the floor and outside the disabled categories", () => {
  const result = run([finding(), finding({ category: "style", severity: "medium" }), finding({ line: 3, severity: "low" }), finding({ file: "ui.py", line: 7, category: "security", severity: "medium", issue: "Unescaped input.", suggested_fix: "Escape it." })]);
  assert.deepEqual(result, { exit: 0, problems: [], comments: [{ file: "api.py", line: 12, severity: "medium", body: "Unchecked None. Suggested fix: Return early." },
    { file: "ui.py", line: 7, severity: "medium", body: "Unescaped input. Suggested fix: Escape it." }] });
  assert.deepEqual(run([]), { exit: 0, comments: [], problems: [] });
});

test("e1 a failed run fails the job instead of passing it silently", () => {
  const cases: Array<[string, number, string]> = [[envelope({ subtype: "error_max_turns", is_error: true }), 1, "error_max_turns"], [envelope({ subtype: "error_max_structured_output_retries", is_error: true }), 1, "retries"],
    [envelope(), 0, "structured_output"], [envelope({ is_error: true, structured_output: { findings: [] } }), 0, "ended"], ["Error: no key", 1, "not a JSON"], ["[1]", 0, "not a JSON"], ["", 0, "not a JSON"]];
  for (const [out, code, word] of cases) {
    const result = gate(out, code, REVIEW_SCHEMA, POLICY);
    assert.ok(result && result.exit === 1 && result.comments.length === 0 && result.problems.some((p: string) => p.includes(word)), JSON.stringify([out, result]));
  }
  const failed = run([], 2);
  assert.ok(failed.exit === 1 && failed.problems.some((p: string) => p.includes("exited with status 2")), JSON.stringify(failed));
});

test("e2 an answer that breaks the schema fails the job and names the path of the problem", () => {
  const { file, ...noFile } = finding();
  const table: Array<[unknown[], string]> = [[[finding({ line: "12" })], "$.findings[0].line"], [[finding({ severity: "critical" })], "$.findings[0].severity"], [[noFile], "$.findings[0].file"], [[{ ...finding(), confidence: 0.9 }], "$.findings[0].confidence"]];
  for (const [bad, path] of table) {
    const result = run(bad);
    assert.ok(result.exit === 1 && result.comments.length === 0 && result.problems.some((p: string) => p.startsWith(`schema ${path}`)), JSON.stringify([bad, result]));
  }
});

test("e3 a finding at the failing severity blocks the merge and lower ones only comment", () => {
  const blocked = run([finding({ severity: "high" }), finding({ line: 30 })]);
  assert.ok(blocked.exit === 1 && blocked.problems.length === 0);
  assert.deepEqual(blocked.comments.map((c: any) => c.severity), ["high", "medium"]);
  assert.equal(run([finding({ severity: "medium" })]).exit, 0);
  assert.equal(run([finding({ severity: "medium" })], 0, { ...POLICY, fail_on: ["medium", "high"] }).exit, 1);
  assert.equal(run([finding({ severity: "high", category: "style" })]).exit, 0, "a disabled category cannot block");
});

test("e4 the prompt lists earlier findings and existing tests and asks for new or unaddressed issues only", () => {
  const prior = [{ file: "api.py", line: 12, category: "bug", issue: "Unchecked None." }, { file: "ui.py", line: 7, category: "security", issue: "Unescaped input." }];
  const full = reviewPrompt("+ x = 1", prior, ["test_empty_cart", "test_two_items"]);
  assert.equal(full, ["<instructions>", "Review the change in <diff> against the criteria in the project instructions.", "Report only findings that are new or still unaddressed.",
    "Do not repeat a finding listed in <already_reported>.", "Do not suggest a test for a behaviour that an existing test in <existing_tests> already covers.", "</instructions>",
    "<already_reported>", "- api.py:12 [bug] Unchecked None.", "- ui.py:7 [security] Unescaped input.", "</already_reported>", "<existing_tests>", "- test_empty_cart", "- test_two_items",
    "</existing_tests>", "<diff>", "+ x = 1", "</diff>"].join("\n"));
  assert.equal(reviewPrompt("+ x = 1"), ["<instructions>", "Review the change in <diff> against the criteria in the project instructions.", "Report only findings that are new or still unaddressed.", "</instructions>", "<diff>", "+ x = 1", "</diff>"].join("\n"));
});

function keywords(node: any): Set<string> {
  const found = new Set<string>();
  if (Array.isArray(node)) for (const v of node) for (const k of keywords(v)) found.add(k);
  else if (node && typeof node === "object") {
    for (const [k, v] of Object.entries(node)) {
      if (k === "properties" && v && typeof v === "object") for (const sub of Object.values(v as object)) for (const x of keywords(sub)) found.add(x);
      else {
        found.add(k);
        for (const x of keywords(v)) found.add(x);
      }
    }
  }
  return found;
}

test("e5 the schema file is valid draft 07 and requires every field of a finding", () => {
  let schema: any;
  try {
    schema = JSON.parse(read("review-schema.json"));
  } catch (error) {
    if (error instanceof SyntaxError) assert.fail(`review-schema.json is not valid JSON: ${error.message}`);
    throw error;
  }
  assert.equal(schema.$schema ?? "http://json-schema.org/draft-07/schema#", "http://json-schema.org/draft-07/schema#", "the SDK validates draft-07 and rejects a newer $schema");
  const found = keywords(schema);
  assert.ok(!["minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf", "minLength", "maxLength"].some((k) => found.has(k)), "structured outputs do not support numeric or string constraints");
  const items = schema.properties?.findings?.items ?? {};
  assert.deepEqual([...(items.required ?? [])].sort(), ["category", "detected_pattern", "file", "issue", "line", "severity", "suggested_fix"], "every field of a finding is required");
  assert.ok(items.additionalProperties === false && schema.additionalProperties === false);
  assert.deepEqual(schema.required, ["findings"]);
  const props = items.properties ?? {};
  assert.deepEqual([...(props.severity?.enum ?? [])].sort(), ["high", "low", "medium"]);
  assert.ok(["bug", "security", "style", "other"].every((c) => (props.category?.enum ?? []).includes(c)));
  assert.equal(props.line?.type, "integer");
  assert.deepEqual(schemaCheck({ findings: [finding()] }, schema), []);
  assert.deepEqual(schemaCheck({ findings: [] }, schema), []);
  const { detected_pattern, ...noPattern } = finding();
  for (const bad of [noPattern, finding({ severity: "critical" }), finding({ line: "12" }), { ...finding(), confidence: 0.9 }]) assert.ok(schemaCheck({ findings: [bad] }, schema).length > 0, `the schema accepted ${JSON.stringify(bad)}`);
});

test("e6 the workflow runs claude headless with json output a schema a turn limit read only tools and the context file", () => {
  const wf: any = parseYaml(read(".github/workflows/claude-review.yml"));
  const triggers = typeof wf.on === "object" && wf.on !== null && !Array.isArray(wf.on) ? Object.keys(wf.on) : [wf.on];
  assert.ok(triggers.includes("pull_request"), "run the review on pull requests");
  const job = wf.jobs.review;
  assert.ok(Number.isInteger(job["timeout-minutes"]) && job["timeout-minutes"] <= 30, "a stuck run must not hold the runner");
  assert.equal(job.permissions?.contents, "read", "a review reads the repository");
  const step = job.steps.find((s: any) => String(s.run ?? "").includes("claude"));
  assert.ok(step, "a step runs claude");
  assert.ok(String(step.env?.ANTHROPIC_API_KEY ?? "").startsWith("${{ secrets."), "the key comes from the secrets context");
  const command = String(step.run);
  assert.deepEqual(lintCommand(command), [], JSON.stringify(lintCommand(command)));
  assert.ok(Number(/--max-turns[ =](\d+)/.exec(command)?.[1]) <= 10);
  const tools = (/--allowed[Tt]ools[ =]("[^"]*"|\S+)/.exec(command)?.[1] ?? "").replace(/^"|"$/g, "").match(/[^\s,(]+(?:\([^)]*\))?/g) ?? [];
  assert.ok(tools.length > 0 && tools.every((t: string) => ["Read", "Grep", "Glob"].includes(t) || /^Bash\(git (diff|log|show|status)( \*)?\)$/.test(t)), `a review needs read-only tools: ${tools}`);
  assert.match(command, /--json-schema\s+"\$\(cat review-schema\.json\)"/, "pass the schema file to --json-schema");
  assert.match(command, /--append-system-prompt-file\s+CLAUDE\.md/, "--bare skips CLAUDE.md, so pass it by hand");
});

function section(text: string, title: string): string[] {
  const m = new RegExp(`^## ${title.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}\\s*\\n([\\s\\S]*?)(?=^## |$(?![\\s\\S]))`, "im").exec(text);
  assert.ok(m, `CLAUDE.md has no section '## ${title}'`);
  return (m as RegExpExecArray)[1].split("\n").filter((l) => l.startsWith("- ")).map((l) => l.slice(2));
}

test("e7 the project file states what to report and what to skip with a severity example for each level", () => {
  const text = read("CLAUDE.md");
  assert.deepEqual(VAGUE.filter((p) => text.toLowerCase().includes(p)), [], "a general instruction like be conservative does not improve precision: name the patterns");
  const report = section(text, "Report");
  const skip = section(text, "Skip");
  assert.ok(report.length >= 3 && report.some((r) => /bug/i.test(r)) && report.some((r) => /security/i.test(r)), "list at least three categories to report, among them bugs and security");
  assert.ok(skip.length >= 2 && skip.some((s) => /style/i.test(s)), "list what to skip, minor style among it");
  const severity = section(text, "Severity");
  for (const level of ["high", "medium", "low"]) {
    const line = severity.find((s) => s.toLowerCase().startsWith(`${level}:`));
    assert.ok(line && /`[^`]+`/.test(line), `severity ${level} needs a concrete example in code`);
  }
  assert.ok(section(text, "Testing standards").some((s) => s.includes("tests/fixtures/")), "name the fixtures folder in the testing standards");
});

test("e8 no file holds a personal path an address or a key", () => {
  const hits: string[] = [];
  for (const path of walk(ROOT).sort()) {
    const text = readFileSync(path, "utf8");
    for (const [label, pattern] of [["home path", /(\/home\/\w+|\/Users\/\w+|C:\\Users)/], ["email address", /[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+/], ["key", /sk-ant-[\w-]{6,}/]] as Array<[string, RegExp]>) {
      if (pattern.test(text)) hits.push(`${relative(ROOT, path)}: ${label}`);
    }
  }
  assert.deepEqual(hits, []);
});
