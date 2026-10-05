# Case lists of module 81-reliability-of-multi-agent-systems: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/81-reliability-of-multi-agent-systems/unit-01/practice-1"] = {
    "name": "reliable_agents", "suite": "ReliableAgentsTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "tasks run in order and receive the results of the tasks they need"),
        ("e1", "edge", "every retry of a task carries the same idempotency key"),
        ("e2", "edge", "retries stop at the limit and a fatal failure is not retried"),
        ("e3", "edge", "a failure stays inside its branch and dependents are skipped"),
        ("e4", "edge", "a breaker stops calls to an agent that keeps failing and a success resets it"),
        ("e5", "edge", "a fallback degrades one task with its own key and is not checkpointed"),
        ("e6", "edge", "a second run resumes from the checkpoint and retries only what failed"),
        ("e7", "edge", "an unexpected crash is not swallowed and keeps the work already checkpointed"),
    ],
    "plants": {
        "wrong-retry-new-key": (["e1", "e3", "e5", "e6", "e7", "m1"], "sends a new idempotency key with every retry"),
        "wrong-key-changes-late": (["e1"], "sends a new idempotency key from the third attempt on"),
        "wrong-fatal-retried": (["e2", "e3", "e5"], "retries a failure that cannot be fixed by retrying"),
        "wrong-attempts-off-by-one": (["e1", "e2", "e4"], "makes one attempt fewer than the limit"),
        "wrong-dependents-still-run": (["e3"], "runs a task whose dependency failed"),
        "wrong-failure-stops-run": (["e2", "e3", "e4"], "stops the whole run at the first failed task"),
        "wrong-breaker-total": (["e4"], "counts every failure of an agent and never resets on a success"),
        "wrong-breaker-never-opens": (["e4"], "keeps calling an agent that has failed past the threshold"),
        "wrong-fallback-same-key": (["e5"], "sends the primary's idempotency key to the fallback agent"),
        "wrong-fallback-checkpointed": (["e5"], "checkpoints a degraded result as if it were final"),
        "wrong-resume-reruns": (["e6"], "runs the tasks that the checkpoint already holds"),
        "wrong-crash-swallowed": (["e7"], "turns an unexpected crash into a failed task"),
        "wrong-checkpoint-late": (["e5", "e7"], "writes the checkpoint only when the whole run ends"),
    },
}
