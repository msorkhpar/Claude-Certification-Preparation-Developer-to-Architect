"""A cost model and a model router. See ../../statement.md for the contract."""


class NoModelError(Exception):
    """No model in the catalog can take the task."""


def request_cost(model, usage, batch=False):
    # TODO: the cost of one request in micro-dollars, rounded to 6 decimals.
    return None


def route(catalog, task):
    # TODO: the id of the cheapest model that can take the task; raise NoModelError when none can.
    return None
