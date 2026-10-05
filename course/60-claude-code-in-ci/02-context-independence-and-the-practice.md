# What the run knows: context, independence and the practice

**Level:** Architect · **Module 60:** Claude Code in CI · **Page 2 of 3**
**Exams:** A3.6; S5

**After this page you can** give a CI run the project context it needs, keep the reviewer independent of the session that wrote the code, put earlier findings and existing tests in the prompt so that a re-run reports only what is new, write review criteria that a run can follow, and write the module's practice.

Checked on 2026-10-03 against the Claude Code documentation pages "Run Claude Code programmatically", "Best practices for Claude Code" (fresh context for review) and "Claude Code GitHub Actions", documenting behaviour up to Claude Code v2.1.286. The practice is a workflow, a schema, a criteria file and a decision function, graded by Python, TypeScript, Java and Kotlin test suites, offline, on the course's model of the documented command-line behaviour (`examples/60-ci-gate`); nothing in it starts Claude Code. This page deepens module 38 and module 40 and does not repeat them. The command and the gate are the first page.

> **Exam guide and current product.** *What the guide states (task statement 3.6), and so what the exam keys:* the CI task (3.6) asks for CLAUDE.md to give the run testing standards, fixture conventions and review criteria; session context isolation, because "the same Claude session that generated code is less effective at reviewing its own changes compared to an independent review instance"; prior review findings included in the prompt when a re-run follows new commits, "instructing Claude to report only new or still-unaddressed issues"; and existing test files in the prompt of a test-generation run, so that suggestions do not duplicate covered scenarios. *What the current product does (documentation checked 2026-10-03):* the same ideas, with one wrinkle for CLAUDE.md: it is read by an ordinary run, and "`--bare` ... skip[s] auto-discovery of hooks, skills, ... auto memory, and CLAUDE.md", so a bare CI run gets its criteria through `--append-system-prompt-file`. The documentation's advice on review is the guide's: "A fresh context improves code review since Claude won't be biased toward code it just wrote." On the exam, answer with CLAUDE.md for criteria, an independent instance for review, and findings in the prompt for re-runs; in a bare job, pass the file.

## Why it matters

A pull request is reviewed on every push. The first push gets six comments; the second push, after the author fixed four of them, gets the same six and two new ones, and the author stops reading. A test-generation step proposes a test for a branch that an existing test already covers, three times in a week. Both failures have one cause: each run starts from nothing, and the prompt did not say what had already happened. Scenario S5 asks how to make the step report only what is new, and the answer is in the prompt, not in the model.

## The idea

### The criteria live in a file

"Be conservative" and "report only important issues" do not change what a reviewer reports, because they leave the threshold unstated. The criteria file says what to report, what to skip, and what each severity means, with a concrete example at each level; module 61 shows why the examples matter. The same file holds the testing standards a generation run needs: where tests live, which fixtures they use, and what makes a test worth adding ("a branch or an edge case that no existing test covers"). In an ordinary run Claude reads `CLAUDE.md` itself, and the documentation's advice for CI is to keep it concise because it is read on every run. In a bare run the file is passed explicitly with `--append-system-prompt-file`; a bare run without it reviews with no criteria at all, and nothing says so.

### An independent reviewer

A session that wrote a change carries its reasoning, and it is "less effective at reviewing its own changes" for that reason. In CI the arrangement is easy: the review job is a separate run that sees the diff and the criteria, and nothing the author's session decided. The documented patterns for interactive work are a second session as reviewer, or a reviewer subagent in a fresh context that "sees only the diff and the criteria you give it, not the reasoning that produced the change". Two cautions from the documentation: a reviewer asked to find gaps will usually report some, so tell it to flag only what affects correctness or the stated requirements; and the independent run is a separate invocation, not a second pass in the same conversation.

### A re-run needs memory it does not have

A CI run does not remember the last one. For the review that follows new commits, put what was already reported in the prompt, say not to repeat it, and say to report only issues that are new or still unaddressed. For test generation, put the existing tests in the prompt and say not to suggest a covered scenario. The practice's prompt builder puts the instructions and the two lists first and the diff last, in tagged sections: that order is the course's design, which keeps the instructions and the lists together. The prompting guidance for very long inputs is the reverse, with the long material near the top and the question after it, so for a large diff end the prompt with a one-line restatement of the task.

A record of findings between runs, which the prompt carries, is the only state the pipeline has. Where it lives (a comment on the pull request, a file, a store) is a design choice; the exam only asks that it reaches the prompt.

### Posting the result

Because the answer is structured, each finding becomes an inline comment with its file, line, severity and suggested fix; a `detected_pattern` field per finding lets the team analyse which patterns draw dismissals and tune the criteria. The Claude Code GitHub Action does the plumbing for the common case, taking `claude_args` such as `--max-turns` and `--allowedTools`, and the documentation lists the same controls for cost: a concise CLAUDE.md, a turn limit, job timeouts and concurrency limits.

### The example

