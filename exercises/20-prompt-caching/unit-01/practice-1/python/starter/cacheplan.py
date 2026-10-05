"""Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract."""
import logging

log = logging.getLogger(__name__)

SECTIONS = {"tools": 0, "system": 1, "messages": 2}
MAX_BREAKPOINTS = 4


class PlanError(Exception):
    """The request cannot be cached as asked (the API would answer 400, or the plan can never hit)."""


def _check_tools(blocks):
    """TODO 1 of 6 (finish this to pass e5): refuse a volatile tool definition.

    Receives the list of blocks. Raises PlanError when a block in the `tools` section is volatile (tools come first, nothing volatile may
    sit in the prefix); returns nothing otherwise. Example: _check_tools([{"id": "t", "section": "tools", "tokens": 9, "volatile": True}])
    raises PlanError
    """


def _ordered(blocks):
    """TODO 2 of 6 (finish this to pass m1, e1 and e6): the blocks in cache-friendly order, as a new list.

    Receives the blocks. Returns a new list: sections in the order of SECTIONS (tools, system, messages), blocks of one section in the
    order given, then every volatile block moved to the very end in its given order. The input list is left unchanged.
    Example: ids of [m(messages), s(system, volatile), t(tools)] come out as t, m, s
    """
    return []


def _wants_breakpoint(block, total, min_tokens):
    """TODO 3 of 6 (finish this to pass e2 and e5): does this block carry a breakpoint?

    Receives the block, `total` (the stable tokens from the start up to and including this block) and `min_tokens`. Returns True when the
    block asks for a breakpoint, is not volatile and total is at least min_tokens; False otherwise.
    Example: _wants_breakpoint({"breakpoint": True}, 500, 1024) -> False
    """
    return False


def _marker(block):
    """TODO 4 of 6 (finish this to pass m1): the cache lifetime a breakpoint carries.

    Receives the block. Returns its `ttl` ("5m" or "1h"), "5m" when it has none. Example: _marker({"ttl": "1h"}) -> "1h", _marker({}) -> "5m"
    """
    return None


def _check_count(marked):
    """TODO 5 of 6 (finish this to pass e3): at most MAX_BREAKPOINTS breakpoints.

    Receives the list of lifetimes of the blocks that kept a breakpoint. Raises PlanError when there are more than MAX_BREAKPOINTS.
    Example: _check_count(["5m"] * 5) raises PlanError
    """


def _check_lifetimes(marked):
    """TODO 6 of 6 (finish this to pass e4): a 1h breakpoint must come before every 5m one.

    Receives the lifetimes of the kept breakpoints in request order. Raises PlanError when a "1h" follows a "5m".
    Example: _check_lifetimes(["5m", "1h"]) raises PlanError, _check_lifetimes(["1h", "5m"]) does not
    """


def plan_request(blocks, min_tokens=1024):
    log.debug("plan_request input: %r", blocks)
    _check_tools(blocks)
    plan, total = [], 0
    for block in _ordered(blocks):
        if not block.get("volatile", False):
            total += block["tokens"]
        wanted = _wants_breakpoint(block, total, min_tokens)
        plan.append({"id": block["id"], "cache": _marker(block) if wanted else None})
    marked = [p["cache"] for p in plan if p["cache"]]
    _check_count(marked)
    _check_lifetimes(marked)
    return plan
