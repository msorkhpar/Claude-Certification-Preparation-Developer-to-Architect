import { test } from "node:test";
import assert from "node:assert/strict";
import { REVIEW_SCHEMA, buildCommand, envelope, gate, lintCommand, schemaCheck } from "./ciGate.ts";

const POLICY = { min_severity: "medium", disabled_categories: ["style"], fail_on: ["high"] };
const finding = (over = {}) => ({ file: "api.py", line: 12, category: "bug", severity: "medium", issue: "Unchecked None.", suggested_fix: "Return early.", detected_pattern: "missing-none-check", ...over });
const run = (findings: unknown[], code = 0, over = {}) => gate(envelope({ structured_output: { findings }, ...over }), code, REVIEW_SCHEMA, POLICY);
const quote = (a: string) => (/^[\w@%+=:,./-]+$/.test(a) ? a : `'${a.replace(/'/g, "'\\''")}'`);

test("the command is headless json schema checked bounded and read only", () => {
  const argv = buildCommand("Review.", { type: "object" });
  assert.deepEqual(argv.slice(0, 3), ["claude", "--bare", "-p"]);
  assert.equal(argv[argv.indexOf("--output-format") + 1], "json");
  assert.deepEqual(JSON.parse(argv[argv.indexOf("--json-schema") + 1]), { type: "object" });
  assert.equal(argv[argv.indexOf("--max-turns") + 1], "8");
  assert.equal(argv[argv.indexOf("--allowedTools") + 1], "Read,Grep,Glob,Bash(git diff *)");
  assert.deepEqual(lintCommand(argv.map(quote).join(" ")), []);
});

test("lint names what a careless step lacks", () => {
  assert.deepEqual(lintCommand("claude 'Review it'"), ["no-print", "no-json", "no-schema", "no-turn-limit", "no-tool-list", "no-bare"]);
  assert.ok(lintCommand("claude -p x --allowedTools Bash,Read").includes("wide-tools") && lintCommand("claude -p x --allowedTools 'Read,Edit'").includes("wide-tools"));
  assert.ok(!lintCommand("claude -p x --allowedTools 'Bash(git diff *)'").includes("wide-tools"));
  assert.ok(lintCommand("claude --bare -p x --output-format json --json-schema '{}' --max-turns 5 --allowedTools Read").includes("bare-without-context"));
  assert.ok(lintCommand("claude -p x --max-turns 50").includes("no-turn-limit"));
  assert.deepEqual(lintCommand("no tool here"), ["no-claude-command"]);
});

test("the schema check reads types enums required and extra keys", () => {
  assert.deepEqual(schemaCheck({ findings: [] }, REVIEW_SCHEMA), []);
  assert.deepEqual(schemaCheck({ findings: [finding({ line: "12" })] }, REVIEW_SCHEMA), ["$.findings[0].line: expected integer"]);
  assert.ok(schemaCheck({ findings: [finding({ severity: "critical" })] }, REVIEW_SCHEMA)[0].startsWith('$.findings[0].severity: "critical" is not one of'));
  assert.deepEqual(schemaCheck({ findings: [{ ...finding(), confidence: 0.9 }] }, REVIEW_SCHEMA), ["$.findings[0].confidence: is not allowed"]);
});

test("a good run posts the findings above the floor and outside the disabled categories", () => {
  const result = run([finding(), finding({ category: "style", severity: "medium" }), finding({ line: 3, severity: "low" })]);
  assert.deepEqual(result, { exit: 0, comments: [{ file: "api.py", line: 12, severity: "medium", body: "Unchecked None. Suggested fix: Return early." }], problems: [] });
});

test("a high finding fails the job and no findings pass it", () => {
  assert.equal(run([finding({ severity: "high" })]).exit, 1);
  assert.equal(run([finding({ severity: "high" })]).comments[0].severity, "high");
  assert.equal(run([]).exit, 0);
});

test("a failed run fails the job instead of passing it silently", () => {
  const cases: Array<[string, number, string]> = [[envelope({ subtype: "error_max_turns", is_error: true }), 1, "error_max_turns"],
    [envelope({ subtype: "error_max_structured_output_retries", is_error: true }), 1, "retries"], [envelope(), 0, "structured_output"], ["Error: no key", 1, "not a JSON"], ["[1]", 0, "not a JSON"]];
  for (const [out, code, word] of cases) {
    const result = gate(out, code, REVIEW_SCHEMA, POLICY);
    assert.ok(result.exit === 1 && result.comments.length === 0 && result.problems.some((p) => p.includes(word)), JSON.stringify([out, result]));
  }
  assert.ok(gate(envelope({ structured_output: { findings: [] } }), 2, REVIEW_SCHEMA, POLICY).problems[0].includes("exited with status 2"));
});
