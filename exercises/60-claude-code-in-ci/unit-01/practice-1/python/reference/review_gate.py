"""The decision of a review job and the prompt of a review run. See ../../statement.md."""
import json

from schema_check import schema_check

SEVERITIES = ["low", "medium", "high"]


def review_prompt(diff, prior=(), existing_tests=()):
    lines = ["<instructions>", "Review the change in <diff> against the criteria in the project instructions.", "Report only findings that are new or still unaddressed."]
    if prior:
        lines.append("Do not repeat a finding listed in <already_reported>.")
    if existing_tests:
        lines.append("Do not suggest a test for a behaviour that an existing test in <existing_tests> already covers.")
    lines.append("</instructions>")
    if prior:
        lines += ["<already_reported>"] + [f"- {p['file']}:{p['line']} [{p['category']}] {p['issue']}" for p in prior] + ["</already_reported>"]
    if existing_tests:
        lines += ["<existing_tests>"] + [f"- {t}" for t in existing_tests] + ["</existing_tests>"]
    lines += ["<diff>", diff, "</diff>"]
    return "\n".join(lines)


def gate(stdout, exit_code, schema, policy):
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
