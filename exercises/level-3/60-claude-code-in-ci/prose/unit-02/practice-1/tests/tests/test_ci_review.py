import json
import os
import re
import sys
from pathlib import Path

# The solution is the file beside this test.
ROOT = Path(str(Path(__file__).resolve().parent.parent / "project"))
HERE = Path(__file__).resolve()
for name in ("40-workflow-lint/python", "60-ci-gate/python"):
    sys.path.insert(0, str(next(p for p in (Path("/w/examples") / name, *(HERE.parents[i] / "examples" / name for i in range(min(len(HERE.parents), 9)))) if p.exists())))
sys.path.insert(0, str(ROOT))
from ci_gate import REVIEW_SCHEMA, envelope, lint_command  # noqa: E402  (the course's model of the command line and the result envelope)
from miniyaml import parse_yaml  # noqa: E402
from review_gate import gate, review_prompt  # noqa: E402
from schema_check import schema_check  # noqa: E402

POLICY = {"min_severity": "medium", "disabled_categories": ["style"], "fail_on": ["high"]}
VAGUE = ("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment")


def read(rel):
    path = ROOT / rel
    assert path.is_file(), f"{rel} is missing"
    return path.read_text()


def finding(**over):
    return {"file": "api.py", "line": 12, "category": "bug", "severity": "medium", "issue": "Unchecked None.", "suggested_fix": "Return early.", "detected_pattern": "missing-none-check", **over}


def run(findings, code=0, policy=POLICY, **over):
    result = gate(envelope(structured_output={"findings": findings}, **over), code, REVIEW_SCHEMA, policy)
    assert isinstance(result, dict), "gate returned nothing"
    return result


def test_m1_a_valid_run_posts_the_findings_above_the_floor_and_outside_the_disabled_categories():
    result = run([finding(), finding(category="style", severity="medium"), finding(line=3, severity="low"), finding(file="ui.py", line=7, category="security", severity="medium", issue="Unescaped input.", suggested_fix="Escape it.")])
    assert result == {"exit": 0, "problems": [], "comments": [{"file": "api.py", "line": 12, "severity": "medium", "body": "Unchecked None. Suggested fix: Return early."},
                                                             {"file": "ui.py", "line": 7, "severity": "medium", "body": "Unescaped input. Suggested fix: Escape it."}]}
    assert run([]) == {"exit": 0, "comments": [], "problems": []}


def test_e1_a_failed_run_fails_the_job_instead_of_passing_it_silently():
    cases = ((envelope(subtype="error_max_turns", is_error=True), 1, "error_max_turns"), (envelope(subtype="error_max_structured_output_retries", is_error=True), 1, "retries"),
             (envelope(), 0, "structured_output"), (envelope(is_error=True, structured_output={"findings": []}), 0, "ended"), ("Error: no key", 1, "not a JSON"), ("[1]", 0, "not a JSON"), ("", 0, "not a JSON"))
    for out, code, word in cases:
        result = gate(out, code, REVIEW_SCHEMA, POLICY)
        assert isinstance(result, dict) and result["exit"] == 1 and result["comments"] == [] and any(word in p for p in result["problems"]), (out, result)
    failed = run([], code=2)
    assert failed["exit"] == 1 and any("exited with status 2" in p for p in failed["problems"]), failed


def test_e2_an_answer_that_breaks_the_schema_fails_the_job_and_names_the_path_of_the_problem():
    for bad, path in (([finding(line="12")], "$.findings[0].line"), ([finding(severity="critical")], "$.findings[0].severity"), ([{k: v for k, v in finding().items() if k != "file"}], "$.findings[0].file"),
                      ([{**finding(), "confidence": 0.9}], "$.findings[0].confidence")):
        result = run(bad)
        assert result["exit"] == 1 and result["comments"] == [] and any(p.startswith(f"schema {path}") for p in result["problems"]), (bad, result)


def test_e3_a_finding_at_the_failing_severity_blocks_the_merge_and_lower_ones_only_comment():
    blocked = run([finding(severity="high"), finding(line=30)])
    assert blocked["exit"] == 1 and [c["severity"] for c in blocked["comments"]] == ["high", "medium"] and blocked["problems"] == []
    assert run([finding(severity="medium")])["exit"] == 0
    assert run([finding(severity="medium")], policy={**POLICY, "fail_on": ["medium", "high"]})["exit"] == 1
    assert run([finding(severity="high", category="style")])["exit"] == 0, "a disabled category cannot block"


def test_e4_the_prompt_lists_earlier_findings_and_existing_tests_and_asks_for_new_or_unaddressed_issues_only():
    prior = [{"file": "api.py", "line": 12, "category": "bug", "issue": "Unchecked None."}, {"file": "ui.py", "line": 7, "category": "security", "issue": "Unescaped input."}]
    full = review_prompt("+ x = 1", prior, ["test_empty_cart", "test_two_items"])
    assert full == "\n".join(["<instructions>", "Review the change in <diff> against the criteria in the project instructions.", "Report only findings that are new or still unaddressed.",
                              "Do not repeat a finding listed in <already_reported>.", "Do not suggest a test for a behaviour that an existing test in <existing_tests> already covers.", "</instructions>",
                              "<already_reported>", "- api.py:12 [bug] Unchecked None.", "- ui.py:7 [security] Unescaped input.", "</already_reported>", "<existing_tests>", "- test_empty_cart", "- test_two_items",
                              "</existing_tests>", "<diff>", "+ x = 1", "</diff>"])
    first = review_prompt("+ x = 1")
    assert first == "\n".join(["<instructions>", "Review the change in <diff> against the criteria in the project instructions.", "Report only findings that are new or still unaddressed.", "</instructions>", "<diff>", "+ x = 1", "</diff>"])


