"""A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md."""
import re

MIN_CACHEABLE = 512  # tokens: a shorter prefix cannot be cached


def tokens(text):
    return -(-len(text) // 4)  # one token per four characters, rounded up


def assemble(modules, variables, budget):
    # TODO: static modules first, then the dynamic ones with their variables filled; drop dynamic modules to fit the budget; mark the breakpoint.
    return None


def choose_model(workload, models):
    # TODO: the name of the cheapest model that meets the tier and the latency, or None.
    return None


def reusable_prefix(a, b):
    # TODO: the tokens of the cached prefix that two assembled prompts share, or 0.
    return None
