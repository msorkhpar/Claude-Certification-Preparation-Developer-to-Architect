"""Two decompositions with scripted model replies: a per-file pass followed by one cross-file pass, and an adaptive loop that plans each step from the last.

The "model" is a set of hand-written functions, so the output shows what each pass was given and what the control flow did with the answers, and nothing about
how a real model would review the code. The files, summaries and findings are illustrative.
"""

CHANGE = {
    "api.py": "def get_user(id):\n    return db.find(id)\n",
    "db.py": "def find(name):\n    return rows.get(name)\n",
    "ui.py": "def show(user):\n    print(user['name'])\n",
}
SUMMARIES = {"api.py": "get_user passes an id to db.find", "db.py": "find looks a row up by name", "ui.py": "show prints the name field"}


def file_pass(path, text):
    """One call per file: it is given this file and nothing else."""
    print(f"  file pass {path}: {len(text.splitlines())} lines, no other file")
    return SUMMARIES[path]


def cross_pass(summaries):
    """One call over the summaries: the relations between files, never the text."""
    print(f"  cross pass: {len(summaries)} summaries, {sum(len(s) for s in CHANGE.values())} characters of source withheld")
    names = {p: s for p, s in summaries}
    if "id" in names["api.py"] and "name" in names["db.py"]:
        return ["api.py passes an id but db.py looks up by name"]
    return []


def plan(goal, steps):
    """A scripted planner: what it answers depends on what the steps so far found."""
    done = [s for s, _ in steps]
    if not steps:
        return {"done": False, "next": "list the test files"}
    if "list the test files" in done and "run the failing test" not in done:
        return {"done": False, "next": "run the failing test"}
    if "read the module under test" in done:
        return {"done": True, "summary": "the failure is in parse()"}
    if steps[-1][1].startswith("1 failure"):
        return {"done": False, "next": "read the module under test"}
    return {"done": True, "summary": "nothing failed"}


def work(subtask):
    return {"list the test files": "3 files", "run the failing test": "1 failure in test_parse", "read the module under test": "parse() drops the last field"}[subtask]


def run_adaptive(goal, max_steps=5):
    steps = []
    while True:
        reply = plan(goal, list(steps))
        if reply["done"]:
            return "done", steps, reply["summary"]
        if len(steps) >= max_steps:
            return "step_limit", steps, ""
        steps.append((reply["next"], work(reply["next"])))
        print(f"  step {len(steps)}: {reply['next']} -> {steps[-1][1]}")


def main():
    print("per file, then across files:")
    summaries = [(path, file_pass(path, text)) for path, text in CHANGE.items()]
    for finding in cross_pass(summaries):
        print("  finding:", finding)
    print("\nadaptive, each step planned from the last:")
    status, steps, summary = run_adaptive("find why the parser test fails")
    print(f"  status: {status} after {len(steps)} steps; {summary}")


if __name__ == "__main__":
    main()
