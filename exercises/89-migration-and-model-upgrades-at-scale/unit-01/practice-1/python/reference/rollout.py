"""Rollout kit: the retirement calendar, the settings a new model refuses, a gate on a regression suite and a staged roll-out. See ../../statement.md."""
import logging
from collections import namedtuple
from datetime import date

log = logging.getLogger(__name__)

Case = namedtuple("Case", "id segment must_pass old_ok new_ok old_cost new_cost new_ms")
Request = namedtuple("Request", "model temperature top_p top_k thinking tool_choice strict prefill")
STAGES = [1, 5, 25, 100]
TARGET = "claude-sonnet-5-5"


def days_until(today, when):
    """Whole days from one ISO date to another, negative when it has passed (written for you)."""
    return (date.fromisoformat(when) - date.fromisoformat(today)).days


def percentile(values, p):
    """Nearest-rank percentile, 0 for no values (written for you)."""
    ordered = sorted(values)
    return ordered[(p * len(ordered) + 99) // 100 - 1] if ordered else 0


def _status_line(days, name, tentative):
    level = "retired" if days < 0 else "urgent" if days <= 14 else "migrate now" if days <= 60 else "watch"
    return f"{name}: {days} days, {level}{' (tentative)' if tentative else ''}"


def retirement_status(models, today):
    log.debug("retirement_status input: %r", models)
    rows = sorted((days_until(today, when), name, tentative) for name, when, tentative in models)
    return [_status_line(days, name, tentative) for days, name, tentative in rows]


def migrate_request(request, target=TARGET):
    changes = []
    if request.model != target:
        changes.append(f"model set to {target}")
    for field in ("temperature", "top_p", "top_k"):
        if getattr(request, field) is not None:
            changes.append(f"removed {field}")
    thinking = _migrate_thinking(request.thinking, changes)
    tool_choice, strict = request.tool_choice, request.strict
    if tool_choice in ("any", "tool"):
        tool_choice, strict = "auto", True
        changes.append("forced tool choice replaced by auto with strict tools")
    if request.prefill:
        changes.append("assistant prefill removed; state the format in the instructions")
    return Request(target, None, None, None, thinking, tool_choice, strict, False), changes


def _migrate_thinking(thinking, changes):
    if thinking == "budget":
        changes.append("thinking budget replaced by adaptive thinking; sweep the effort")
        return "adaptive"
    if thinking == "disabled":
        changes.append("thinking disabled replaced by between_tools")
        return "between_tools"
    return thinking


def _must_pass(cases):
    failed = sorted(c.id for c in cases if c.must_pass and not c.new_ok)
    return ["must-pass failed: " + ", ".join(failed)] if failed else []


def _protected(cases, protected):
    hit = sorted({c.segment for c in cases if c.old_ok and not c.new_ok and c.segment in protected})
    return ["protected segment lost answers: " + ", ".join(hit)] if hit else []


def _net_loss(cases):
    lost, gained = sum(1 for c in cases if c.old_ok and not c.new_ok), sum(1 for c in cases if c.new_ok and not c.old_ok)
    return [f"net loss: lost {lost}, gained {gained}"] if lost > gained else []


def _cost(cases, max_cost_up):
    old_total, new_total = sum(c.old_cost for c in cases), sum(c.new_cost for c in cases)
    up = (new_total - old_total) * 100 // old_total if old_total > 0 and new_total > old_total else 0
    return [f"cost up {up}% over the {max_cost_up}% limit"] if up > max_cost_up else []


def _latency(cases, max_p95):
    p95 = percentile([c.new_ms for c in cases], 95)
    return [f"p95 latency {p95} ms over the {max_p95} ms limit"] if p95 > max_p95 else []


def _decision(reasons):
    return "no-go" if reasons else "go"


def gate(cases, protected, max_cost_up, max_p95):
    reasons = _must_pass(cases) + _protected(cases, protected) + _net_loss(cases) + _cost(cases, max_cost_up) + _latency(cases, max_p95)
    return {"decision": _decision(reasons), "reasons": reasons}


def rollout_step(stage, requests, errors, min_requests, max_errors_per_1000):
    if requests < min_requests:
        return f"hold at {stage}"
    if errors * 1000 // requests > max_errors_per_1000:
        return "rollback to 0"
    if stage == STAGES[-1]:
        return "complete"
    return f"advance to {STAGES[STAGES.index(stage) + 1]}"
