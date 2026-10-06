import json
import shlex

from ci_gate import REVIEW_SCHEMA, build_command, envelope, gate, lint_command, schema_check

POLICY = {"min_severity": "medium", "disabled_categories": ["style"], "fail_on": ["high"]}


def finding(**over):
    return {"file": "api.py", "line": 12, "category": "bug", "severity": "medium", "issue": "Unchecked None.", "suggested_fix": "Return early.", "detected_pattern": "missing-none-check", **over}


def run(findings, code=0, **over):
    return gate(envelope(structured_output={"findings": findings}, **over), code, REVIEW_SCHEMA, POLICY)


def test_the_command_is_headless_json_schema_checked_bounded_and_read_only():
    argv = build_command("Review.", {"type": "object"})
    assert argv[:3] == ["claude", "--bare", "-p"] and argv[argv.index("--output-format") + 1] == "json"
    assert json.loads(argv[argv.index("--json-schema") + 1]) == {"type": "object"}
    assert argv[argv.index("--max-turns") + 1] == "8" and argv[argv.index("--allowedTools") + 1] == "Read,Grep,Glob,Bash(git diff *)"
    assert lint_command(shlex.join(argv)) == []


def test_lint_names_what_a_careless_step_lacks():
    assert lint_command("claude 'Review it'") == ["no-print", "no-json", "no-schema", "no-turn-limit", "no-tool-list", "no-bare"]
    assert "wide-tools" in lint_command("claude -p x --allowedTools Bash,Read") and "wide-tools" in lint_command("claude -p x --allowedTools 'Read,Edit'")
    assert "wide-tools" not in lint_command("claude -p x --allowedTools 'Bash(git diff *)'")
    assert "bare-without-context" in lint_command("claude --bare -p x --output-format json --json-schema '{}' --max-turns 5 --allowedTools Read")
    assert "no-turn-limit" in lint_command("claude -p x --max-turns 50") and lint_command("no tool here") == ["no-claude-command"]


def test_the_schema_check_reads_types_enums_required_and_extra_keys():
    assert schema_check({"findings": []}, REVIEW_SCHEMA) == []
    assert schema_check({"findings": [finding(line="12")]}, REVIEW_SCHEMA) == ["$.findings[0].line: expected integer"]
    assert schema_check({"findings": [finding(severity="critical")]}, REVIEW_SCHEMA)[0].startswith("$.findings[0].severity: 'critical' is not one of")
    extra = finding()
    extra["confidence"] = 0.9
    assert schema_check({"findings": [extra]}, REVIEW_SCHEMA) == ["$.findings[0].confidence: is not allowed"]


def test_a_good_run_posts_the_findings_above_the_floor_and_outside_the_disabled_categories():
    result = run([finding(), finding(category="style", severity="medium"), finding(line=3, severity="low")])
    assert result == {"exit": 0, "comments": [{"file": "api.py", "line": 12, "severity": "medium", "body": "Unchecked None. Suggested fix: Return early."}], "problems": []}


def test_a_high_finding_fails_the_job_and_no_findings_pass_it():
    assert run([finding(severity="high")])["exit"] == 1 and run([finding(severity="high")])["comments"][0]["severity"] == "high"
    assert run([])["exit"] == 0


def test_a_failed_run_fails_the_job_instead_of_passing_it_silently():
    for out, code, word in ((envelope(subtype="error_max_turns", is_error=True), 1, "error_max_turns"), (envelope(subtype="error_max_structured_output_retries", is_error=True), 1, "retries"),
                            (envelope(), 0, "structured_output"), ("Error: no key", 1, "not a JSON"), ("[1]", 0, "not a JSON")):
        result = gate(out, code, REVIEW_SCHEMA, POLICY)
        assert result["exit"] == 1 and result["comments"] == [] and any(word in p for p in result["problems"]), (out, result)
    assert "exited with status 2" in gate(envelope(structured_output={"findings": []}), 2, REVIEW_SCHEMA, POLICY)["problems"][0]
