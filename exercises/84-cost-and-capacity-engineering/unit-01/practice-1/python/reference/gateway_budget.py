"""The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md."""


def route(request, policy, status):
    if status == "block":
        return None
    wanted = request.get("model")
    model = wanted if wanted in policy["allowed"] else policy["routes"].get(request["task"], policy["default"])
    if status == "warn":
        model = policy["cheaper"].get(model, model)
    return model


def admit(spend, budget, estimate):
    if budget <= 0:
        return "block"
    after = spend + estimate
    if after > budget:
        return "block"
    if after * 100 >= budget * 80:
        return "warn"
    return "allow"


def showback(rows, prices):
    totals = {}
    for r in rows:
        if r["model"] not in prices:
            raise ValueError(f"unknown model: {r['model']}")
        p = prices[r["model"]]
        totals[r["team"]] = totals.get(r["team"], 0) + r["input"] * p["input"] + r["cache_read"] * p["cache_read"] + r["output"] * p["output"]
    result = [{"team": team, "cents": (value + 500_000) // 1_000_000} for team, value in totals.items()]
    return sorted(result, key=lambda x: (-x["cents"], x["team"]))


def delivery(p95_seconds, timeout_seconds, margin_percent):
    return "sync" if p95_seconds * (100 + margin_percent) <= timeout_seconds * 100 else "accept-and-poll"
