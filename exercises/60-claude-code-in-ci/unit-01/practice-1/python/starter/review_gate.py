"""The decision of a review job and the prompt of a review run. See ../../statement.md."""
import json  # noqa: F401

from schema_check import schema_check  # noqa: F401  (provided: errors of a value against a schema)

SEVERITIES = ["low", "medium", "high"]


def review_prompt(diff, prior=(), existing_tests=()):
    # TODO: instructions, the findings already reported, the tests that exist, and the diff last.
    return None


def gate(stdout, exit_code, schema, policy):
    # TODO: decide the job from the exit status and the JSON the run printed: {"exit", "comments", "problems"}.
    return None
