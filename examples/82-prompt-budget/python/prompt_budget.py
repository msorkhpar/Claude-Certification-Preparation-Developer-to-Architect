"""A prompt assembled from modules in cache-friendly order, with a token budget and the cache breakpoint.

The Claude documentation on prompt caching (read on 2026-10-04) says cache prefixes are created "in the following order: tools, system,
then messages" and that a prompt shorter than the model's minimum "cannot be cached, even if marked with cache_control" (512 tokens for
Claude Sonnet 5.5). The prompting guide says to put long documents "near the top of your prompt, above your query". This file orders
the modules of a request that way, estimates tokens as one per four characters (a rough rule, not the model's tokenizer), marks the
breakpoint after the last static module and shows which edits keep the cached prefix and which break it. No model is called.
"""
MIN_CACHEABLE = 512  # tokens, Claude Sonnet 5.5

POLICY = "Refunds above 200 are approved by a supervisor. Gift cards are never refunded in cash. " * 26
MODULES = [
    {"name": "role", "static": True, "text": "You are the support assistant of Northwind Outfitters. Answer from the policy only."},
    {"name": "policy", "static": True, "text": POLICY},
    {"name": "customer", "static": False, "text": "Customer: {customer}. Tier: {tier}."},
    {"name": "question", "static": False, "text": "Question: {question}"},
]


def tokens(text):
    return -(-len(text) // 4)  # ceiling of characters over four


def assemble(modules, variables):
    """Static modules first, in the order given, then the dynamic ones with their variables filled in."""
    ordered = [m for m in modules if m["static"]] + [m for m in modules if not m["static"]]
    blocks = [{"name": m["name"], "static": m["static"], "text": m["text"].format(**variables) if not m["static"] else m["text"]} for m in ordered]
    prefix = sum(tokens(b["text"]) for b in blocks if b["static"])
    last_static = max((i for i, b in enumerate(blocks) if b["static"]), default=None)
    return {"blocks": blocks, "tokens": sum(tokens(b["text"]) for b in blocks), "prefix_tokens": prefix,
            "breakpoint": last_static if prefix >= MIN_CACHEABLE else None}


def cached_prefix(prompt):
    return "".join(b["text"] for b in prompt["blocks"][: prompt["breakpoint"] + 1]) if prompt["breakpoint"] is not None else ""


def main():
    first = assemble(MODULES, {"customer": "Ana", "tier": "gold", "question": "Can I return a gift card?"})
    print("order:", " > ".join(b["name"] for b in first["blocks"]))
    print(f"tokens: {first['tokens']} in all, {first['prefix_tokens']} in the static prefix, minimum {MIN_CACHEABLE}")
    print("breakpoint after:", first["blocks"][first["breakpoint"]]["name"])
    second = assemble(MODULES, {"customer": "Ben", "tier": "basic", "question": "Where is my parcel?"})
    print("next request, other customer: prefix identical:", cached_prefix(second) == cached_prefix(first))
    edited = [dict(m, text=m["text"].replace("200", "300")) if m["name"] == "policy" else m for m in MODULES]
    third = assemble(edited, {"customer": "Ana", "tier": "gold", "question": "Can I return a gift card?"})
    print("after a policy edit: prefix identical:", cached_prefix(third) == cached_prefix(first))
    short = assemble(MODULES[:1] + MODULES[2:], {"customer": "Ana", "tier": "gold", "question": "Hi"})
    print("without the policy: breakpoint", short["breakpoint"], "because", short["prefix_tokens"], "tokens is under", MIN_CACHEABLE)


if __name__ == "__main__":
    main()
