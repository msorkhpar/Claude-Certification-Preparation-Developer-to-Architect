"""A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md."""
import logging
import re

log = logging.getLogger(__name__)

MIN_CACHEABLE = 512  # tokens: a shorter prefix cannot be cached


def tokens(text):
    return -(-len(text) // 4)  # one token per four characters, rounded up


def fill(text, variables):
    """TODO 1 of 7 (unlocks e2): fill the variables of a dynamic module's text.

    Receives the text and a dict of variables. Returns the text with every `{name}` replaced by that variable's value (extra variables are ignored).
    Raises `ValueError("missing variable: <name>")` when a variable has no value.
    Example: fill("Q: {q}", {"q": "hello"}) -> "Q: hello"
    """
    return text


def check_static(static):
    """TODO 2 of 7 (unlocks e1): refuse a variable in a static module.

    Receives the list of static modules. Raises `ValueError` with a message that says `static` (for example `static module <name> holds a variable, which
    would break the cache`) when any module's text holds a `{variable}`; otherwise returns nothing.
    Example: a static module with the text "policy for {customer}" -> ValueError
    """
    return None


def pick_victim(kept):
    """TODO 3 of 7 (unlocks e3): which dynamic module is dropped first?

    Receives the list of kept dynamic modules, each `{name, text, priority}`. Returns the index of the one with the lowest priority; of two with the same
    priority, the later one (the higher index).
    Example: priorities [1, 9, 1] -> 2
    """
    return 0


def fit_budget(prefix, kept, budget):
    """TODO 4 of 7 (unlocks e3 and e4): drop dynamic modules until the prompt fits the budget.

    Receives the tokens of the static prefix, the list `kept` of dynamic modules (change it in place) and the budget. While `prefix` plus the tokens of the
    kept modules exceeds the budget, remove the module `pick_victim` chooses and note its name. Returns the dropped names in the order they were dropped.
    When nothing is left to drop and the budget is still exceeded, raises `ValueError("over budget: ...")`: static modules are never dropped.
    Example: kept priorities [1, 1] and a budget that fits one of them -> the later name is dropped
    """
    return []


def breakpoint_of(static_count, prefix):
    """TODO 5 of 7 (unlocks m1 and e5): where the cache breakpoint goes.

    Receives the number of static blocks and the tokens of the static prefix. Returns the index of the last static block when there is at least one static
    block and the prefix has at least `MIN_CACHEABLE` tokens; otherwise `None`.
    Example: breakpoint_of(2, 512) -> 1, breakpoint_of(2, 511) -> None, breakpoint_of(0, 900) -> None
    """
    return None


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
    """TODO 6 of 7 (unlocks e6): choose the model for a workload.

    Receives the workload `{tier, max_latency_ms}` and a list of models `{name, tier, latency_ms, price_out}`. Returns the name of the model with the lowest
    `price_out` among those whose `tier` is at least the workload's and whose `latency_ms` is within the limit; equal prices go to the lower name; `None` when no
    model fits. Example: two models at the same price named "mid" and "mid2", both fitting -> "mid"
    """
    return None


def reusable_prefix(a, b):
    """TODO 7 of 7 (unlocks e7): how many tokens of cached prefix can be reused?

    Receives two assembled prompts (each with `blocks` and `breakpoint`). When both have a breakpoint, the breakpoints are equal and every block up to and
    including it is identical (name and text) in both, returns the tokens of those blocks; otherwise returns 0.
    Example: two prompts that differ only in their dynamic blocks -> the tokens of the static prefix; one edited static block -> 0
    """
    return 0
