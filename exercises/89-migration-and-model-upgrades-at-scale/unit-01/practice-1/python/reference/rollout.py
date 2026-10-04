"""Rollout kit: the retirement calendar, the settings a new model refuses, a gate on a regression suite and a staged roll-out. See ../../statement.md."""
from collections import namedtuple
from datetime import date

Case = namedtuple("Case", "id segment must_pass old_ok new_ok old_cost new_cost new_ms")
Request = namedtuple("Request", "model temperature top_p top_k thinking tool_choice strict prefill")
STAGES = [1, 5, 25, 100]
TARGET = "claude-sonnet-5-5"


def days_until(today, when):
    return (date.fromisoformat(when) - date.fromisoformat(today)).days


def percentile(values, p):
    ordered = sorted(values)
    return ordered[(p * len(ordered) + 99) // 100 - 1] if ordered else 0


def retirement_status(models, today):
    rows = sorted((days_until(today, when), name, tentative) for name, when, tentative in models)
    out = []
    for days, name, tentative in rows:
        level = "retired" if days < 0 else "urgent" if days <= 14 else "migrate now" if days <= 60 else "watch"
        out.append(f"{name}: {days} days, {level}{' (tentative)' if tentative else ''}")
    return out


def migrate_request(request, target=TARGET):
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
    return Request(target, None, None, None, thinking, tool_choice, strict, False), changes


def gate(cases, protected, max_cost_up, max_p95):
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
    if requests < min_requests:
        return f"hold at {stage}"
    if errors * 1000 // requests > max_errors_per_1000:
        return "rollback to 0"
    if stage == STAGES[-1]:
        return "complete"
    return f"advance to {STAGES[STAGES.index(stage) + 1]}"
