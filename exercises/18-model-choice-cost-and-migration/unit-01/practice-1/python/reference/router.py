"""A cost model and a model router. See ../../statement.md for the contract."""
import logging

log = logging.getLogger(__name__)


class NoModelError(Exception):
    """No model in the catalog can take the task."""


def _writes(usage):
    """The cache-write tokens as (5-minute kind, 1-hour kind); an unsplit count is all the 5-minute kind."""
    created = usage.get("cache_creation")
    if created is None:
        return usage.get("cache_creation_input_tokens", 0), 0
    return created.get("ephemeral_5m_input_tokens", 0), created.get("ephemeral_1h_input_tokens", 0)


def _cache_cost(model, usage):
    """The cost of the cache parts of a request: the writes and the reads."""
    five, hour = _writes(usage)
    price = model["input"]
    return five * price * 1.25 + hour * price * 2.0 + usage.get("cache_read_input_tokens", 0) * price * model["cache_read_multiplier"]


def _apply_batch(total, batch):
    """The cost after the batch discount: half of it when `batch` is true."""
    return total * 0.5 if batch else total


def request_cost(model, usage, batch=False):
    """Cost of one request in micro-dollars (a price of $N per million tokens is N micro-dollars per token)."""
    total = usage.get("input_tokens", 0) * model["input"] + _cache_cost(model, usage) + usage.get("output_tokens", 0) * model["output"]
    return round(_apply_batch(total, batch), 6)


def _input_tokens(usage):
    """The tokens the model has to hold as input: uncached input, cache reads and every cache write."""
    five, hour = _writes(usage)
    return usage.get("input_tokens", 0) + usage.get("cache_read_input_tokens", 0) + five + hour


def _can_take(model, task, input_tokens):
    """Whether the model is allowed and big enough for the task."""
    return (not model.get("deprecated", False)
            and model["tier"] >= task.get("min_tier", 1)
            and input_tokens <= model["context"]
            and task.get("max_tokens", 0) <= model["max_output"])


def _cheapest(models, usage, batch):
    """The id of the cheapest of `models` for this usage; on a tie the lower tier, then the smaller id."""
    best = min(models, key=lambda m: (request_cost(m, usage, batch), m["tier"], m["id"]))
    return best["id"]


def _require_choice(models):
    """An empty list of models is an error: no model can take the task."""
    if not models:
        raise NoModelError("no model can take this task")


def route(catalog, task):
    """The id of the cheapest model that can take the task; raises NoModelError when none can."""
    log.debug("route input: %r", task)
    usage = task["usage"]
    eligible = [m for m in catalog if _can_take(m, task, _input_tokens(usage))]
    _require_choice(eligible)
    return _cheapest(eligible, usage, task.get("batch", False))
