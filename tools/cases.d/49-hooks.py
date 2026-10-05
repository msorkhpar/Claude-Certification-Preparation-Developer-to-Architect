# Case lists of module 49-hooks: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/49-hooks/unit-01/practice-1"] = {
    "name": "hooks", "suite": "HooksTest", "langs": ["python", "typescript"],
    "cases": [
        ("m1", "main", "the gate allows a small refund asks about a middle one and denies a large one"),
        ("e1", "edge", "each limit belongs to the lower tier and the next cent goes up"),
        ("e2", "edge", "a missing or invalid amount is denied and other tools are left alone"),
        ("e3", "edge", "the output gets a date a status word and a decimal amount"),
        ("e4", "edge", "output that is not json or is already readable is left alone"),
        ("e5", "edge", "the hooks are registered on tool name matchers with a timeout"),
        ("e6", "edge", "a command hook blocks with exit two and a reason and fails closed on bad input"),
        ("e7", "edge", "the settings block runs the command hook on bash with a timeout"),
        ("e8", "edge", "a run through the sdk denies the large refund and shows the model a readable order"),
    ],
    "plants": {
        "wrong-200-asks": (["e1"], "asks a person about a refund of exactly the automatic limit"),
        "wrong-500-denied": (["e1"], "denies a refund of exactly the upper limit"),
        "wrong-missing-amount-allowed": (["e2"], "lets a refund with no amount through"),
        "wrong-coerces-amount": (["e2"], "turns a text or a boolean amount into a number"),
        "wrong-gates-every-tool": (["e2"], "applies the refund limits to a tool that is not a refund"),
        "wrong-always-milliseconds": (["e3"], "reads every epoch as milliseconds"),
        "wrong-cents-kept": (["e3"], "leaves amount_cents beside the new amount"),
        "wrong-rewrites-plain-text": (["e4"], "answers a replacement for output that is not json"),
        "wrong-not-idempotent": (["e4"], "reports a change for output that is already readable"),
        "wrong-no-matcher": (["e5"], "registers the refund gate for every tool"),
        "wrong-no-timeout": (["e5"], "registers the hooks without a timeout"),
        "wrong-exit-one": (["e6"], "blocks with exit code 1, which does not block"),
        "wrong-bad-input-passes": (["e6"], "lets a call through when the hook input is not json"),
        "wrong-settings-no-timeout": (["e7"], "writes the settings hook without a timeout"),
        "wrong-turn-limit-too-low": (["e8"], "limits the run to two turns, so the second refund is never reached"),
    },
}