<!-- example: m60-ci-gate tabs: python,typescript,java,kotlin -->
```python
"""A Claude Code review step in CI, from the command line to the exit status: build the headless command, lint a command someone wrote, and gate on the JSON the run prints.

Facts checked on the Claude Code pages "Run Claude Code programmatically" and "CLI reference" and the Agent SDK pages on structured outputs and the result message
(read on 2026-10-03, Claude Code v2.1.286): `-p` runs without a prompt for input; `--output-format json` prints one JSON object whose `structured_output` field holds the
answer when `--json-schema` is given; a run can end with `is_error` true and a subtype such as `error_max_turns` or `error_max_structured_output_retries`; `--max-turns` exits with an error
when the limit is reached; `--bare` skips CLAUDE.md, hooks, skills, MCP servers and auto memory, so the context is passed with `--append-system-prompt-file`. The envelope below is
the shape the SDK documents for its result message; nothing here ran the real binary, needed a key or touched the network.
"""
import json
import re
import logging

log = logging.getLogger(__name__)

SEVERITIES = ["low", "medium", "high"]
REVIEW_SCHEMA = {
    "type": "object",
    "properties": {"findings": {"type": "array", "items": {"type": "object", "properties": {
        "file": {"type": "string"}, "line": {"type": "integer"}, "category": {"type": "string", "enum": ["bug", "security", "style", "other"]},
        "severity": {"type": "string", "enum": SEVERITIES}, "issue": {"type": "string"}, "suggested_fix": {"type": "string"}, "detected_pattern": {"type": "string"}},
        "required": ["file", "line", "category", "severity", "issue", "suggested_fix", "detected_pattern"], "additionalProperties": False}}},
    "required": ["findings"], "additionalProperties": False,
}


def build_command(prompt, schema, max_turns=8, tools=("Read", "Grep", "Glob", "Bash(git diff *)"), context_file="CLAUDE.md"):
    """The argument list of a review run: headless, one JSON object out, the answer checked against a schema, a turn limit, read-only tools, and the project context passed by hand."""
    return ["claude", "--bare", "-p", prompt, "--append-system-prompt-file", context_file, "--output-format", "json", "--json-schema", json.dumps(schema, separators=(",", ":")),
            "--max-turns", str(max_turns), "--allowedTools", ",".join(tools)]


def lint_command(text):
    """Findings (rule ids) for the text of a shell step that runs `claude`: what a CI run needs and what it must not be allowed."""
    text = re.sub(r"\\\n\s*", " ", text)
    found = []
    if not re.search(r"\sclaude\b", " " + text):
        return ["no-claude-command"]
    if not re.search(r"\s(-p|--print)\b", text):
        found.append("no-print")
    if not re.search(r"--output-format[ =]json\b", text):
        found.append("no-json")
    if not re.search(r"--json-schema\b", text):
        found.append("no-schema")
    turns = re.search(r"--max-turns[ =](\d+)", text)
    if not turns or int(turns.group(1)) > 20:
        found.append("no-turn-limit")
    allowed = re.search(r"--allowed[Tt]ools[ =](\"[^\"]*\"|'[^']*'|\S+)", text)
    names = re.findall(r"[^\s,(]+(?:\([^)]*\))?", allowed.group(1).strip("\"'")) if allowed else []
    if not allowed:
        found.append("no-tool-list")
    elif any(n in ("Bash", "Edit", "Write", "MultiEdit", "NotebookEdit") for n in names):
        found.append("wide-tools")
    if "--bare" not in text:
        found.append("no-bare")
    elif not re.search(r"--append-system-prompt(-file)?\b", text):
        found.append("bare-without-context")
    return found


def schema_check(value, schema, path="$"):
    """Errors of a value against the JSON Schema subset the structured outputs support: type, enum, required, properties, items and additionalProperties false."""
    kinds = {"object": dict, "array": list, "string": str, "boolean": bool, "null": type(None)}
    want = schema.get("type")
    wants = want if isinstance(want, list) else [want] if want else []
    ok = not wants
    for kind in wants:
        if kind == "integer":
            ok = ok or (isinstance(value, int) and not isinstance(value, bool))
        elif kind == "number":
            ok = ok or (isinstance(value, (int, float)) and not isinstance(value, bool))
        else:
            ok = ok or isinstance(value, kinds[kind])
    if not ok:
        return [f"{path}: expected {' or '.join(wants)}"]
    errors = []
    if "enum" in schema and value not in schema["enum"]:
        errors.append(f"{path}: {value!r} is not one of {schema['enum']}")
    if isinstance(value, dict):
        errors += [f"{path}.{k}: is required" for k in schema.get("required", []) if k not in value]
        if schema.get("additionalProperties") is False:
            errors += [f"{path}.{k}: is not allowed" for k in value if k not in schema.get("properties", {})]
        for k, sub in schema.get("properties", {}).items():
            if k in value:
                errors += schema_check(value[k], sub, f"{path}.{k}")
    if isinstance(value, list) and "items" in schema:
        for i, item in enumerate(value):
            errors += schema_check(item, schema["items"], f"{path}[{i}]")
    return errors


def gate(stdout, exit_code, schema, policy):
    """Decide a review job from what `claude -p --output-format json --json-schema ...` printed. A run that failed in any way fails the job: it never passes by saying nothing."""
    problems = []
    if exit_code != 0:
        problems.append(f"claude exited with status {exit_code}")
    try:
        envelope = json.loads(stdout)
    except ValueError:
        envelope = None
    if not isinstance(envelope, dict):
        return {"exit": 1, "comments": [], "problems": problems + ["the output is not a JSON object"]}
    if envelope.get("is_error") or envelope.get("subtype") != "success":
        problems.append(f"the run ended with {envelope.get('subtype')}")
    output = envelope.get("structured_output")
    if output is None:
        problems.append("the result has no structured_output")
    else:
        problems += [f"schema {e}" for e in schema_check(output, schema)]
    if problems:
        return {"exit": 1, "comments": [], "problems": problems}
    floor = SEVERITIES.index(policy["min_severity"])
    comments = [{"file": f["file"], "line": f["line"], "severity": f["severity"], "body": f"{f['issue']} Suggested fix: {f['suggested_fix']}"}
                for f in output["findings"] if f["category"] not in policy["disabled_categories"] and SEVERITIES.index(f["severity"]) >= floor]
    blocked = any(c["severity"] in policy["fail_on"] for c in comments)
    return {"exit": 1 if blocked else 0, "comments": comments, "problems": []}


def envelope(**over):
    return json.dumps({"type": "result", "subtype": "success", "is_error": False, "result": "done", "num_turns": 3, "total_cost_usd": 0.05, "session_id": "s-1", **over})


def main():
    command = build_command("Review the diff on standard input.", {"type": "object", "properties": {"findings": {"type": "array"}}}, tools=("Read", "Grep"))
    print("command:", " ".join(command[:5]), "...", command[-4:])
    print("lint, the good step:", lint_command("git diff main | claude --bare -p 'Review.' --append-system-prompt-file CLAUDE.md --output-format json --json-schema '{}' --max-turns 8 --allowedTools 'Read,Grep'"))
    print("lint, a careless step:", lint_command("claude 'Review the change' --allowedTools Bash,Edit"))
    policy = {"min_severity": "medium", "disabled_categories": ["style"], "fail_on": ["high"]}
    findings = [{"file": "api.py", "line": 12, "category": "bug", "severity": "high", "issue": "Unchecked None.", "suggested_fix": "Return early.", "detected_pattern": "missing-none-check"},
                {"file": "api.py", "line": 40, "category": "style", "severity": "high", "issue": "Long line.", "suggested_fix": "Wrap it.", "detected_pattern": "line-length"},
                {"file": "ui.py", "line": 3, "category": "bug", "severity": "low", "issue": "Odd name.", "suggested_fix": "Rename.", "detected_pattern": "naming"}]
    runs = {"a finding that blocks": (envelope(structured_output={"findings": findings}), 0), "no findings": (envelope(structured_output={"findings": []}), 0),
            "turn limit": (envelope(subtype="error_max_turns", is_error=True), 1), "success without output": (envelope(), 0), "not JSON": ("Error: no key", 1),
            "wrong shape": (envelope(structured_output={"findings": [{"file": "a.py"}]}), 0)}
    for label, (out, code) in runs.items():
        result = gate(out, code, REVIEW_SCHEMA, policy)
        print(f"{label}: exit {result['exit']}, {len(result['comments'])} comment(s){', ' + result['problems'][0] if result['problems'] else ''}")


if __name__ == "__main__":
    main()
```
```text
command: claude --bare -p Review the diff on standard input. --append-system-prompt-file ... ['--max-turns', '8', '--allowedTools', 'Read,Grep']
lint, the good step: []
lint, a careless step: ['no-print', 'no-json', 'no-schema', 'no-turn-limit', 'wide-tools', 'no-bare']
a finding that blocks: exit 1, 1 comment(s)
no findings: exit 0, 0 comment(s)
turn limit: exit 1, 0 comment(s), claude exited with status 1
success without output: exit 1, 0 comment(s), the result has no structured_output
not JSON: exit 1, 0 comment(s), claude exited with status 1
wrong shape: exit 1, 0 comment(s), schema $.findings[0].line: is required
```
```typescript
import { logger } from "./logger.ts";
const log = logger("ci_gate");
/**
 * A Claude Code review step in CI, from the command line to the exit status: build the headless command, lint a command someone wrote, and gate on the JSON the run prints.
 *
 * Facts checked on the Claude Code pages "Run Claude Code programmatically" and "CLI reference" and the Agent SDK pages on structured outputs and the result message
 * (read on 2026-10-03, Claude Code v2.1.286): `-p` runs without a prompt for input; `--output-format json` prints one JSON object whose `structured_output` field holds the
 * answer when `--json-schema` is given; a run can end with `is_error` true and a subtype such as `error_max_turns` or `error_max_structured_output_retries`; `--max-turns` exits with an error
 * when the limit is reached; `--bare` skips CLAUDE.md, hooks, skills, MCP servers and auto memory, so the context is passed with `--append-system-prompt-file`. The envelope below is
 * the shape the SDK documents for its result message; nothing here ran the real binary, needed a key or touched the network.
 */
export const SEVERITIES = ["low", "medium", "high"];
export type Schema = { [key: string]: any };
export const REVIEW_SCHEMA: Schema = {
  type: "object",
  properties: { findings: { type: "array", items: { type: "object", properties: {
    file: { type: "string" }, line: { type: "integer" }, category: { type: "string", enum: ["bug", "security", "style", "other"] },
    severity: { type: "string", enum: SEVERITIES }, issue: { type: "string" }, suggested_fix: { type: "string" }, detected_pattern: { type: "string" } },
    required: ["file", "line", "category", "severity", "issue", "suggested_fix", "detected_pattern"], additionalProperties: false } } },
  required: ["findings"], additionalProperties: false,
};

/** The argument list of a review run: headless, one JSON object out, the answer checked against a schema, a turn limit, read-only tools, and the project context passed by hand. */
export function buildCommand(prompt: string, schema: Schema, maxTurns = 8, tools = ["Read", "Grep", "Glob", "Bash(git diff *)"], contextFile = "CLAUDE.md"): string[] {
  return ["claude", "--bare", "-p", prompt, "--append-system-prompt-file", contextFile, "--output-format", "json", "--json-schema", JSON.stringify(schema),
    "--max-turns", String(maxTurns), "--allowedTools", tools.join(",")];
}

/** Findings (rule ids) for the text of a shell step that runs `claude`: what a CI run needs and what it must not be allowed. */
export function lintCommand(raw: string): string[] {
  const text = raw.replace(/\\\n\s*/g, " ");
  if (!/\sclaude\b/.test(" " + text)) return ["no-claude-command"];
  const found: string[] = [];
  if (!/\s(-p|--print)\b/.test(text)) found.push("no-print");
  if (!/--output-format[ =]json\b/.test(text)) found.push("no-json");
  if (!/--json-schema\b/.test(text)) found.push("no-schema");
  const turns = /--max-turns[ =](\d+)/.exec(text);
  if (!turns || Number(turns[1]) > 20) found.push("no-turn-limit");
  const allowed = /--allowed[Tt]ools[ =]("[^"]*"|'[^']*'|\S+)/.exec(text);
  const names = allowed ? allowed[1].replace(/^["']|["']$/g, "").match(/[^\s,(]+(?:\([^)]*\))?/g) ?? [] : [];
  if (!allowed) found.push("no-tool-list");
  else if (names.some((n) => ["Bash", "Edit", "Write", "MultiEdit", "NotebookEdit"].includes(n))) found.push("wide-tools");
  if (!text.includes("--bare")) found.push("no-bare");
  else if (!/--append-system-prompt(-file)?\b/.test(text)) found.push("bare-without-context");
  return found;
}

const isKind = (kind: string, value: unknown): boolean =>
  kind === "object" ? typeof value === "object" && value !== null && !Array.isArray(value)
    : kind === "array" ? Array.isArray(value)
    : kind === "string" ? typeof value === "string"
    : kind === "boolean" ? typeof value === "boolean"
    : kind === "null" ? value === null
    : kind === "integer" ? typeof value === "number" && Number.isInteger(value)
    : kind === "number" ? typeof value === "number" : false;

/** Errors of a value against the JSON Schema subset the structured outputs support: type, enum, required, properties, items and additionalProperties false. */
export function schemaCheck(value: any, schema: Schema, path = "$"): string[] {
  const wants: string[] = Array.isArray(schema.type) ? schema.type : schema.type ? [schema.type] : [];
  if (wants.length > 0 && !wants.some((k) => isKind(k, value))) return [`${path}: expected ${wants.join(" or ")}`];
  const errors: string[] = [];
  if (schema.enum && !schema.enum.includes(value)) errors.push(`${path}: ${JSON.stringify(value)} is not one of ${JSON.stringify(schema.enum)}`);
  if (isKind("object", value)) {
    for (const k of schema.required ?? []) if (!(k in value)) errors.push(`${path}.${k}: is required`);
    if (schema.additionalProperties === false) for (const k of Object.keys(value)) if (!(k in (schema.properties ?? {}))) errors.push(`${path}.${k}: is not allowed`);
    for (const [k, sub] of Object.entries(schema.properties ?? {})) if (k in value) errors.push(...schemaCheck(value[k], sub as Schema, `${path}.${k}`));
  }
  if (Array.isArray(value) && schema.items) value.forEach((item, i) => errors.push(...schemaCheck(item, schema.items, `${path}[${i}]`)));
  return errors;
}

export type Policy = { min_severity: string; disabled_categories: string[]; fail_on: string[] };
export type Gate = { exit: number; comments: Array<{ file: string; line: number; severity: string; body: string }>; problems: string[] };

/** Decide a review job from what `claude -p --output-format json --json-schema ...` printed. A run that failed in any way fails the job: it never passes by saying nothing. */
export function gate(stdout: string, exitCode: number, schema: Schema, policy: Policy): Gate {
  const problems: string[] = [];
  if (exitCode !== 0) problems.push(`claude exited with status ${exitCode}`);
  let envelope: any = null;
  try {
    envelope = JSON.parse(stdout);
  } catch {
    envelope = null;
  }
  if (!isKind("object", envelope)) return { exit: 1, comments: [], problems: [...problems, "the output is not a JSON object"] };
  if (envelope.is_error || envelope.subtype !== "success") problems.push(`the run ended with ${envelope.subtype}`);
  const output = envelope.structured_output;
  if (output === undefined || output === null) problems.push("the result has no structured_output");
  else problems.push(...schemaCheck(output, schema).map((e) => `schema ${e}`));
  if (problems.length > 0) return { exit: 1, comments: [], problems };
  const floor = SEVERITIES.indexOf(policy.min_severity);
  const comments = output.findings
    .filter((f: any) => !policy.disabled_categories.includes(f.category) && SEVERITIES.indexOf(f.severity) >= floor)
    .map((f: any) => ({ file: f.file, line: f.line, severity: f.severity, body: `${f.issue} Suggested fix: ${f.suggested_fix}` }));
  const blocked = comments.some((c: any) => policy.fail_on.includes(c.severity));
  return { exit: blocked ? 1 : 0, comments, problems: [] };
}

export function envelope(over: Record<string, unknown> = {}): string {
  return JSON.stringify({ type: "result", subtype: "success", is_error: false, result: "done", num_turns: 3, total_cost_usd: 0.05, session_id: "s-1", ...over });
}

function main() {
  const command = buildCommand("Review the diff on standard input.", { type: "object", properties: { findings: { type: "array" } } }, 8, ["Read", "Grep"]);
  console.log("command:", command.slice(0, 5).join(" "), "...", JSON.stringify(command.slice(-4)));
  console.log("lint, the good step:", JSON.stringify(lintCommand("git diff main | claude --bare -p 'Review.' --append-system-prompt-file CLAUDE.md --output-format json --json-schema '{}' --max-turns 8 --allowedTools 'Read,Grep'")));
  console.log("lint, a careless step:", JSON.stringify(lintCommand("claude 'Review the change' --allowedTools Bash,Edit")));
  const policy: Policy = { min_severity: "medium", disabled_categories: ["style"], fail_on: ["high"] };
  const findings = [
    { file: "api.py", line: 12, category: "bug", severity: "high", issue: "Unchecked None.", suggested_fix: "Return early.", detected_pattern: "missing-none-check" },
    { file: "api.py", line: 40, category: "style", severity: "high", issue: "Long line.", suggested_fix: "Wrap it.", detected_pattern: "line-length" },
    { file: "ui.py", line: 3, category: "bug", severity: "low", issue: "Odd name.", suggested_fix: "Rename.", detected_pattern: "naming" },
  ];
  const runs: Record<string, [string, number]> = {
    "a finding that blocks": [envelope({ structured_output: { findings } }), 0], "no findings": [envelope({ structured_output: { findings: [] } }), 0],
    "turn limit": [envelope({ subtype: "error_max_turns", is_error: true }), 1], "success without output": [envelope(), 0], "not JSON": ["Error: no key", 1],
    "wrong shape": [envelope({ structured_output: { findings: [{ file: "a.py" }] } }), 0],
  };
  for (const [label, [out, code]] of Object.entries(runs)) {
    const result = gate(out, code, REVIEW_SCHEMA, policy);
    console.log(`${label}: exit ${result.exit}, ${result.comments.length} comment(s)${result.problems.length ? ", " + result.problems[0] : ""}`);
  }
}

if (import.meta.main) main();
```
```text
command: claude --bare -p Review the diff on standard input. --append-system-prompt-file ... ["--max-turns","8","--allowedTools","Read,Grep"]
lint, the good step: []
lint, a careless step: ["no-print","no-json","no-schema","no-turn-limit","wide-tools","no-bare"]
a finding that blocks: exit 1, 1 comment(s)
no findings: exit 0, 0 comment(s)
turn limit: exit 1, 0 comment(s), claude exited with status 1
success without output: exit 1, 0 comment(s), the result has no structured_output
not JSON: exit 1, 0 comment(s), claude exited with status 1
wrong shape: exit 1, 0 comment(s), schema $.findings[0].line: is required
```
```java
import static harness.Show.py;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A Claude Code review step in CI, from the command line to the exit status: build the headless command, lint a command someone wrote, and gate on the JSON the run prints.
 *
 * <p>Facts checked on the Claude Code pages "Run Claude Code programmatically" and "CLI reference" and the Agent SDK pages on structured outputs and the result message
 * (read on 2026-10-03, Claude Code v2.1.286): `-p` runs without a prompt for input; `--output-format json` prints one JSON object whose `structured_output` field holds the
 * answer when `--json-schema` is given; a run can end with `is_error` true and a subtype such as `error_max_turns` or `error_max_structured_output_retries`; `--max-turns` exits with an error
 * when the limit is reached; `--bare` skips CLAUDE.md, hooks, skills, MCP servers and auto memory, so the context is passed with `--append-system-prompt-file`. The envelope below is
 * the shape the SDK documents for its result message; nothing here ran the real binary, needed a key or touched the network. The JSON is read with Jackson.
 */
public final class CiGate {
    private static final System.Logger LOG = System.getLogger(CiGate.class.getName());
    static final List<String> SEVERITIES = List.of("low", "medium", "high");
    private static final ObjectMapper JSON = new ObjectMapper();

    static JsonNode json(String text) {
        try {
            return JSON.readTree(text);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    static final JsonNode REVIEW_SCHEMA = json("""
        {"type": "object",
         "properties": {"findings": {"type": "array", "items": {"type": "object", "properties": {
           "file": {"type": "string"}, "line": {"type": "integer"}, "category": {"type": "string", "enum": ["bug", "security", "style", "other"]},
           "severity": {"type": "string", "enum": ["low", "medium", "high"]}, "issue": {"type": "string"}, "suggested_fix": {"type": "string"}, "detected_pattern": {"type": "string"}},
           "required": ["file", "line", "category", "severity", "issue", "suggested_fix", "detected_pattern"], "additionalProperties": false}}},
         "required": ["findings"], "additionalProperties": false}""");

    /** What a team decides about review findings: the lowest severity to post, the categories to drop and the severities that fail the job. */
    record Policy(String minSeverity, List<String> disabledCategories, List<String> failOn) {}

    /** One review comment to post. */
    record Comment(String file, int line, String severity, String body) {}

    /** The decision: the exit status, the comments to post and what went wrong. */
    record Decision(int exit, List<Comment> comments, List<String> problems) {}

    /** The argument list of a review run: headless, one JSON object out, the answer checked against a schema, a turn limit, read-only tools, and the project context passed by hand. */
    static List<String> buildCommand(String prompt, JsonNode schema, int maxTurns, List<String> tools, String contextFile) {
        return List.of("claude", "--bare", "-p", prompt, "--append-system-prompt-file", contextFile, "--output-format", "json", "--json-schema", schema.toString(),
            "--max-turns", String.valueOf(maxTurns), "--allowedTools", String.join(",", tools));
    }

    static List<String> buildCommand(String prompt, JsonNode schema) {
        return buildCommand(prompt, schema, 8, List.of("Read", "Grep", "Glob", "Bash(git diff *)"), "CLAUDE.md");
    }

    private static String stripQuotes(String s) {
        int from = 0, to = s.length();
        while (from < to && (s.charAt(from) == '"' || s.charAt(from) == '\'')) from++;
        while (to > from && (s.charAt(to - 1) == '"' || s.charAt(to - 1) == '\'')) to--;
        return s.substring(from, to);
    }

    private static boolean has(String regex, String text) {
        return Pattern.compile(regex).matcher(text).find();
    }

    /** Findings (rule ids) for the text of a shell step that runs `claude`: what a CI run needs and what it must not be allowed. */
    static List<String> lintCommand(String text) {
        text = text.replaceAll("\\\\\\n\\s*", " ");
        List<String> found = new ArrayList<>();
        if (!has("\\sclaude\\b", " " + text)) return List.of("no-claude-command");
        if (!has("\\s(-p|--print)\\b", text)) found.add("no-print");
        if (!has("--output-format[ =]json\\b", text)) found.add("no-json");
        if (!has("--json-schema\\b", text)) found.add("no-schema");
        Matcher turns = Pattern.compile("--max-turns[ =](\\d+)").matcher(text);
        if (!turns.find() || Integer.parseInt(turns.group(1)) > 20) found.add("no-turn-limit");
        Matcher allowed = Pattern.compile("--allowed[Tt]ools[ =](\"[^\"]*\"|'[^']*'|\\S+)").matcher(text);
        boolean given = allowed.find();
        List<String> names = new ArrayList<>();
        if (given) {
            Matcher n = Pattern.compile("[^\\s,(]+(?:\\([^)]*\\))?").matcher(stripQuotes(allowed.group(1).strip()));
            while (n.find()) names.add(n.group());
        }
        if (!given) found.add("no-tool-list");
        else if (names.stream().anyMatch(n -> List.of("Bash", "Edit", "Write", "MultiEdit", "NotebookEdit").contains(n))) found.add("wide-tools");
        if (!text.contains("--bare")) found.add("no-bare");
        else if (!has("--append-system-prompt(-file)?\\b", text)) found.add("bare-without-context");
        return found;
    }

    private static boolean isType(JsonNode value, String kind) {
        return switch (kind) {
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            case "string" -> value.isTextual();
            case "boolean" -> value.isBoolean();
            case "null" -> value.isNull();
            case "integer" -> value.isIntegralNumber();
            case "number" -> value.isNumber();
            default -> throw new IllegalArgumentException(kind);
        };
    }

    /** Errors of a value against the JSON Schema subset the structured outputs support: type, enum, required, properties, items and additionalProperties false. */
    static List<String> schemaCheck(JsonNode value, JsonNode schema, String path) {
        List<String> wants = new ArrayList<>();
        JsonNode want = schema.get("type");
        if (want != null) {
            if (want.isArray()) want.forEach(w -> wants.add(w.asText()));
            else wants.add(want.asText());
        }
        boolean ok = wants.isEmpty() || wants.stream().anyMatch(k -> isType(value, k));
        if (!ok) return List.of(path + ": expected " + String.join(" or ", wants));
        List<String> errors = new ArrayList<>();
        JsonNode enumeration = schema.get("enum");
        if (enumeration != null) {
            boolean found = false;
            for (JsonNode e : enumeration) found |= e.equals(value);
            if (!found) errors.add(path + ": " + py(value) + " is not one of " + py(enumeration));
        }
        if (value.isObject()) {
            JsonNode required = schema.get("required");
            if (required != null) for (JsonNode k : required) if (!value.has(k.asText())) errors.add(path + "." + k.asText() + ": is required");
            JsonNode extra = schema.get("additionalProperties");
            JsonNode properties = schema.has("properties") ? schema.get("properties") : JSON.createObjectNode();
            if (extra != null && extra.isBoolean() && !extra.asBoolean()) {
                for (var it = value.fieldNames(); it.hasNext();) {
                    String k = it.next();
                    if (!properties.has(k)) errors.add(path + "." + k + ": is not allowed");
                }
            }
            for (var it = properties.fields(); it.hasNext();) {
                var sub = it.next();
                if (value.has(sub.getKey())) errors.addAll(schemaCheck(value.get(sub.getKey()), sub.getValue(), path + "." + sub.getKey()));
            }
        }
        if (value.isArray() && schema.has("items")) {
            for (int i = 0; i < value.size(); i++) errors.addAll(schemaCheck(value.get(i), schema.get("items"), path + "[" + i + "]"));
        }
        return errors;
    }

    static List<String> schemaCheck(JsonNode value, JsonNode schema) {
        return schemaCheck(value, schema, "$");
    }

    private static String text(JsonNode n) {
        return n.asText();
    }

    /** Decide a review job from what `claude -p --output-format json --json-schema ...` printed. A run that failed in any way fails the job: it never passes by saying nothing. */
    static Decision gate(String stdout, int exitCode, JsonNode schema, Policy policy) {
        List<String> problems = new ArrayList<>();
        if (exitCode != 0) problems.add("claude exited with status " + exitCode);
        JsonNode envelope;
        try {
            envelope = JSON.readTree(stdout);
        } catch (Exception e) {
            envelope = null;
        }
        if (envelope == null || !envelope.isObject()) {
            problems.add("the output is not a JSON object");
            return new Decision(1, List.of(), problems);
        }
        JsonNode subtype = envelope.get("subtype");
        if (envelope.path("is_error").asBoolean(false) || subtype == null || !subtype.isTextual() || !subtype.asText().equals("success")) {
            problems.add("the run ended with " + (subtype == null || subtype.isNull() ? "None" : subtype.asText()));
        }
        JsonNode output = envelope.get("structured_output");
        if (output == null || output.isNull()) problems.add("the result has no structured_output");
        else for (String e : schemaCheck(output, schema)) problems.add("schema " + e);
        if (!problems.isEmpty()) return new Decision(1, List.of(), problems);
        int floor = SEVERITIES.indexOf(policy.minSeverity());
        List<Comment> comments = new ArrayList<>();
        for (JsonNode f : output.get("findings")) {
            if (policy.disabledCategories().contains(text(f.get("category"))) || SEVERITIES.indexOf(text(f.get("severity"))) < floor) continue;
            comments.add(new Comment(text(f.get("file")), f.get("line").asInt(), text(f.get("severity")), text(f.get("issue")) + " Suggested fix: " + text(f.get("suggested_fix"))));
        }
        boolean blocked = comments.stream().anyMatch(c -> policy.failOn().contains(c.severity()));
        return new Decision(blocked ? 1 : 0, comments, List.of());
    }

    static String envelope(Map<String, Object> over) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "result");
        body.put("subtype", "success");
        body.put("is_error", false);
        body.put("result", "done");
        body.put("num_turns", 3);
        body.put("total_cost_usd", 0.05);
        body.put("session_id", "s-1");
        body.putAll(over);
        try {
            return JSON.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String envelope() {
        return envelope(Map.of());
    }

    /** Python's shlex.quote for one word. */
    static String quote(String s) {
        if (s.isEmpty()) return "''";
        return s.matches("[\\w@%+=:,./-]+") ? s : "'" + s.replace("'", "'\"'\"'") + "'";
    }

    static String join(List<String> words) {
        return words.stream().map(CiGate::quote).collect(Collectors.joining(" "));
    }

    static Map<String, Object> finding(String file, int line, String category, String severity, String issue, String fix, String pattern) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("file", file);
        f.put("line", line);
        f.put("category", category);
        f.put("severity", severity);
        f.put("issue", issue);
        f.put("suggested_fix", fix);
        f.put("detected_pattern", pattern);
        return f;
    }

    public static void main(String[] args) {
        List<String> command = buildCommand("Review the diff on standard input.", json("{\"type\": \"object\", \"properties\": {\"findings\": {\"type\": \"array\"}}}"), 8, List.of("Read", "Grep"), "CLAUDE.md");
        System.out.println("command: " + String.join(" ", command.subList(0, 5)) + " ... " + py(command.subList(command.size() - 4, command.size())));
        System.out.println("lint, the good step: " + py(lintCommand("git diff main | claude --bare -p 'Review.' --append-system-prompt-file CLAUDE.md --output-format json --json-schema '{}' --max-turns 8 --allowedTools 'Read,Grep'")));
        System.out.println("lint, a careless step: " + py(lintCommand("claude 'Review the change' --allowedTools Bash,Edit")));
        Policy policy = new Policy("medium", List.of("style"), List.of("high"));
        List<Map<String, Object>> findings = List.of(
            finding("api.py", 12, "bug", "high", "Unchecked None.", "Return early.", "missing-none-check"),
            finding("api.py", 40, "style", "high", "Long line.", "Wrap it.", "line-length"),
            finding("ui.py", 3, "bug", "low", "Odd name.", "Rename.", "naming"));
        Map<String, String[]> runs = new LinkedHashMap<>();
        runs.put("a finding that blocks", new String[] {envelope(Map.of("structured_output", Map.of("findings", findings))), "0"});
        runs.put("no findings", new String[] {envelope(Map.of("structured_output", Map.of("findings", List.of()))), "0"});
        runs.put("turn limit", new String[] {envelope(Map.of("subtype", "error_max_turns", "is_error", true)), "1"});
        runs.put("success without output", new String[] {envelope(), "0"});
        runs.put("not JSON", new String[] {"Error: no key", "1"});
        runs.put("wrong shape", new String[] {envelope(Map.of("structured_output", Map.of("findings", List.of(Map.of("file", "a.py"))))), "0"});
        runs.forEach((label, run) -> {
            Decision d = gate(run[0], Integer.parseInt(run[1]), REVIEW_SCHEMA, policy);
            System.out.println(label + ": exit " + d.exit() + ", " + d.comments().size() + " comment(s)" + (d.problems().isEmpty() ? "" : ", " + d.problems().get(0)));
        });
    }
}
```
```text
command: claude --bare -p Review the diff on standard input. --append-system-prompt-file ... ['--max-turns', '8', '--allowedTools', 'Read,Grep']
lint, the good step: []
lint, a careless step: ['no-print', 'no-json', 'no-schema', 'no-turn-limit', 'wide-tools', 'no-bare']
a finding that blocks: exit 1, 1 comment(s)
no findings: exit 0, 0 comment(s)
turn limit: exit 1, 0 comment(s), claude exited with status 1
success without output: exit 1, 0 comment(s), the result has no structured_output
not JSON: exit 1, 0 comment(s), claude exited with status 1
wrong shape: exit 1, 0 comment(s), schema $.findings[0].line: is required
```
```kotlin
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import harness.Show.py

private val log = System.getLogger("ci_gate")

/**
 * A Claude Code review step in CI, from the command line to the exit status: build the headless command, lint a command someone wrote, and gate on the JSON the run prints.
 *
 * Facts checked on the Claude Code pages "Run Claude Code programmatically" and "CLI reference" and the Agent SDK pages on structured outputs and the result message
 * (read on 2026-10-03, Claude Code v2.1.286): `-p` runs without a prompt for input; `--output-format json` prints one JSON object whose `structured_output` field holds the
 * answer when `--json-schema` is given; a run can end with `is_error` true and a subtype such as `error_max_turns` or `error_max_structured_output_retries`; `--max-turns` exits with an error
 * when the limit is reached; `--bare` skips CLAUDE.md, hooks, skills, MCP servers and auto memory, so the context is passed with `--append-system-prompt-file`. The envelope below is
 * the shape the SDK documents for its result message; nothing here ran the real binary, needed a key or touched the network. The JSON is read with Jackson.
 */
val SEVERITIES = listOf("low", "medium", "high")
private val JSON = ObjectMapper()

fun json(text: String): JsonNode = JSON.readTree(text)

val REVIEW_SCHEMA: JsonNode = json(
    """
    {"type": "object",
     "properties": {"findings": {"type": "array", "items": {"type": "object", "properties": {
       "file": {"type": "string"}, "line": {"type": "integer"}, "category": {"type": "string", "enum": ["bug", "security", "style", "other"]},
       "severity": {"type": "string", "enum": ["low", "medium", "high"]}, "issue": {"type": "string"}, "suggested_fix": {"type": "string"}, "detected_pattern": {"type": "string"}},
       "required": ["file", "line", "category", "severity", "issue", "suggested_fix", "detected_pattern"], "additionalProperties": false}}},
     "required": ["findings"], "additionalProperties": false}
    """,
)

/** What a team decides about review findings: the lowest severity to post, the categories to drop and the severities that fail the job. */
data class Policy(val minSeverity: String, val disabledCategories: List<String>, val failOn: List<String>)

/** One review comment to post. */
data class Comment(val file: String, val line: Int, val severity: String, val body: String)

/** The decision: the exit status, the comments to post and what went wrong. */
data class Decision(val exit: Int, val comments: List<Comment>, val problems: List<String>)

/** The argument list of a review run: headless, one JSON object out, the answer checked against a schema, a turn limit, read-only tools, and the project context passed by hand. */
fun buildCommand(
    prompt: String,
    schema: JsonNode,
    maxTurns: Int = 8,
    tools: List<String> = listOf("Read", "Grep", "Glob", "Bash(git diff *)"),
    contextFile: String = "CLAUDE.md",
): List<String> = listOf(
    "claude", "--bare", "-p", prompt, "--append-system-prompt-file", contextFile, "--output-format", "json", "--json-schema", schema.toString(),
    "--max-turns", maxTurns.toString(), "--allowedTools", tools.joinToString(","),
)

/** Findings (rule ids) for the text of a shell step that runs `claude`: what a CI run needs and what it must not be allowed. */
fun lintCommand(raw: String): List<String> {
    val text = raw.replace(Regex("""\\\n\s*"""), " ")
    val found = mutableListOf<String>()
    if (!Regex("""\sclaude\b""").containsMatchIn(" $text")) return listOf("no-claude-command")
    if (!Regex("""\s(-p|--print)\b""").containsMatchIn(text)) found += "no-print"
    if (!Regex("""--output-format[ =]json\b""").containsMatchIn(text)) found += "no-json"
    if (!Regex("""--json-schema\b""").containsMatchIn(text)) found += "no-schema"
    val turns = Regex("""--max-turns[ =](\d+)""").find(text)
    if (turns == null || turns.groupValues[1].toInt() > 20) found += "no-turn-limit"
    val allowed = Regex("""--allowed[Tt]ools[ =]("[^"]*"|'[^']*'|\S+)""").find(text)
    val names = if (allowed != null) Regex("""[^\s,(]+(?:\([^)]*\))?""").findAll(allowed.groupValues[1].trim().trim('"', '\'')).map { it.value }.toList() else emptyList()
    if (allowed == null) found += "no-tool-list"
    else if (names.any { it in listOf("Bash", "Edit", "Write", "MultiEdit", "NotebookEdit") }) found += "wide-tools"
    if ("--bare" !in text) found += "no-bare"
    else if (!Regex("""--append-system-prompt(-file)?\b""").containsMatchIn(text)) found += "bare-without-context"
    return found
}

private fun isType(value: JsonNode, kind: String): Boolean = when (kind) {
    "object" -> value.isObject
    "array" -> value.isArray
    "string" -> value.isTextual
    "boolean" -> value.isBoolean
    "null" -> value.isNull
    "integer" -> value.isIntegralNumber
    "number" -> value.isNumber
    else -> throw IllegalArgumentException(kind)
}

/** Errors of a value against the JSON Schema subset the structured outputs support: type, enum, required, properties, items and additionalProperties false. */
fun schemaCheck(value: JsonNode, schema: JsonNode, path: String = "$"): List<String> {
    val want = schema.get("type")
    val wants = when {
        want == null -> emptyList()
        want.isArray -> want.map { it.asText() }
        else -> listOf(want.asText())
    }
    if (wants.isNotEmpty() && wants.none { isType(value, it) }) return listOf("$path: expected ${wants.joinToString(" or ")}")
    val errors = mutableListOf<String>()
    val enumeration = schema.get("enum")
    if (enumeration != null && enumeration.none { it == value }) errors += "$path: ${py(value)} is not one of ${py(enumeration)}"
    if (value.isObject) {
        schema.get("required")?.forEach { k -> if (!value.has(k.asText())) errors += "$path.${k.asText()}: is required" }
        val properties = schema.get("properties")
        val extra = schema.get("additionalProperties")
        if (extra != null && extra.isBoolean && !extra.asBoolean()) value.fieldNames().forEach { k -> if (properties == null || !properties.has(k)) errors += "$path.$k: is not allowed" }
        properties?.fields()?.forEach { (k, sub) -> if (value.has(k)) errors += schemaCheck(value.get(k), sub, "$path.$k") }
    }
    if (value.isArray && schema.has("items")) value.forEachIndexed { i, item -> errors += schemaCheck(item, schema.get("items"), "$path[$i]") }
    return errors
}

/** Decide a review job from what `claude -p --output-format json --json-schema ...` printed. A run that failed in any way fails the job: it never passes by saying nothing. */
fun gate(stdout: String, exitCode: Int, schema: JsonNode, policy: Policy): Decision {
    val problems = mutableListOf<String>()
    if (exitCode != 0) problems += "claude exited with status $exitCode"
    val envelope = try { JSON.readTree(stdout) } catch (e: Exception) { null }
    if (envelope == null || !envelope.isObject) return Decision(1, emptyList(), problems + "the output is not a JSON object")
    val subtype = envelope.get("subtype")
    if (envelope.path("is_error").asBoolean(false) || subtype == null || !subtype.isTextual || subtype.asText() != "success") {
        problems += "the run ended with ${if (subtype == null || subtype.isNull) "None" else subtype.asText()}"
    }
    val output = envelope.get("structured_output")
    if (output == null || output.isNull) problems += "the result has no structured_output" else problems += schemaCheck(output, schema).map { "schema $it" }
    if (problems.isNotEmpty()) return Decision(1, emptyList(), problems)
    val floor = SEVERITIES.indexOf(policy.minSeverity)
    val comments = output!!.get("findings")
        .filter { it.get("category").asText() !in policy.disabledCategories && SEVERITIES.indexOf(it.get("severity").asText()) >= floor }
        .map { Comment(it.get("file").asText(), it.get("line").asInt(), it.get("severity").asText(), "${it.get("issue").asText()} Suggested fix: ${it.get("suggested_fix").asText()}") }
    val blocked = comments.any { it.severity in policy.failOn }
    return Decision(if (blocked) 1 else 0, comments, emptyList())
}

fun envelope(over: Map<String, Any?> = emptyMap()): String =
    JSON.writeValueAsString(linkedMapOf<String, Any?>("type" to "result", "subtype" to "success", "is_error" to false, "result" to "done", "num_turns" to 3, "total_cost_usd" to 0.05, "session_id" to "s-1") + over)

/** Python's shlex.quote for one word. */
fun quote(s: String): String = if (s.isEmpty()) "''" else if (Regex("""[\w@%+=:,./-]+""").matches(s)) s else "'" + s.replace("'", "'\"'\"'") + "'"

fun join(words: List<String>): String = words.joinToString(" ") { quote(it) }

fun finding(file: String = "api.py", line: Int = 12, category: String = "bug", severity: String = "medium", issue: String = "Unchecked None.", fix: String = "Return early.", pattern: String = "missing-none-check"): MutableMap<String, Any?> =
    linkedMapOf("file" to file, "line" to line, "category" to category, "severity" to severity, "issue" to issue, "suggested_fix" to fix, "detected_pattern" to pattern)

fun main() {
    val command = buildCommand("Review the diff on standard input.", json("""{"type": "object", "properties": {"findings": {"type": "array"}}}"""), tools = listOf("Read", "Grep"))
    println("command: ${command.take(5).joinToString(" ")} ... ${py(command.takeLast(4))}")
    println("lint, the good step: ${py(lintCommand("git diff main | claude --bare -p 'Review.' --append-system-prompt-file CLAUDE.md --output-format json --json-schema '{}' --max-turns 8 --allowedTools 'Read,Grep'"))}")
    println("lint, a careless step: ${py(lintCommand("claude 'Review the change' --allowedTools Bash,Edit"))}")
    val policy = Policy("medium", listOf("style"), listOf("high"))
    val findings = listOf(
        finding(severity = "high", issue = "Unchecked None.", fix = "Return early.", pattern = "missing-none-check"),
        finding(line = 40, category = "style", severity = "high", issue = "Long line.", fix = "Wrap it.", pattern = "line-length"),
        finding(file = "ui.py", line = 3, severity = "low", issue = "Odd name.", fix = "Rename.", pattern = "naming"),
    )
    val runs = linkedMapOf(
        "a finding that blocks" to (envelope(mapOf("structured_output" to mapOf("findings" to findings))) to 0),
        "no findings" to (envelope(mapOf("structured_output" to mapOf("findings" to emptyList<Any>()))) to 0),
        "turn limit" to (envelope(mapOf("subtype" to "error_max_turns", "is_error" to true)) to 1),
        "success without output" to (envelope() to 0),
        "not JSON" to ("Error: no key" to 1),
        "wrong shape" to (envelope(mapOf("structured_output" to mapOf("findings" to listOf(mapOf("file" to "a.py"))))) to 0),
    )
    for ((label, run) in runs) {
        val d = gate(run.first, run.second, REVIEW_SCHEMA, policy)
        println("$label: exit ${d.exit}, ${d.comments.size} comment(s)${if (d.problems.isNotEmpty()) ", " + d.problems[0] else ""}")
    }
}
```
```text
command: claude --bare -p Review the diff on standard input. --append-system-prompt-file ... ['--max-turns', '8', '--allowedTools', 'Read,Grep']
lint, the good step: []
lint, a careless step: ['no-print', 'no-json', 'no-schema', 'no-turn-limit', 'wide-tools', 'no-bare']
a finding that blocks: exit 1, 1 comment(s)
no findings: exit 0, 0 comment(s)
turn limit: exit 1, 0 comment(s), claude exited with status 1
success without output: exit 1, 0 comment(s), the result has no structured_output
not JSON: exit 1, 0 comment(s), claude exited with status 1
wrong shape: exit 1, 0 comment(s), schema $.findings[0].line: is required
```
<!-- /example -->

