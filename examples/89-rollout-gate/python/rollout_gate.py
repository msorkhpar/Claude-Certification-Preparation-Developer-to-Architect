"""Moving a system to a new model at scale: the calendar of retirements, the settings a new model refuses, a gate that a regression suite must pass, and a staged roll-out with a way back.

The cases, costs, timings and counts are invented. The model names and dates are the ones the Claude documentation listed on 2026-10-04; the rules about settings are those of its migration guide for Claude Sonnet 5.5. Nothing here calls a model.
"""
import logging

log = logging.getLogger(__name__)
from collections import namedtuple
from datetime import date

Case = namedtuple("Case", "id segment must_pass old_ok new_ok old_cost new_cost new_ms")
Request = namedtuple("Request", "model temperature top_p top_k thinking tool_choice strict prefill")
STAGES = [1, 5, 25, 100]
TARGET = "claude-sonnet-5-5"


def days_until(today, when):
    return (date.fromisoformat(when) - date.fromisoformat(today)).days


def level(days):
    if days < 0:
        return "retired"
    if days <= 14:
        return "urgent"
    return "migrate now" if days <= 60 else "watch"


def retirement_status(models, today):
    """One line per model, the nearest retirement first. A date marked tentative is a date that may move later."""
    rows = sorted((days_until(today, when), name, tentative) for name, when, tentative in models)
    return [f"{name}: {days} days, {level(days)}{' (tentative)' if tentative else ''}" for days, name, tentative in rows]


def migrate_request(request, target=TARGET):
    """The settings the target model refuses are removed or replaced, and each change is named."""
    changes = []
    if request.model != target:
        changes.append(f"model set to {target}")
    for field in ("temperature", "top_p", "top_k"):
        if getattr(request, field) is not None:
            changes.append(f"removed {field}")
    thinking = request.thinking
    if thinking == "budget":
        thinking = "adaptive"
        changes.append("thinking budget replaced by adaptive thinking; sweep the effort")
    elif thinking == "disabled":
        thinking = "between_tools"
        changes.append("thinking disabled replaced by between_tools")
    tool_choice, strict = request.tool_choice, request.strict
    if tool_choice in ("any", "tool"):
        tool_choice, strict = "auto", True
        changes.append("forced tool choice replaced by auto with strict tools")
    if request.prefill:
        changes.append("assistant prefill removed; state the format in the instructions")
    new = Request(target, None, None, None, thinking, tool_choice, strict, False)
    return new, changes


def percentile(values, p):
    ordered = sorted(values)
    return ordered[(p * len(ordered) + 99) // 100 - 1] if ordered else 0


def gate(cases, protected, max_cost_up, max_p95):
    """A go needs every check to pass; every check that fails adds a reason, in a fixed order."""
    log.debug("gate input: %r", cases)
    reasons = []
    failed = sorted(c.id for c in cases if c.must_pass and not c.new_ok)
    if failed:
        reasons.append("must-pass failed: " + ", ".join(failed))
    lost = [c for c in cases if c.old_ok and not c.new_ok]
    gained = [c for c in cases if c.new_ok and not c.old_ok]
    hit = sorted({c.segment for c in lost if c.segment in protected})
    if hit:
        reasons.append("protected segment lost answers: " + ", ".join(hit))
    if len(lost) > len(gained):
        reasons.append(f"net loss: lost {len(lost)}, gained {len(gained)}")
    old_total, new_total = sum(c.old_cost for c in cases), sum(c.new_cost for c in cases)
    up = (new_total - old_total) * 100 // old_total if old_total > 0 and new_total > old_total else 0
    if up > max_cost_up:
        reasons.append(f"cost up {up}% over the {max_cost_up}% limit")
    p95 = percentile([c.new_ms for c in cases], 95)
    if p95 > max_p95:
        reasons.append(f"p95 latency {p95} ms over the {max_p95} ms limit")
    return {"decision": "no-go" if reasons else "go", "reasons": reasons}


def rollout_step(stage, requests, errors, min_requests, max_errors_per_1000):
    """Hold until the stage has enough requests, roll back to zero when errors pass the limit, otherwise go on."""
    if requests < min_requests:
        return f"hold at {stage}"
    if errors * 1000 // requests > max_errors_per_1000:
        return "rollback to 0"
    if stage == STAGES[-1]:
        return "complete"
    return f"advance to {STAGES[STAGES.index(stage) + 1]}"


def suite():
    rows = []
    for i, ms in enumerate((900, 950, 1000, 1100, 1200), 1):
        rows.append(Case(f"b{i}", "billing", True, True, True, 4, 5, ms))
    for i, ms in enumerate((1500, 1600, 1700, 1800, 2100), 1):
        rows.append(Case(f"r{i}", "refund", i <= 2, True, i != 4, 6, 8, ms))
    for i in range(1, 11):
        rows.append(Case(f"f{i}", "faq", False, i not in (8, 9, 10), i != 10, 2, 3, 500 + 20 * i + 80 * (i // 2)))
    return rows


def main():
    models = [("claude-haiku-4-5-20251001", "2026-10-15", True), ("claude-sonnet-4-5-20250929", "2026-11-30", False), ("claude-opus-4-1-20250805", "2026-08-05", False)]
    print("retirement calendar on 2026-10-04:")
    for line in retirement_status(models, "2026-10-04"):
        print("  " + line)
    old = Request("claude-sonnet-4-5-20250929", 0.7, 0.9, None, "budget", "tool", False, True)
    new, changes = migrate_request(old)
    print(f"request for {new.model}: {len(changes)} changes")
    for change in changes:
        print("  " + change)
    cases = suite()
    first = gate(cases, {"refund"}, 25, 2000)
    print(f"gate on {len(cases)} cases: {first['decision']}")
    for reason in first["reasons"]:
        print("  " + reason)
    fixed = [c._replace(new_ok=True) if c.id == "r4" else c for c in cases]
    second = gate(fixed, {"refund"}, 40, 2000)
    print(f"gate after the refund fix, cost limit 40%: {second['decision']}, {len(second['reasons'])} reasons")
    for stage, requests, errors in ((1, 2000, 6), (5, 300, 0), (5, 10000, 20), (25, 50000, 400), (100, 50000, 10)):
        print(f"roll-out at {stage}% with {requests} requests and {errors} errors: {rollout_step(stage, requests, errors, 1000, 5)}")


if __name__ == "__main__":
    main()
