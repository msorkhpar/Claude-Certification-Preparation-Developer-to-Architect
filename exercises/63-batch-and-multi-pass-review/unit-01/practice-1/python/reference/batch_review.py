"""Batch and multi-pass review decisions: when a batch fits, what to resubmit, how a review is split into passes, and how the passes are combined. See ../../statement.md."""

SEVERITIES = ("low", "medium", "high")


def submission_interval(sla_hours, window_hours=24, handling_hours=2):
    interval = sla_hours - window_hours - handling_hours
    if interval <= 0:
        raise ValueError("the SLA leaves no room to wait for a batch to fill")
    return interval


def choose_api(blocking, needs_tool_loop=False):
    return "synchronous" if blocking or needs_tool_loop else "batch"


def resubmission_plan(results, sizes, limit):
    plan = []
    for custom_id, kind in results:
        if kind == "succeeded":
            continue
        if sizes.get(custom_id, 0) > limit:
            action = "chunk"
        elif kind == "invalid_request":
            action = "fix"
        else:
            action = "resubmit"
        plan.append((custom_id, action))
    return plan


def review_plan(files):
    passes = [{"name": f"local:{f}", "files": [f]} for f in files]
    if len(files) > 1:
        passes.append({"name": "integration", "files": list(files)})
    return passes


def merge_passes(passes):
    merged = {}
    for number, findings in enumerate(passes):
        for f in findings:
            key = (f["file"], f["line"], f["issue"])
            m = merged.get(key)
            if m is None:
                m = merged[key] = {"file": f["file"], "line": f["line"], "issue": f["issue"], "severity": f["severity"], "passes": set(), "confidence": f["confidence"]}
            if SEVERITIES.index(f["severity"]) > SEVERITIES.index(m["severity"]):
                m["severity"] = f["severity"]
            m["confidence"] = min(m["confidence"], f["confidence"])
            m["passes"].add(number)
    out = []
    for m in merged.values():
        count = len(m["passes"])
        out.append({**m, "passes": count, "route": "accept" if count >= 2 and m["confidence"] >= 80 else "verify"})
    return out
