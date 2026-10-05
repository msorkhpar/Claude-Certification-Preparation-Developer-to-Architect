# Case lists of module 54-distributing-tools-across-agents: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/54-distributing-tools-across-agents/unit-01/practice-1"] = {
    "name": "distribute", "suite": "DistributeTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "each role gets only the tools of its specialisation in catalog order"),
        ("e1", "edge", "a role over its budget or given an unknown or unscoped outside tool or a duplicate catalog name is refused"),
        ("e2", "edge", "an irreversible tool is given only by an explicit grant"),
        ("e3", "edge", "a model that accepts forcing gets the native choice and the others get auto with strict tools and one named tool"),
        ("e4", "edge", "a change of tool choice costs the cached messages a change of tools costs everything and a repeat costs nothing"),
        ("e5", "edge", "a reply is checked against the call that was required"),
        ("e6", "edge", "an unknown tool a wrong owner or a bad amount is refused"),
        ("e7", "edge", "the cap is inclusive and an irreversible call needs approval that never lifts the cap"),
    ],
    "plants": {
        "wrong-sorted-names": (["e1", "e2", "m1"], "lists a role's tools alphabetically instead of in catalog order"),
        "wrong-no-budget": (["e1"], "lets a role have more tools than the budget"),
        "wrong-unscoped-extra": (["e1"], "grants an outside tool that is not marked as a scoped cross-role tool"),
        "wrong-duplicates-ok": (["e1"], "accepts two catalog entries with the same name"),
        "wrong-irreversible-by-tag": (["e2", "m1"], "gives an irreversible tool to every role whose specialisation matches"),
        "wrong-forces-everywhere": (["e3", "e4"], "forces the tool choice on models that reject it"),
        "wrong-fallback-all-tools": (["e3", "e4"], "offers every tool in the fallback for a named first tool"),
        "wrong-fallback-no-verify": (["e3"], "does not ask for the reply to be checked in the fallback"),
        "wrong-tools-ignored": (["e4"], "does not count a narrower tool list as a change that costs the whole cache"),
        "wrong-choice-ignored": (["e4"], "does not count a changed tool choice as a cost to the cached messages"),
        "wrong-repeat-costs": (["e4"], "reports a cost for a request that repeats the previous one"),
        "wrong-missed-call-ok": (["e5"], "accepts a reply with no tool call when one was required"),
        "wrong-any-tool-counts": (["e5"], "accepts any tool when a particular one was required first"),
        "wrong-default-allow": (["e6"], "allows a tool that the policy does not name"),
        "wrong-owner-unchecked": (["e6", "e7"], "does not compare the customer with the verified one"),
        "wrong-cap-exclusive": (["e7"], "refuses an amount equal to the cap"),
        "wrong-approval-lifts-cap": (["e7"], "lets an approval allow an amount above the cap"),
        "wrong-approval-skipped": (["e7"], "allows an irreversible call without an approval"),
        "wrong-irreversible-extra-dropped": (["e2"], "silently leaves out an irreversible tool that was granted explicitly outside the role's tags"),
    },
}
