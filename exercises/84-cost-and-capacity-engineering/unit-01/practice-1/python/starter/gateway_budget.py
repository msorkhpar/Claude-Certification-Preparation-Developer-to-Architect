"""The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md."""
import logging

log = logging.getLogger(__name__)


def _chosen_model(request, policy):
    """TODO 1 of 6 (unlocks m1 and e6): the model the policy picks for a request.

    Receives the request (`task`, and perhaps `model`, a pin) and the policy (`allowed`, `routes`, `default`). Returns the pinned model when it is in
    `policy["allowed"]`; otherwise the model `policy["routes"]` names for the task, or `policy["default"]` for a task that is not listed.
    Example: task "review" with no pin -> "opus"; task "classify" pinned to "opus" (not allowed) -> "haiku"
    """
    return None


def route(request, policy, status):
    log.debug("route input: %r", request)
    """TODO 2 of 6 (unlocks e1): the model for the request once the budget status is known.

    Receives the request, the policy and the budget status ("allow", "warn" or "block"). Returns None when the status is "block"; the model from
    `_chosen_model` when it is "allow"; for "warn" the cheaper model that `policy["cheaper"]` names for it (the model itself when none is named).
    Example: status "warn", chosen model "opus", cheaper {"opus": "sonnet"} -> "sonnet"
    """
    return None


def admit(spend, budget, estimate):
    """TODO 3 of 6 (unlocks e2): admit one more request against the budget.

    Receives what the team has spent, its budget and the estimated cost of the request, all in cents. Returns "block" when the budget is not positive or
    `spend + estimate` is more than the budget; "warn" when it reaches 80 percent of the budget (80 percent itself warns) and is not over; else "allow".
    Example: admit(700, 1000, 100) -> "warn", admit(900, 1000, 101) -> "block"
    """
    return None


def _cost(row, prices):
    """TODO 4 of 6 (unlocks e3): what one row of tokens costs.

    Receives a row (`model`, `input`, `cache_read`, `output`: token counts) and `prices`, which maps a model to cents per million tokens for the same three
    kinds. Returns tokens times price, summed over the three kinds (not yet divided by a million). Raises ValueError(f"unknown model: {name}") for a model without a price.
    Example: input 1_000_000 at price 200 and nothing else -> 200_000_000
    """
    return 0


def _to_cents(value):
    """TODO 5 of 6 (unlocks e4): a team's total in whole cents.

    Receives a total in cents times a million. Returns it divided by one million and rounded to the nearest cent, halves up, with integer arithmetic.
    Example: _to_cents(500_000) -> 1, _to_cents(499_999) -> 0
    """
    return 0


def showback(rows, prices):
    totals = {}
    for r in rows:
        totals[r["team"]] = totals.get(r["team"], 0) + _cost(r, prices)
    result = [{"team": team, "cents": _to_cents(value)} for team, value in totals.items()]
    return sorted(result, key=lambda x: (-x["cents"], x["team"]))


def delivery(p95_seconds, timeout_seconds, margin_percent):
    """TODO 6 of 6 (unlocks e5): sync or accept-and-poll.

    Receives the p95 latency and the caller's timeout in seconds and a safety margin in percent. Returns "sync" when the p95 plus the margin fits within the
    timeout (an exact fit counts), otherwise "accept-and-poll". Use integer arithmetic: p95 * (100 + margin) against timeout * 100.
    Example: delivery(8, 10, 25) -> "sync", delivery(8, 10, 26) -> "accept-and-poll"
    """
    return None
