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
