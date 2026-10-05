# Case lists of module 50-task-decomposition: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/50-task-decomposition/unit-01/practice-1"] = {
    "name": "decompose", "suite": "DecomposeTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "each file is reviewed alone and the cross pass reads their summaries"),
        ("e1", "edge", "a file pass sees only its own file and the cross pass sees summaries never the text"),
        ("e2", "edge", "a long file is reviewed in labelled parts and a blank file is skipped"),
        ("e3", "edge", "a failing file is reported and left out of the cross pass which needs two files"),
        ("e4", "edge", "the planner is asked again after each step with the steps so far and the loop ends when it says done"),
        ("e5", "edge", "the loop stops on a repeated subtask or no next step or an unreadable reply and counts the step limit exactly"),
        ("e6", "edge", "the strategy follows what is known about the steps and whether the items interact"),
    ],
    "plants": {
        "wrong-shared-context": (["e1"], "gives each file pass the text of every file"),
        "wrong-cross-gets-text": (["e1"], "hands the cross pass the file text beside the summaries"),
        "wrong-no-chunking": (["e2"], "sends a long file in one piece whatever the limit"),
        "wrong-blank-reviewed": (["e2"], "reviews a file that holds only blank lines"),
        "wrong-failed-in-cross": (["e3"], "keeps a failed file among the reviewed ones"),
        "wrong-cross-with-one": (["e3"], "runs the cross pass when only one file was reviewed"),
        "wrong-no-history": (["e4"], "asks the planner without the steps done so far"),
        "wrong-repeat-allowed": (["e5"], "lets the planner repeat a subtask"),
        "wrong-limit-off-by-one": (["e5"], "runs one step more than the step limit"),
        "wrong-strategy-items-first": (["e6"], "chooses per item and cross even when the steps are not known"),
    },
}
