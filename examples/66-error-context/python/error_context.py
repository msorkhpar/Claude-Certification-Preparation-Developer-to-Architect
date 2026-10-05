"""What a coordinator is told when one of five sources fails, under four ways of reporting it.

The exam guide (task 5.3) calls structured error context (failure type, the query attempted, partial results, alternatives) what lets a coordinator recover intelligently. It names two anti-patterns: a generic status
such as "search unavailable", which hides the context, and silent suppression, which reports an empty result as a success; terminating the whole workflow on one failure is the third. The five sources below and
their outcomes are invented for the illustration; nothing here calls a model or a search tool.
"""
import logging

log = logging.getLogger(__name__)

# source -> (kind, items); kind is ok, timeout or permission
OUTCOMES = {"news": ("ok", ["n1", "n2"]), "papers": ("timeout", ["p1"]), "patents": ("ok", []), "filings": ("permission", []), "blogs": ("ok", ["b1"])}
TRY = {"timeout": "retry later", "permission": "request access"}


def found(outcomes):
    return [item for kind, items in outcomes.values() if kind == "ok" for item in items]


def generic(outcomes):
    down = [source for source, (kind, _) in outcomes.items() if kind != "ok"]
    return f"found {', '.join(found(outcomes))}; sources unavailable: {', '.join(down)}"


def suppress(outcomes):
    nothing = [source for source, (kind, items) in outcomes.items() if kind != "ok" or not items]
    return f"found {', '.join(found(outcomes))}; nothing found in: {', '.join(nothing)}"


def terminate(outcomes):
    kept = []
    for source, (kind, items) in outcomes.items():
        if kind != "ok":
            return f"aborted at {source}; found {', '.join(kept)}"
        kept += items
    return f"found {', '.join(kept)}"


def structured(outcomes):
    good = [s for s, (kind, items) in outcomes.items() if kind == "ok" and items]
    partial = [f"{s} ({kind}, kept {', '.join(items)})" for s, (kind, items) in outcomes.items() if kind != "ok" and items]
    empty = [s for s, (kind, items) in outcomes.items() if kind == "ok" and not items]
    gaps = [f"{s} ({kind}, try: {TRY[kind]})" for s, (kind, items) in outcomes.items() if kind != "ok" and not items]
    parts = [("well supported", good), ("partial", partial), ("no findings", empty), ("gaps", gaps)]
    return "; ".join(f"{name}: {', '.join(items)}" for name, items in parts if items)


def main():
    for name, report in (("generic status", generic), ("silent empty", suppress), ("abort on failure", terminate), ("structured context", structured)):
        print(f"{name}: {report(OUTCOMES)}")


if __name__ == "__main__":
    main()