def test_e5_the_schema_file_is_valid_draft_07_and_requires_every_field_of_a_finding():
    try:
        schema = json.loads(read("review-schema.json"))
    except json.JSONDecodeError as error:
        raise AssertionError(f"review-schema.json is not valid JSON: {error}")
    assert schema.get("$schema", "http://json-schema.org/draft-07/schema#") == "http://json-schema.org/draft-07/schema#", "the SDK validates draft-07 and rejects a newer $schema"

    def keywords(node):
        found = set()
        if isinstance(node, dict):
            for k, v in node.items():
                if k == "properties" and isinstance(v, dict):
                    for sub in v.values():
                        found |= keywords(sub)
                else:
                    found.add(k)
                    found |= keywords(v)
        elif isinstance(node, list):
            for v in node:
                found |= keywords(v)
        return found

    banned = {"minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf", "minLength", "maxLength"}
    assert not (banned & keywords(schema)), "structured outputs do not support numeric or string constraints"
    items = schema.get("properties", {}).get("findings", {}).get("items", {})
    assert set(items.get("required", [])) == {"file", "line", "category", "severity", "issue", "suggested_fix", "detected_pattern"}, "every field of a finding is required"
    assert items.get("additionalProperties") is False and schema.get("additionalProperties") is False and schema.get("required") == ["findings"]
    props = items.get("properties", {})
    assert set(props.get("severity", {}).get("enum", [])) == {"low", "medium", "high"} and {"bug", "security", "style", "other"} <= set(props.get("category", {}).get("enum", []))
    assert props.get("line", {}).get("type") == "integer"
    assert schema_check({"findings": [finding()]}, schema) == [] and schema_check({"findings": []}, schema) == []
    for bad in ({k: v for k, v in finding().items() if k != "detected_pattern"}, finding(severity="critical"), finding(line="12"), {**finding(), "confidence": 0.9}):
        assert schema_check({"findings": [bad]}, schema), f"the schema accepted {bad}"


def test_e6_the_workflow_runs_claude_headless_with_json_output_a_schema_a_turn_limit_read_only_tools_and_the_context_file():
    wf = parse_yaml(read(".github/workflows/claude-review.yml"))
    assert "pull_request" in (wf["on"] if isinstance(wf["on"], dict) else {wf["on"]: 1}), "run the review on pull requests"
    job = wf["jobs"]["review"]
    assert isinstance(job.get("timeout-minutes"), int) and job["timeout-minutes"] <= 30, "a stuck run must not hold the runner"
    assert (job.get("permissions") or {}).get("contents") == "read", "a review reads the repository"
    step = next((s for s in job["steps"] if "claude" in str(s.get("run", ""))), None)
    assert step is not None, "a step runs claude"
    assert str((step.get("env") or {}).get("ANTHROPIC_API_KEY", "")).startswith("${{ secrets."), "the key comes from the secrets context"
    command = str(step["run"])
    assert lint_command(command) == [], lint_command(command)
    assert int(re.search(r"--max-turns[ =](\d+)", command).group(1)) <= 10
    tools = re.findall(r"[^\s,(]+(?:\([^)]*\))?", re.search(r"--allowed[Tt]ools[ =](\"[^\"]*\"|\S+)", command).group(1).strip("\""))
    assert tools and all(t in ("Read", "Grep", "Glob") or re.fullmatch(r"Bash\(git (diff|log|show|status)( \*)?\)", t) for t in tools), f"a review needs read-only tools: {tools}"
    assert re.search(r"--json-schema\s+\"\$\(cat review-schema\.json\)\"", command), "pass the schema file to --json-schema"
    assert re.search(r"--append-system-prompt-file\s+CLAUDE\.md", command), "--bare skips CLAUDE.md, so pass it by hand"


def section(text, title):
    m = re.search(rf"(?ims)^## {re.escape(title)}\s*\n(.*?)(?=^## |\Z)", text)
    assert m, f"CLAUDE.md has no section '## {title}'"
    return [line[2:] for line in m.group(1).splitlines() if line.startswith("- ")]


def test_e7_the_project_file_states_what_to_report_and_what_to_skip_with_a_severity_example_for_each_level():
    text = read("CLAUDE.md")
    assert not [p for p in VAGUE if p in text.lower()], "a general instruction like be conservative does not improve precision: name the patterns"
    report, skip = section(text, "Report"), section(text, "Skip")
    assert len(report) >= 3 and any("bug" in r.lower() for r in report) and any("security" in r.lower() for r in report), "list at least three categories to report, among them bugs and security"
    assert len(skip) >= 2 and any("style" in s.lower() for s in skip), "list what to skip, minor style among it"
    severity = section(text, "Severity")
    for level in ("high", "medium", "low"):
        line = next((s for s in severity if s.lower().startswith(f"{level}:")), None)
        assert line and re.search(r"`[^`]+`", line), f"severity {level} needs a concrete example in code"
    assert any("tests/fixtures/" in s for s in section(text, "Testing standards")), "name the fixtures folder in the testing standards"


def test_e8_no_file_holds_a_personal_path_an_address_or_a_key():
    hits = []
    for path in sorted(ROOT.rglob("*")):
        if path.is_file() and "__pycache__" not in path.parts:
            text = path.read_text(errors="ignore")
            for label, pattern in (("home path", r"(/home/\w+|/Users/\w+|C:\\Users)"), ("email address", r"[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"), ("key", r"sk-ant-[\w-]{6,}")):
                if re.search(pattern, text):
                    hits.append(f"{path.relative_to(ROOT)}: {label}")
    assert not hits, hits