### The practice: a review job that fails closed

The practice is in [`exercises/60-claude-code-in-ci`](../../exercises/60-claude-code-in-ci/unit-01/practice-1/statement.md). You write the workflow (headless, bare, JSON with a schema, a turn limit, read-only tools, the criteria file, a timeout and a secret), the schema of a finding, the criteria file, the prompt builder that carries earlier findings and existing tests, and the gate that turns the run's output into a job status and comments. It is graded by test suites in Python, TypeScript, Java and Kotlin, offline. The configuration files are the same in every language, and the decision function is the code part: `review_gate.py`, `reviewGate.ts`, `ReviewGate.java` or `ReviewGate.kt`, with a provided schema checker. The statement lists nine cases, and each says what you should see when it works.

## Traps

1. **"Have the session that wrote the change also review it; it knows the code best."** It is tempting because the session has the context. The exam rejects it: that is the reason it reviews badly, since it carries the reasoning that produced the change. An independent instance sees the diff and the criteria.
2. **"Run the review again on each push with the same prompt."** It is tempting because each run is correct on its own. The exam rejects it: without the earlier findings in the prompt, every push repeats them. Include them and ask for new or unaddressed issues only.
3. **"Tell the reviewer to be conservative and report only high-confidence issues."** It is tempting because it sounds like a precision filter. The exam rejects it: the threshold is still unstated. Explicit categories, what to skip and a severity example at each level are what change the output.
4. **"The bare run will read the repository's CLAUDE.md like any other."** It is tempting because the file is in the checkout. The documentation rejects it: bare mode skips CLAUDE.md, so the criteria go in with `--append-system-prompt-file`.

