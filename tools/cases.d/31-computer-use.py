# Case lists of module 31-computer-use: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/31-computer-use/unit-01/practice-1"] = {
    "name": "computer", "suite": "ComputerTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the loop runs scaled actions and sends every result in one message"),
        ("e1", "edge", "the screen is scaled to what the model may see and clicks are scaled back"),
        ("e2", "edge", "a click on a risky element needs a persons confirmation"),
        ("e3", "edge", "a failed action stops the rest of its batch"),
        ("e4", "edge", "invalid actions come back as error results with a reason"),
        ("e5", "edge", "the loop ends on a stop reason or at the turn limit"),
        ("e6", "edge", "old screenshots are replaced so the context does not fill up"),
        ("e7", "edge", "screenshots and zooms are sent at the size the model may see"),
    ],
    "plants": {
        "wrong-unscaled-click": (["m1"], "uses the model's coordinates as screen coordinates without scaling them back"),
        "wrong-no-confirm": (["e2"], "clicks a payment element without asking a person"),
        "wrong-continue-after-failure": (["e3"], "keeps running the actions of a batch after one has failed"),
        "wrong-result-per-message": (["m1"], "sends each tool result in a user message of its own"),
        "wrong-screenshot-full-size": (["e7"], "sends the screenshot at the full screen size instead of the scaled size"),
        "wrong-prune-oldest-kept": (["e6"], "keeps the oldest screenshots and removes the newest"),
        "wrong-extra-turn": (["e5"], "makes one model call more than the turn limit"),
    },
}
