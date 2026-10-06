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
    """TODO 1 of 9 (unlocks e7): one line of the retirement calendar.

    Receives the days left, the model name and whether the date is tentative. Returns `<name>: <days> days, <level>` with ` (tentative)`
    added when it is tentative; the level is `retired` below 0, `urgent` up to 14, `migrate now` up to 60, else `watch`.
    Example: _status_line(14, "a", True) -> "a: 14 days, urgent (tentative)"
    """
    return ""


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
    """TODO 2 of 9 (unlocks e8): migrate the thinking setting.

    Receives the thinking setting and the list of changes so far. `budget` becomes `adaptive` and `disabled` becomes `between_tools`, each
    appending its sentence from the statement to `changes`; anything else is kept. Returns the new setting.
    Example: _migrate_thinking("disabled", changes) -> "between_tools", and changes gains "thinking disabled replaced by between_tools"
    """
    return thinking


def _must_pass(cases):
    """TODO 3 of 9 (unlocks e1): the reasons for failed must-pass cases.

    Receives the cases. Returns a list with `must-pass failed: <ids>` (sorted, joined by `, `) for the cases marked must pass that the new
    model fails, or an empty list. Example: one failing must-pass case a1 -> ["must-pass failed: a1"]
    """
    return []


def _protected(cases, protected):
    """TODO 4 of 9 (unlocks e2): the reason for a protected segment that lost answers.

    Receives the cases and the set of protected segments. Returns a list with `protected segment lost answers: <segments>` (sorted,
    distinct) for the protected segments with a case the old model got right and the new one did not, or an empty list.
    Example: refund lost b3 -> ["protected segment lost answers: refund"]
    """
    return []


def _net_loss(cases):
    """TODO 5 of 9 (unlocks e3): the reason for more losses than gains.

    Receives the cases. Returns a list with `net loss: lost N, gained M` when the cases lost (old right, new wrong) outnumber the cases
    gained (the opposite), or an empty list. Example: 2 lost and 1 gained -> ["net loss: lost 2, gained 1"]
    """
    return []


def _cost(cases, max_cost_up):
    """TODO 6 of 9 (unlocks e4): the reason for a cost rise over the limit.

    Receives the cases and the largest allowed rise in whole percent. The rise is the new total cost over the old one, rounded down, 0 when
    the old total is 0 or the cost fell. Returns a list with `cost up X% over the Y% limit` when X is above Y, or an empty list.
    Example: costs 40 -> 50 with a limit of 20 -> ["cost up 25% over the 20% limit"]; costs 40 -> 48 with a limit of 20 -> []
    """
    return []


def _latency(cases, max_p95):
    """TODO 7 of 9 (unlocks e5): the reason for a slow tail.

    Receives the cases and the largest allowed 95th-percentile time in ms. Uses `percentile` over the `new_ms` of the cases. Returns a list
    with `p95 latency X ms over the Y ms limit` when X is above Y, or an empty list. Example: p95 3000 with a limit of 2000 -> one reason
    """
    return []


def _decision(reasons):
    """TODO 8 of 9 (unlocks m1): the decision of the gate.

    Receives the list of reasons. Returns `no-go` when there is any reason, `go` when there is none. Example: _decision([]) -> "go"
    """
    return ""


def gate(cases, protected, max_cost_up, max_p95):
    reasons = _must_pass(cases) + _protected(cases, protected) + _net_loss(cases) + _cost(cases, max_cost_up) + _latency(cases, max_p95)
    return {"decision": _decision(reasons), "reasons": reasons}


def rollout_step(stage, requests, errors, min_requests, max_errors_per_1000):
    """TODO 9 of 9 (unlocks e6): the step of a staged roll-out.

    Receives the stage (1, 5, 25 or 100), the requests and errors seen, the fewest requests to judge and the most errors per 1000.
    Returns `hold at <stage>` with too few requests, `rollback to 0` when errors per 1000 (rounded down) pass the limit, `complete` at
    stage 100 when healthy, else `advance to <next stage>`. Example: rollout_step(1, 2000, 6, 1000, 5) -> "advance to 5"
    """
    return ""
