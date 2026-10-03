"""Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract."""


class PlanError(Exception):
    """The request cannot be cached as asked."""


def plan_request(blocks, min_tokens=1024):
    # TODO: return the blocks in cache-friendly order, each as {"id": ..., "cache": None | "5m" | "1h"}.
    return None
