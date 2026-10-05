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
    """TODO 1 of 6 (finish this to pass e1): the cost of the cache parts of a request: the writes and the reads.

    Receives the model (`input` price, `cache_read_multiplier`) and the usage. Returns the 5-minute write tokens times 1.25 times the input price, plus the
    1-hour write tokens times 2 times the input price, plus the cache read tokens times the input price times the model's cache_read_multiplier. `_writes(usage)` gives the two write counts.
    Example: input price 4, cache_read_input_tokens 1000, multiplier 0.05 -> 200.0
    """
    return 0.0


def _apply_batch(total, batch):
    """TODO 2 of 6 (finish this to pass e2): the cost after the batch discount.

    Receives the total cost and whether the request is a batch. Returns half of the total when `batch` is true, the total unchanged otherwise.
    Example: _apply_batch(1000, True) -> 500.0, _apply_batch(1000, False) -> 1000
    """
    return total


def request_cost(model, usage, batch=False):
    """Cost of one request in micro-dollars (a price of $N per million tokens is N micro-dollars per token)."""
    total = usage.get("input_tokens", 0) * model["input"] + _cache_cost(model, usage) + usage.get("output_tokens", 0) * model["output"]
    return round(_apply_batch(total, batch), 6)


def _input_tokens(usage):
    """TODO 3 of 6 (finish this to pass e4): the tokens the model has to hold as input.

    Receives the usage. Returns uncached input tokens plus cache read tokens plus every cache write (both kinds; `_writes(usage)` gives them). A missing field counts as 0.
    Example: {"input_tokens": 250000, "cache_read_input_tokens": 50000} -> 300000
    """
    return 0


def _can_take(model, task, input_tokens):
    """TODO 4 of 6 (finish this to pass m1, e4 and e5): whether the model is allowed and big enough for the task.

    Receives the model, the task (`min_tier` default 1, `max_tokens` default 0) and the task's total input tokens. Returns True when the model is not deprecated,
    its tier is at least min_tier, its context holds the input tokens and its max_output holds max_tokens.
    Example: a model with context 200000 and input_tokens 250000 -> False
    """
    return False


def _cheapest(models, usage, batch):
    """TODO 5 of 6 (finish this to pass m1, e3 and e6): the id of the cheapest of `models` for this usage.

    Receives a non-empty list of models, the usage and the batch flag. Prices each with request_cost(model, usage, batch) and returns the id of the lowest; on equal
    cost the lower tier wins, then the smaller id. The order of the list never matters.
    Example: Sonnet and Opus costing 200000 each -> the Sonnet id (tier 2 before tier 3)
    """
    return ""


def _require_choice(models):
    """TODO 6 of 6 (finish this to pass e5): an empty list of models is an error.

    Receives the models that can take the task. Raises NoModelError("no model can take this task") when the list is empty; returns nothing otherwise.
    Example: _require_choice([]) raises NoModelError
    """


def route(catalog, task):
    """The id of the cheapest model that can take the task; raises NoModelError when none can."""
    log.debug("route input: %r", task)
    usage = task["usage"]
    eligible = [m for m in catalog if _can_take(m, task, _input_tokens(usage))]
    _require_choice(eligible)
    return _cheapest(eligible, usage, task.get("batch", False))
