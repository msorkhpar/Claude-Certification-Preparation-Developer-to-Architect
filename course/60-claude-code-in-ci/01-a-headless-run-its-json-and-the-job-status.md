# A headless run, its JSON and the job status

**Level:** Architect · **Module 60:** Claude Code in CI · **Page 1 of 3**
**Exams:** A3.6; S5

**After this page you can** run Claude Code without a person with `-p`, ask for one machine-readable object with `--output-format json` and `--json-schema`, read the answer from `structured_output`, give the run a turn limit and read-only tools, and write the gate that fails the job whenever the run did not produce a valid answer.

Checked on 2026-10-03 against the Claude Code documentation pages "Run Claude Code programmatically", "CLI reference", "Claude Code GitHub Actions" and the Agent SDK pages on structured outputs, documenting behaviour up to Claude Code v2.1.286. Nothing here ran the real binary, needed a key or touched the network: the example builds the command and decides the job from sample output shaped like the documented result message. This page deepens module 38 (headless runs and the flags that bound them) and module 40 (Claude in the software life cycle) and does not repeat them. What the run knows, and the criteria, are the second page.

> **Exam guide and current product.** *What the guide states (task statement 3.6), and so what the exam keys:* for CI/CD integration it names the `-p` (or `--print`) flag for non-interactive mode, `--output-format json` together with `--json-schema` to produce structured findings that can be posted as inline comments, and CLAUDE.md as the way to give the CI run project context. *What the current product does (documentation checked 2026-10-03):* all of it. `-p` runs a prompt non-interactively; with `--output-format json` the output is one object, and with `--json-schema` it "includes metadata about the request (session ID, usage, etc.) with the structured output in the `structured_output` field". An invalid schema is an error: "`claude` exits with `Error: --json-schema is not a valid JSON Schema`". The `format` keyword is accepted but only as an annotation. `--max-turns` "Exits with an error when the limit is reached". `--bare` is recommended for scripted calls, and a bare run skips CLAUDE.md. On the exam, the guide's three items are the answer; for a real pipeline add the failure handling on this page.

## Why it matters

A team adds a review step to its pull requests. The first version runs `claude -p "review this"` and writes the prose to a file; it exits with status 0 whatever happened. One night the run hits its turn limit and prints an error, the job stays green, and a pull request with an unchecked `None` is merged under a green check. Scenario S5 asks which design makes the step trustworthy, and the exam's answer has two halves: make the output machine-readable so it can be checked, and make every kind of failure visible in the job status.

## The idea

### The command

A CI review step has seven parts, and each one answers a question:

| Part | Flag | The question it answers |
|---|---|---|
| Non-interactive | `-p` | Who answers prompts? Nobody: the run starts, works and exits |
| One object out | `--output-format json` | How does a script read the result? As one JSON object |
| A checked answer | `--json-schema '<schema>'` | What shape must the answer have? The schema's, validated before the run ends |
| A bounded run | `--max-turns 8` | What stops a loop? The run exits with an error at the limit |
| Least privilege | `--allowedTools "Read,Grep,Glob"` | What may it do without asking? Read and search only |
| A reproducible start | `--bare` | Which machine's settings apply? None: no hooks, skills, MCP servers, memory or CLAUDE.md |
| The context | `--append-system-prompt-file CLAUDE.md` | Where do the criteria come from? The file, passed on purpose |

A bare run needs `ANTHROPIC_API_KEY` in the environment, because it does not use a subscription login; in the workflow that is a secret reference, never a literal. The diff goes in on standard input or in the prompt. In a headless run nobody can approve a prompt, so the tool list must be complete and minimal: read tools for a review, and no bare `Bash`, which would let the run execute anything.

### What the run prints

With `--json-schema`, the object has the answer in `structured_output` beside the run's metadata: the result text, the session id, the cost. The documented result message also carries a `subtype`: `success`, or an error such as `error_max_turns` or `error_max_structured_output_retries` ("No valid output remained after multiple attempts"), with `is_error` set for the failures. One more case is documented on purpose: "A result can also end with subtype `success` but no `structured_output` value ... Treat that case as a failure as well."

The JSON shape of the command-line output follows the SDK's result message; the command-line pages document `result`, `structured_output`, `session_id` and the cost fields, and the SDK pages document the subtypes. The course's model uses the SDK's field names and was not compared with the binary's output.

### The gate

A job's status is a decision, and a decision made on prose is a guess. The gate reads the exit status of `claude` and the JSON, and fails the job in every case where no valid answer exists:

1. `claude` exited with a non-zero status;
2. the output is not a JSON object;
3. `is_error` is set, or the `subtype` is not `success`;
4. there is no `structured_output`;
5. the answer breaks the schema.

A job never passes by saying nothing. Retrying inside the job repeats cost with no reason to expect a different result, so the failure is reported and a person or a scheduled run decides. Only an answer that clears all five is read for findings, and then policy decides what a finding does: findings below a floor are dropped, categories the team disabled are dropped, and a kept finding at a failing severity (`high`, say) fails the job while lower ones only comment. This is the same fail-closed rule that module 48 applies to a workflow's gates.

The schema has two jobs. It makes the answer checkable, and with closed enums for category and severity it makes the policy computable: a severity that is free text cannot be compared with a floor. Keep to the features structured outputs support (types, `enum`, `required`, nested objects and arrays); make a field optional only when the value can really be absent.

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

## Traps

1. **"Run `claude -p` and treat exit status 0 as a passed review."** It is tempting because the step ran. The exam rejects it: a run can end in a limit, an error subtype or an empty result, so the job must read the JSON and fail unless a valid answer is present.
2. **"Ask for prose and parse the findings with a regular expression."** It is tempting because it needs no schema. The exam rejects it: `--output-format json` with `--json-schema` returns a validated object in `structured_output`, which is what inline comments are built from.
3. **"Allow `Bash` so the review can run `git diff`."** It is tempting because the review needs the diff. The exam rejects it: a bare `Bash` allows any command in an unattended run. Pass the diff in, or list a narrow pattern such as `Bash(git diff *)`.
4. **"A `success` subtype means the findings are there."** It is tempting because success sounds final. The documentation rejects it: a run can end with `success` and no `structured_output`, and that is a failure.

## Quiz

1. A CI step runs Claude Code unattended, and each issue it reports must be posted as an inline note. Which flags make the reply usable for that?
   - **a**: `--output-format text` with a prompt asking for a bulleted list
   - **b**: `--max-turns` with a high limit so that the list is complete
   - **c**: `--output-format json` with `--json-schema`
   - **d**: `--bare` alone, which formats replies for scripts

2. A review run ends with the subtype `success`, yet the object holds no `structured_output`. What should the job do?
   - **a**: Fail, because no valid answer exists
   - **b**: Pass, since the run reports success
   - **c**: Retry the same request until a value appears
   - **d**: Pass, with a note that nothing was reported

<details>
<summary>Answer key</summary>

1. **c**. The schema makes the answer a validated object in `structured_output`. *a* is ruled out because text leaves the entries to be parsed from prose, and "a decision made on prose is a guess". *b* is ruled out because a limit bounds the loop and does not shape the answer: "The run exits with an error at the limit". *d* is ruled out because the flag controls what the run loads, with "no hooks, skills, MCP servers, memory or CLAUDE.md", and formats nothing.
2. **a**. A run without a valid answer is a failure whatever its subtype says. *b* is ruled out because the documentation says "Treat that case as a failure as well". *c* is ruled out because "Retrying inside the job repeats cost with no reason to expect a different result". *d* is ruled out because "A job never passes by saying nothing", and a silent pass cannot be told from a failed run.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
