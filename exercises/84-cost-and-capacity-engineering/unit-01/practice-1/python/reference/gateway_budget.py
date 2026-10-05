"""The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md."""
import logging

log = logging.getLogger(__name__)


def _chosen_model(request, policy):
    wanted = request.get("model")
    return wanted if wanted in policy["allowed"] else policy["routes"].get(request["task"], policy["default"])


def route(request, policy, status):
    log.debug("route input: %r", request)
    if status == "block":
        return None
    model = _chosen_model(request, policy)
    return policy["cheaper"].get(model, model) if status == "warn" else model


def admit(spend, budget, estimate):
    if budget <= 0:
        return "block"
    after = spend + estimate
    if after > budget:
        return "block"
    if after * 100 >= budget * 80:
        return "warn"
    return "allow"


def _cost(row, prices):
    if row["model"] not in prices:
        raise ValueError(f"unknown model: {row['model']}")
    p = prices[row["model"]]
    return row["input"] * p["input"] + row["cache_read"] * p["cache_read"] + row["output"] * p["output"]


def _to_cents(value):
    return (value + 500_000) // 1_000_000


def showback(rows, prices):
    totals = {}
    for r in rows:
        totals[r["team"]] = totals.get(r["team"], 0) + _cost(r, prices)
    result = [{"team": team, "cents": _to_cents(value)} for team, value in totals.items()]
    return sorted(result, key=lambda x: (-x["cents"], x["team"]))


def delivery(p95_seconds, timeout_seconds, margin_percent):
    return "sync" if p95_seconds * (100 + margin_percent) <= timeout_seconds * 100 else "accept-and-poll"
