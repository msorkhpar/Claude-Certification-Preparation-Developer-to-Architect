"""Three decisions of a Claude Code session on a code-generation task: plan mode or direct execution, one message or several for a list of problems, and what a failing test run must say.

The rules are the ones the exam guide states for tasks 3.4 and 3.5 and the best-practices page confirms (read on 2026-10-03): plan when the change is large, architectural,
touches many files or has more than one valid approach; execute directly when you could describe the diff in one sentence; send interacting problems in one message and
independent problems one after another; and give the model the failing tests, with input and expected output, as the target. No model is called.
"""
import logging

log = logging.getLogger(__name__)


def choose_mode(task):
    """Phases of the work: `plan` first when the change is large, architectural, spread over files or has several valid approaches."""
    small = task["diff_in_one_sentence"] and task["files"] <= 1 and not task["architectural"]
    if small:
        return ["implement"]
    if task["architectural"] or task["approaches"] > 1 or task["files"] > 1:
        return ["explore", "plan", "implement"]
    return ["implement"]


def group_feedback(issues):
    """Messages to send, in order: problems that interact travel together, independent ones go one at a time. issues: [{"id", "interacts_with": [ids]}]."""
    parent = {i["id"]: i["id"] for i in issues}

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    for issue in issues:
        for other in issue.get("interacts_with", []):
            if other in parent:
                parent[find(issue["id"])] = find(other)
    groups = {}
    for issue in issues:
        groups.setdefault(find(issue["id"]), []).append(issue["id"])
    return list(groups.values())


def failure_report(results):
    """The message that closes the loop: each failing test with its input, the expected output and the actual one; nothing about the passing tests."""
    failing = [r for r in results if r["actual"] != r["expected"]]
    if not failing:
        return "All tests pass."
    lines = [f"{len(failing)} of {len(results)} tests fail:"]
    lines += [f"- {r['name']}: input {r['input']!r}, expected {r['expected']!r}, got {r['actual']!r}" for r in failing]
    return "\n".join(lines)


def main():
    tasks = {
        "rename a variable in one function": {"diff_in_one_sentence": True, "files": 1, "architectural": False, "approaches": 1},
        "add a date check to one handler": {"diff_in_one_sentence": True, "files": 1, "architectural": False, "approaches": 1},
        "split a monolith into services": {"diff_in_one_sentence": False, "files": 60, "architectural": True, "approaches": 3},
        "migrate a library used in 45 files": {"diff_in_one_sentence": False, "files": 45, "architectural": False, "approaches": 1},
    }
    for name, task in tasks.items():
        print(f"{name}: {' > '.join(choose_mode(task))}")
    issues = [{"id": "sort-order", "interacts_with": ["pagination"]}, {"id": "pagination", "interacts_with": ["sort-order"]}, {"id": "typo-in-label", "interacts_with": []}, {"id": "null-date", "interacts_with": []}]
    print("messages:", group_feedback(issues))
    results = [{"name": "keeps order", "input": [3, 1, 2], "expected": [1, 2, 3], "actual": [1, 2, 3]}, {"name": "empty list", "input": [], "expected": [], "actual": None},
               {"name": "null entry", "input": [2, None], "expected": [2], "actual": [2, None]}]
    print(failure_report(results))


if __name__ == "__main__":
    main()
