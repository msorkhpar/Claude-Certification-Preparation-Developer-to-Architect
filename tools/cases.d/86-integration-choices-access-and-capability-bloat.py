# Case lists of module 86-integration-choices-access-and-capability-bloat: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/86-integration-choices-access-and-capability-bloat/unit-01/practice-1"] = {
    "name": "capability", "suite": "CapabilityTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "an agent loses the tools its role does not need and the risky ones among them are named"),
        ("e1", "edge", "a tool that is held and needed but never used is reported as dormant and never removed"),
        ("e2", "edge", "a small set loads whole and a large one defers when it has ten tools or over ten thousand tokens"),
        ("e3", "edge", "the number of tools kept loaded stays between three and five and ties are broken by name"),
        ("e4", "edge", "the mechanism follows the counterpart then the path then the number of clients"),
        ("e5", "edge", "a call needs the users scope and the agents scope and an unknown tool is refused"),
        ("e6", "edge", "the gateway checks the credential then the model then the tool then the rate"),
        ("e7", "edge", "the gateway keeps a record of every decision with the team or unknown and no content"),
    ],
    "plants": {
        "wrong-risky-everything": (["m1"], "names every removed tool as risky"),
        "wrong-dormant-removed": (["e1"], "removes a needed tool because nobody used it"),
        "wrong-defer-count-only": (["e2"], "ignores the size of the definitions when it decides whether to defer"),
        "wrong-keep-unclamped": (["e3"], "keeps as many tools loaded as it is asked to, with no floor or ceiling"),
        "wrong-ties-by-insertion": (["e3"], "breaks a tie in usage by the order the tools were listed"),
        "wrong-mechanism-path-first": (["e4"], "answers a call in code before it asks whether the counterpart is an agent"),
        "wrong-auth-user-only": (["e5"], "lets any call through that the user may make, whatever the agent holds"),
        "wrong-gateway-rate-first": (["e6"], "counts the rate before it checks the model"),
        "wrong-gateway-audit-empty-on-deny": (["e7"], "keeps no record of a denied request"),
    },
}
