"""Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract."""
import logging

log = logging.getLogger(__name__)

SECTIONS = {"tools": 0, "system": 1, "messages": 2}
MAX_BREAKPOINTS = 4


class PlanError(Exception):
    """The request cannot be cached as asked (the API would answer 400, or the plan can never hit)."""


def _check_tools(blocks):
    for block in blocks:
        if block.get("volatile", False) and block["section"] == "tools":
            raise PlanError(f"tool definition {block['id']} cannot be volatile: tools come first")


def _ordered(blocks):
    by_section = sorted(blocks, key=lambda b: SECTIONS[b["section"]])  # stable inside a section
    return [b for b in by_section if not b.get("volatile", False)] + [b for b in by_section if b.get("volatile", False)]


def _wants_breakpoint(block, total, min_tokens):
    return block.get("breakpoint", False) and not block.get("volatile", False) and total >= min_tokens


def _marker(block):
    return block.get("ttl", "5m")


def _check_count(marked):
    if len(marked) > MAX_BREAKPOINTS:
        raise PlanError(f"{len(marked)} breakpoints: at most {MAX_BREAKPOINTS}")


def _check_lifetimes(marked):
    seen_five = False
    for cache in marked:
        if cache == "5m":
            seen_five = True
        elif seen_five:
            raise PlanError("a 1h breakpoint must come before every 5m breakpoint")


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
