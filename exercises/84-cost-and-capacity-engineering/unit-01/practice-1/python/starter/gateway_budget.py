"""The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md."""


def route(request, policy, status):
    # TODO: the model for the request, or None when the team is blocked.
    return None


def admit(spend, budget, estimate):
    # TODO: "allow", "warn" or "block" for one more request against the budget.
    return None


def showback(rows, prices):
    # TODO: [{"team", "cents"}] for each team, most expensive first, ties by team name.
    return None


def delivery(p95_seconds, timeout_seconds, margin_percent):
    # TODO: "sync" or "accept-and-poll".
    return None
