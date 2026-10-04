import json
import os
import re
import shlex
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution: the project folder that holds ci/.
ROOT = Path(os.environ.get("SOLUTION_DIR", Path(__file__).resolve().parent.parent / "starter"))

VAGUE = ("be conservative", "be careful", "only report important", "high confidence", "use good judgement", "use your judgment")


def read(rel):
    path = ROOT / rel
    assert path.is_file(), f"{rel} is missing"
    return path.read_text()


def load_json(rel):
    try:
        return json.loads(read(rel))
    except json.JSONDecodeError as error:
        raise AssertionError(f"{rel} is not valid JSON: {error}")


def jobs():
    return {j["name"]: j for j in load_json("ci/pipeline.json").get("jobs", [])}


def job(name):
    found = jobs().get(name)
    assert found, f"ci/pipeline.json has no job named {name}"
    return found


def section(text, title):
    match = re.search(rf"^## {title}\s*\n(.*?)(?=^## |\Z)", text, re.M | re.S)
    return [line[2:].strip() for line in match.group(1).splitlines() if line.startswith("- ")] if match else []


def test_m1_who_waits_decides_between_real_time_and_batch():
    assert job("pre-merge-review")["api"] == "realtime", "a developer waits for the merge check, so it runs in real time"
    assert job("debt-report")["api"] == "batch", "nobody waits for the overnight report, so it runs as a batch"
    for j in jobs().values():
        expected = "realtime" if j["audience"] == "waiting" else "batch"
        assert j["api"] == expected, f"{j['name']}: the audience is {j['audience']}, so the api is {expected}"


def test_e1_a_review_has_a_pass_for_each_file_and_then_an_integration_pass():
    assert job("pre-merge-review")["passes"] == ["per-file", "integration"], "one pass per file for local issues, then one pass across files, in that order"


def test_e2_a_review_runs_in_a_fresh_session_and_is_given_the_earlier_findings():
    review = job("pre-merge-review")
    assert review["session"] == "fresh", "the session that wrote the code is biased toward it"
    assert "prior_findings" in review["context"], "a re-run needs the earlier findings so that it reports only what is new"


def test_e3_every_claude_command_is_headless_json_and_bounded():
    for j in jobs().values():
        tokens = shlex.split(j["command"])
        if tokens[0] != "claude":
            continue
        assert "-p" in tokens or "--print" in tokens, f"{j['name']}: without -p the run waits for input"
        assert "--output-format" in tokens and tokens[tokens.index("--output-format") + 1] == "json", f"{j['name']}: ask for json output"
        assert "--max-turns" in tokens and tokens[tokens.index("--max-turns") + 1].isdigit(), f"{j['name']}: bound the run with --max-turns"
    tokens = shlex.split(job("pre-merge-review")["command"])
    assert "--json-schema" in tokens, "the review answers in a schema"
    schema = load_json(tokens[tokens.index("--json-schema") + 1])
    item = schema["properties"]["findings"]["items"]
    assert item["properties"]["severity"].get("enum"), "severity is a closed list in the schema"
    assert {"file", "line", "severity", "issue", "suggestion"} <= set(item.get("required", [])), "a finding is required to say where, how bad, what and what to do"


def test_e4_the_review_can_only_read():
    review = job("pre-merge-review")
    assert review["tools"] and set(review["tools"]) <= {"Read", "Grep", "Glob"}, f"read-only tools only, found {review['tools']}"
    tokens = shlex.split(review["command"])
    assert "--allowedTools" in tokens, "list the allowed tools in the command"
    assert set(tokens[tokens.index("--allowedTools") + 1].split(",")) <= {"Read", "Grep", "Glob"}, "the command approves read tools only"


def test_e5_the_criteria_name_what_to_report_what_to_skip_and_an_example_for_each_severity():
    text = read("ci/review-criteria.md")
    assert len(section(text, "Report")) >= 2, "list at least two kinds of issue to report"
    assert len(section(text, "Skip")) >= 2, "list at least two kinds of issue to skip"
    levels = {line.split(":")[0]: line for line in section(text, "Severity")}
    for level in ("high", "medium", "low"):
        assert level in levels and "Example:" in levels[level], f"{level} needs a description and an Example:"
    assert not [p for p in VAGUE if p in text.lower()], "a vague instruction does not make a review more precise: name the cases"


def test_e6_the_test_prompt_passes_the_existing_tests_and_says_what_a_useful_test_is():
    text = read("ci/testgen-prompt.md")
    assert "{{existing_tests}}" in text and "{{changed_files}}" in text, "the prompt carries the changed files and the existing tests"
    assert len(section(text, "A useful test")) >= 3, "say what a useful test is, in at least three points"
    assert len(section(text, "Do not write")) >= 2, "say what not to write, in at least two points"


def test_e7_no_file_holds_a_personal_path_an_address_or_a_key():
    hits = []
    for path in sorted(ROOT.rglob("*")):
        if path.is_file():
            text = path.read_text(errors="ignore")
            for label, pattern in (("home path", r"(/home/\w+|/Users/\w+|C:\\Users)"), ("email address", r"[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"), ("key", r"sk-ant-[\w-]{6,}")):
                if re.search(pattern, text):
                    hits.append(f"{path.relative_to(ROOT)}: {label}")
    assert not hits, hits
