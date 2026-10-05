# Case lists of module 89-migration-and-model-upgrades-at-scale: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/89-migration-and-model-upgrades-at-scale/unit-01/practice-1"] = {
    "name": "rollout", "suite": "RolloutTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a change that regresses nothing and stays inside its limits gets a go with no reasons"),
        ("e1", "edge", "a must pass case that fails blocks the change and the ids are listed in order"),
        ("e2", "edge", "a protected segment that lost answers blocks the change even when gains elsewhere match the losses"),
        ("e3", "edge", "more losses than gains blocks the change and both counts are named"),
        ("e4", "edge", "a cost rise over the limit blocks the change and a rise exactly at the limit does not"),
        ("e5", "edge", "the tail is the nearest rank 95th percentile and a single slow case does not block"),
        ("e6", "edge", "a roll out advances when healthy holds with too few requests and rolls back to zero when errors pass the limit"),
        ("e7", "edge", "the retirement calendar counts days ranks the nearest first and names the level"),
        ("e8", "edge", "migration removes the settings the new model refuses and names each change"),
    ],
    "plants": {
        "wrong-must-pass-ignored": (["e1"], "lets a change through although a case that must pass now fails"),
        "wrong-protected-ignored": (["e2"], "ignores a loss in a protected segment when the totals are equal"),
        "wrong-net-loss-ignored": (["e3"], "does not block a change that loses more answers than it gains"),
        "wrong-cost-at-limit": (["e4"], "blocks a cost rise that equals the limit"),
        "wrong-cost-rounded": (["e4"], "rounds the cost rise up to the next whole percent instead of down"),
        "wrong-p95-max": (["e5"], "judges the tail by the slowest single case"),
        "wrong-rollout-min-ignored": (["e6"], "moves a stage on before it has seen enough requests"),
        "wrong-rollout-no-rollback": (["e6"], "keeps advancing a roll-out whose errors passed the limit"),
        "wrong-days-by-name": (["e7"], "lists the models by name and not by how soon they retire"),
        "wrong-level-boundary": (["e7"], "calls a model with exactly fourteen days left merely something to migrate"),
        "wrong-migrate-keeps-sampling": (["e8"], "leaves the sampling settings in the migrated request"),
        "wrong-migrate-keeps-forced-tool": (["e8"], "leaves a forced tool choice in the migrated request"),
    },
}
