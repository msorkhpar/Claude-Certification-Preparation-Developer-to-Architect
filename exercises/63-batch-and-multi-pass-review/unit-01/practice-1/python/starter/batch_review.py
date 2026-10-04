"""Batch and multi-pass review decisions: when a batch fits, what to resubmit, how a review is split into passes, and how the passes are combined. See ../../statement.md."""

SEVERITIES = ("low", "medium", "high")


def submission_interval(sla_hours, window_hours=24, handling_hours=2):
    # TODO: the hours between submissions that still keep every item inside the SLA; refuse an SLA with no room.
    return None


def choose_api(blocking, needs_tool_loop=False):
    # TODO: "synchronous" or "batch" for a workload.
    return None


def resubmission_plan(results, sizes, limit):
    # TODO: from (custom_id, kind) results, the (custom_id, action) pairs for the items that did not succeed.
    return None


def review_plan(files):
    # TODO: the passes of a multi-file review, each {"name", "files"}.
    return None


def merge_passes(passes):
    # TODO: combine the findings of independent passes; each result has file, line, issue, severity, passes, confidence and route.
    return None
