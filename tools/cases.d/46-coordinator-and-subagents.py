# Case lists of module 46-coordinator-and-subagents: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/46-coordinator-and-subagents/unit-01/practice-1"] = {
    "name": "coordinator", "suite": "CoordinatorTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a hub sends one brief to each spoke and synthesizes what comes back"),
        ("e1", "edge", "a question the coordinator can answer itself is answered without any subagent"),
        ("e2", "edge", "a subagent sees its own brief and nothing the others found"),
        ("e3", "edge", "the plan is cleaned of empty briefs and duplicate scopes and capped"),
        ("e4", "edge", "a failing subagent does not stop the others and a run with no findings does not synthesize"),
        ("e5", "edge", "a review sends only the gaps back out and stops when none are left"),
        ("e6", "edge", "the rounds are capped and the gaps that remain are reported"),
    ],
    "plants": {
        "wrong-leaks-context": (["e2"], "appends what earlier subagents found to the brief of the next one"),
        "wrong-no-dedupe": (["e3"], "runs two subagents on the same scope"),
        "wrong-empty-brief-sent": (["e3"], "sends a subagent an empty brief"),
        "wrong-always-delegates": (["e1"], "builds a team even when the planner answered the question itself"),
        "wrong-failure-as-finding": (["e4"], "reports a subagent's error message as a finding"),
        "wrong-synthesizes-nothing": (["e4"], "synthesizes an answer when no subagent returned anything"),
        "wrong-rerun-all": (["e5"], "sends every first-round brief out again in each refinement round"),
        "wrong-rounds-off-by-one": (["e6"], "runs one refinement round more than the limit"),
    },
}
