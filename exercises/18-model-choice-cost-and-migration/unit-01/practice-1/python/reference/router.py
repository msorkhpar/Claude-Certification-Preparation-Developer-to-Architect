"""A cost model and a model router. See ../../statement.md for the contract."""


class NoModelError(Exception):
    """No model in the catalog can take the task."""


def request_cost(model, usage, batch=False):
    """Cost of one request in micro-dollars (a price of $N per million tokens is N micro-dollars per token)."""
    price = model["input"]
    created = usage.get("cache_creation")
    if created is None:
        five, hour = usage.get("cache_creation_input_tokens", 0), 0
    else:
        five, hour = created.get("ephemeral_5m_input_tokens", 0), created.get("ephemeral_1h_input_tokens", 0)
    total = (usage.get("input_tokens", 0) * price
             + five * price * 1.25
             + hour * price * 2.0
             + usage.get("cache_read_input_tokens", 0) * price * model["cache_read_multiplier"]
             + usage.get("output_tokens", 0) * model["output"])
    if batch:
        total *= 0.5
    return round(total, 6)


def _input_tokens(usage):
    created = usage.get("cache_creation")
    written = (created.get("ephemeral_5m_input_tokens", 0) + created.get("ephemeral_1h_input_tokens", 0)
               if created is not None else usage.get("cache_creation_input_tokens", 0))
    return usage.get("input_tokens", 0) + usage.get("cache_read_input_tokens", 0) + written


def route(catalog, task):
    """The id of the cheapest model that can take the task; raises NoModelError when none can."""
    usage, wanted = task["usage"], task.get("max_tokens", 0)
    eligible = [m for m in catalog
                if not m.get("deprecated", False)
                and m["tier"] >= task.get("min_tier", 1)
                and _input_tokens(usage) <= m["context"]
                and wanted <= m["max_output"]]
    if not eligible:
        raise NoModelError("no model can take this task")
    best = min(eligible, key=lambda m: (request_cost(m, usage, task.get("batch", False)), m["tier"], m["id"]))
    return best["id"]