## Quiz

1. A review job runs on every push and keeps posting remarks that the author has already fixed. What fixes it?
   - **a**: Raise the turn limit so that the run can compare with earlier comments
   - **b**: Put earlier reports in the prompt and ask for new issues only
   - **c**: Tell the reviewer to be more conservative about what it reports
   - **d**: Run the review inside the session that wrote the change

2. A job that drafts additional checks keeps proposing situations that are already exercised. What belongs in its prompt?
   - **a**: A request for many more checks so that coverage becomes certain
   - **b**: The production code alone, to keep the whole prompt short
   - **c**: A lower turn limit, so that fewer proposals come back at all
   - **d**: The test files that exist, plus a request to skip covered cases

<details>
<summary>Answer key</summary>

1. **b**. A run has no memory of the last one, so what was reported must be in the prompt. *a* is ruled out because "A CI run does not remember the last one", and a longer loop does not change that. *c* is ruled out because such wording does not change what a reviewer reports, since vague instructions "leave the threshold unstated". *d* is ruled out because the writing session is "less effective at reviewing its own changes" and still would not know what was said on earlier pushes.
2. **d**. The remedy is to show the run what the suite already covers. *a* is ruled out because more proposals add duplicates, while the aim is "so that suggestions do not duplicate covered scenarios". *b* is ruled out because without the tests the run cannot see what is covered: "put the existing tests in the prompt". *c* is ruled out because a record of earlier work carried by the prompt "is the only state the pipeline has", and a turn limit adds none.

