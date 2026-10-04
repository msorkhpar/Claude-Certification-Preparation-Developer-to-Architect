"""Rollout kit: the retirement calendar, the settings a new model refuses, a gate on a regression suite and a staged roll-out. See ../../statement.md."""
from collections import namedtuple
from datetime import date

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


def retirement_status(models, today):
    # TODO: "<name>: <days> days, <level>" per model, the nearest retirement first, with " (tentative)" when the date may move.
    return None


def migrate_request(request, target=TARGET):
    # TODO: (new request, list of changes): drop what the target refuses, replace what it changes, and name each change.
    return None


def gate(cases, protected, max_cost_up, max_p95):
    # TODO: {"decision": "go" or "no-go", "reasons": [...]}: must-pass, protected segments, net loss, cost and tail, in that order.
    return None


def rollout_step(stage, requests, errors, min_requests, max_errors_per_1000):
    # TODO: "hold at S", "rollback to 0", "complete" or "advance to N" for one observed stage.
    return None
