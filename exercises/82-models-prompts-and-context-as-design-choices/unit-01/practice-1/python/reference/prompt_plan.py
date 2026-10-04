"""A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md."""
import re

MIN_CACHEABLE = 512  # tokens: a shorter prefix cannot be cached


def tokens(text):
    return -(-len(text) // 4)  # one token per four characters, rounded up


def _fill(text, variables):
    def swap(match):
        name = match.group(1)
        if name not in variables:
            raise ValueError(f"missing variable: {name}")
        return str(variables[name])
    return re.sub(r"\{(\w+)\}", swap, text)


def assemble(modules, variables, budget):
    static = [m for m in modules if m["static"]]
    dynamic = [m for m in modules if not m["static"]]
    for m in static:
        if re.search(r"\{\w+\}", m["text"]):
            raise ValueError(f"static module {m['name']} holds a variable, which would break the cache")
    kept = [{"name": m["name"], "text": _fill(m["text"], variables), "priority": m.get("priority", 0)} for m in dynamic]
    blocks = [{"name": m["name"], "text": m["text"]} for m in static]
    prefix = sum(tokens(b["text"]) for b in blocks)
    dropped = []
    while prefix + sum(tokens(k["text"]) for k in kept) > budget:
        if not kept:
            raise ValueError("over budget: the static modules alone exceed it")
        victim = min(range(len(kept)), key=lambda i: (kept[i]["priority"], -i))
        dropped.append(kept.pop(victim)["name"])
    used = prefix + sum(tokens(k["text"]) for k in kept)
    blocks += [{"name": k["name"], "text": k["text"]} for k in kept]
    return {"blocks": blocks, "tokens": used, "dropped": dropped, "breakpoint": len(static) - 1 if static and prefix >= MIN_CACHEABLE else None}


def choose_model(workload, models):
    fit = [m for m in models if m["tier"] >= workload["tier"] and m["latency_ms"] <= workload["max_latency_ms"]]
    if not fit:
        return None
    return min(fit, key=lambda m: (m["price_out"], m["name"]))["name"]


def reusable_prefix(a, b):
    ia, ib = a["breakpoint"], b["breakpoint"]
    if ia is None or ib is None or ia != ib:
        return 0
    same = all(a["blocks"][i] == b["blocks"][i] for i in range(ia + 1))
    return sum(tokens(a["blocks"][i]["text"]) for i in range(ia + 1)) if same else 0