</details>

## Module quiz

This quiz covers all three pages of the module.

1. Scenario S5, Claude Code for continuous integration. A team runs Claude Code in CI to review pull requests and to suggest tests. A team asks the same session that wrote a change to review it afterwards, and the verdicts are nearly always approvals. Which change fits?
   - **a**: Ask the same session to think harder before it approves
   - **b**: Give the session the full history of the change to review against
   - **c**: Start a separate run that receives only the diff and the criteria
   - **d**: Lower the number of turns so that the session decides faster

2. Scenario S5, Claude Code for continuous integration. A team runs Claude Code in CI to review pull requests and to suggest tests. A job runs `claude --bare -p`, and its reviews ignore the testing and severity rules that sit in the repository's `CLAUDE.md`. What is the cause and the fix?
   - **a**: The file is too long, so cut it until it fits the prompt
   - **b**: Headless runs read the file only on the first push of a branch
   - **c**: The rules need a `paths` header before a headless run applies them
   - **d**: That mode skips it, so supply it with `--append-system-prompt-file`

3. Scenario S5, Claude Code for continuous integration. A team runs Claude Code in CI to review pull requests and to suggest tests. The policy keeps findings at medium severity or above, disables the style category, and fails the job on high findings. A valid run returns one high style finding and one low bug finding. What does the job do?
   - **a**: It passes and posts no comment
   - **b**: It fails, because a high finding exists
   - **c**: It passes and posts the low bug as a comment
   - **d**: It fails, because a finding in a disabled category counts as an error

<details>
<summary>Answer key</summary>

1. **c**. An independent run sees the change without the reasoning that produced it. *a* is ruled out because the writing session is "less effective at reviewing its own changes", and more effort does not remove the bias. *b* is ruled out because a reviewer "sees only the diff and the criteria you give it, not the reasoning that produced the change". *d* is ruled out because "the independent run is a separate invocation, not a second pass in the same conversation".
2. **d**. Bare mode skips CLAUDE.md, so the criteria must be passed in by hand. *a* is ruled out because length is not the issue: "In a bare run the file is passed explicitly" and otherwise it is not read at all. *b* is ruled out because "A CI run does not remember the last one", and no first-push behaviour exists. *c* is ruled out because no header is involved, and "a bare run without it reviews with no criteria at all, and nothing says so".
3. **a**. The high finding is dropped with its category and the low one falls below the floor, so nothing is kept. *b* is ruled out because "categories the team disabled are dropped", so the high style finding cannot fail the job. *c* is ruled out because "findings below a floor are dropped", and a low finding is below a medium floor. *d* is ruled out because only "a kept finding at a failing severity" fails the job, and a disabled category is dropped before that.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
