"""Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract."""
SECTIONS = {"tools": 0, "system": 1, "messages": 2}
MAX_BREAKPOINTS = 5


class PlanError(Exception):
    """The request cannot be cached as asked (the API would answer 400, or the plan can never hit)."""


def plan_request(blocks, min_tokens=1024):
    for block in blocks:
        if block.get("volatile", False) and block["section"] == "tools":
            raise PlanError(f"tool definition {block['id']} cannot be volatile: tools come first")
    ordered = sorted(blocks, key=lambda b: SECTIONS[b["section"]])  # stable inside a section
    ordered = [b for b in ordered if not b.get("volatile", False)] + [b for b in ordered if b.get("volatile", False)]
    plan, total = [], 0
    for block in ordered:
        volatile = block.get("volatile", False)
        if not volatile:
            total += block["tokens"]
        wanted = block.get("breakpoint", False) and not volatile and total >= min_tokens
        plan.append({"id": block["id"], "cache": block.get("ttl", "5m") if wanted else None})
    marked = [p for p in plan if p["cache"]]
    if len(marked) > MAX_BREAKPOINTS:
        raise PlanError(f"{len(marked)} breakpoints: at most {MAX_BREAKPOINTS}")
    seen_five = False
    for p in marked:
        if p["cache"] == "5m":
            seen_five = True
        elif seen_five:
            raise PlanError("a 1h breakpoint must come before every 5m breakpoint")
    return plan
