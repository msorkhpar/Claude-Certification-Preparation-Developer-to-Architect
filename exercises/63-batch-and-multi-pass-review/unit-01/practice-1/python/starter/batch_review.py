"""Batch and multi-pass review decisions: when a batch fits, what to resubmit, how a review is split into passes, and how the passes are combined. See ../../statement.md."""

import logging

log = logging.getLogger(__name__)

SEVERITIES = ("low", "medium", "high")


def submission_interval(sla_hours, window_hours=24, handling_hours=2):
    log.debug("submission_interval input: %r", sla_hours)
    # TODO 1 of 8 (finish this to pass m1): the interval. Receives the SLA, the window and the handling time, all in
    #   hours. Return the SLA minus the window minus the handling time. Example: an SLA of 30 hours, defaults -> 4.
    interval = sla_hours
    # TODO 2 of 8 (finish this to pass e1): the refusal. When the interval is zero or negative, refuse with an error
    #   that says the SLA leaves no room to wait for a batch to fill. Example: an SLA of 26 hours, defaults -> refused.
    return interval


def choose_api(blocking, needs_tool_loop=False):
    # TODO 3 of 8 (finish this to pass e2): the API. Receives whether something is blocked on the result and whether the
    #   job needs a tool loop. Return synchronous when either is true, otherwise batch. Example: blocking false, tool loop
    #   true -> synchronous.
    return "batch"


def resubmission_plan(results, sizes, limit):
    plan = []
    for custom_id, kind in results:
        # TODO 4 of 8 (finish this to pass e3): the items to resubmit. Skip a result whose kind is succeeded; every
        #   other result is planned by its custom id. Example: results [(a, succeeded), (b, errored)] -> a plan for b
        #   only.
        # TODO 5 of 8 (finish this to pass e4): the action for one item. When the item's size is above the limit, the
        #   action is chunk; otherwise fix when its kind is invalid_request; otherwise resubmit. An entry exactly at the
        #   limit is not chunked. Example: size 101, limit 100 -> chunk.
        if kind == "invalid_request":
            action = "fix"
        else:
            action = "resubmit"
        plan.append((custom_id, action))
    return plan


def review_plan(files):
    passes = [{"name": f"local:{f}", "files": [f]} for f in files]
    # TODO 6 of 8 (finish this to pass e5): the integration pass. After the local passes, when there is more than one
    #   file add one pass named integration over all the files. Example: two files -> local, local, integration; one file
    #   -> local only.
    return passes


def merge_passes(passes):
    merged = {}
    for number, findings in enumerate(passes):
        for f in findings:
            key = (f["file"], f["line"], f["issue"])
            m = merged.get(key)
            if m is None:
                m = merged[key] = {"file": f["file"], "line": f["line"], "issue": f["issue"], "severity": f["severity"], "passes": set(), "confidence": f["confidence"]}
            # TODO 7 of 8 (finish this to pass e6): the merge of one finding seen again. When the same finding (file,
            #   line, issue) is reported by another pass, keep the higher of the two severities (low, medium, high) and
            #   the lower of the two confidences. Example: medium at 90 and high at 70 -> high at 70.
            m["passes"].add(number)
    out = []
    for m in merged.values():
        count = len(m["passes"])
        # TODO 8 of 8 (finish this to pass e7): the route of a merged finding. Receives the number of passes that
        #   reported it and its confidence. Return accept when at least 2 passes reported it and the confidence is at
        #   least 80, otherwise verify. Example: 2 passes, confidence 79 -> verify.
        out.append({**m, "passes": count, "route": "verify"})
    return out
