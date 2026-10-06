"""The decision of a review job and the prompt of a review run. See ../../statement.md."""
import json

from schema_check import schema_check
import logging

log = logging.getLogger(__name__)

SEVERITIES = ["low", "medium", "high"]


def review_prompt(diff, prior=(), existing_tests=()):
    # TODO 1 of 9 (finish this to pass e4): the first instructions of the prompt. Add the sentence "Report only findings
    #   that are new or still unaddressed." as the third line of the instructions, after the line that points to the
    #   project criteria. Example: the prompt starts with <instructions>, the review line, then this sentence.
    lines = ["<instructions>", "Review the change in <diff> against the criteria in the project instructions."]
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
    log.debug("gate input: %r", stdout)
    problems = []
    # TODO 2 of 9 (finish this to pass e1): the exit status. When claude exited with a status other than 0, add the
    #   problem "claude exited with status N" (the job fails whatever the output looks like). Example: exit 2 and a
    #   perfect answer -> exit 1 and that problem.
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
        # TODO 3 of 9 (finish this to pass e2): the schema check. Check the structured output against the schema with
        #   the provided helper and add one problem per error, as "schema " followed by the error (which names the path).
        #   Example: severity "critical" -> "schema $.findings[0].severity: ...".
        pass
    if problems:
        return {"exit": 1, "comments": [], "problems": problems}
    # TODO 4 of 9 (finish this to pass m1): the comments of a valid answer. Keep each finding whose severity is at or
    #   above policy min_severity (the order is low, medium, high) and whose category is not in disabled_categories.
    #   Return one comment {file, line, severity, body} per kept finding, in order, with the body "ISSUE Suggested fix:
    #   FIX". Example: a medium bug at api.py:12 with floor medium -> one comment.
    comments = []
    # TODO 5 of 9 (finish this to pass e3): the decision. The job fails (exit 1) when at least one posted comment has a
    #   severity listed in policy fail_on; otherwise it only comments (exit 0). Example: fail_on [high], one posted medium
    #   comment -> exit 0.
    blocked = False
    return {"exit": 1 if blocked else 0, "comments": comments, "problems": []}
