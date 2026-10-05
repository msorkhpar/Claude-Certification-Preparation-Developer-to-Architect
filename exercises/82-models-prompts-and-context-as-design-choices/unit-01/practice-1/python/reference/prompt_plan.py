"""A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md."""
import logging
import re

log = logging.getLogger(__name__)

MIN_CACHEABLE = 512  # tokens: a shorter prefix cannot be cached


def tokens(text):
    return -(-len(text) // 4)  # one token per four characters, rounded up


def fill(text, variables):
    """Replace every {variable} in the text from the variables; a variable with no value is refused."""
    def swap(match):
        name = match.group(1)
        if name not in variables:
            raise ValueError(f"missing variable: {name}")
        return str(variables[name])
    return re.sub(r"\{(\w+)\}", swap, text)


def check_static(static):
    """A static module whose text holds a {variable} is refused: a value that changes in the prefix breaks the cache."""
    for m in static:
        if re.search(r"\{\w+\}", m["text"]):
            raise ValueError(f"static module {m['name']} holds a variable, which would break the cache")


def pick_victim(kept):
    """The index of the dynamic module to drop first: the lowest priority, and of a tie the later one."""
    return min(range(len(kept)), key=lambda i: (kept[i]["priority"], -i))


def fit_budget(prefix, kept, budget):
    """Drop dynamic modules until prefix + the kept tokens fit the budget; returns the dropped names in order."""
    dropped = []
    while prefix + sum(tokens(k["text"]) for k in kept) > budget:
        if not kept:
            raise ValueError("over budget: the static modules alone exceed it")
        dropped.append(kept.pop(pick_victim(kept))["name"])
    return dropped


def breakpoint_of(static_count, prefix):
    """The index of the last static block when there is one and the prefix has at least MIN_CACHEABLE tokens; otherwise None."""
    return static_count - 1 if static_count and prefix >= MIN_CACHEABLE else None


def assemble(modules, variables, budget):
    log.debug("assemble input: %r", modules)
    static = [m for m in modules if m["static"]]
    dynamic = [m for m in modules if not m["static"]]
    check_static(static)
    kept = [{"name": m["name"], "text": fill(m["text"], variables), "priority": m.get("priority", 0)} for m in dynamic]
    blocks = [{"name": m["name"], "text": m["text"]} for m in static]
    prefix = sum(tokens(b["text"]) for b in blocks)
    dropped = fit_budget(prefix, kept, budget)
    used = prefix + sum(tokens(k["text"]) for k in kept)
    blocks += [{"name": k["name"], "text": k["text"]} for k in kept]
    return {"blocks": blocks, "tokens": used, "dropped": dropped, "breakpoint": breakpoint_of(len(static), prefix)}


def choose_model(workload, models):
    """The name of the cheapest model that meets the tier and the latency; ties go to the lower name; None when none fits."""
    fit = [m for m in models if m["tier"] >= workload["tier"] and m["latency_ms"] <= workload["max_latency_ms"]]
    if not fit:
        return None
    return min(fit, key=lambda m: (m["price_out"], m["name"]))["name"]


def reusable_prefix(a, b):
    """The tokens of the cached prefix two assembled prompts share, or 0."""
    ia, ib = a["breakpoint"], b["breakpoint"]
    if ia is None or ib is None or ia != ib:
        return 0
    same = all(a["blocks"][i] == b["blocks"][i] for i in range(ia + 1))
    return sum(tokens(a["blocks"][i]["text"]) for i in range(ia + 1)) if same else 0
