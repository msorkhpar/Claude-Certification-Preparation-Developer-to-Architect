# Plan mode or direct execution

**Level:** Architect · **Module 59:** Plan mode and iterative refinement · **Page 1 of 2**
**Exams:** A3.4; S2

**After this page you can** decide whether a code-generation task needs plan mode or can be done directly, say what plan mode does and does not stop, enter it and approve or reject a plan, and follow the explore, plan, implement, commit order.

Checked on 2026-10-03 against the Claude Code documentation pages "Choose a permission mode" (plan mode) and "Best practices for Claude Code" (explore first, then plan, then code), documenting behaviour up to Claude Code v2.1.286. Nothing here was run against a live session: the example is a model of the documented decision rules, written as plain code over task descriptions. This page deepens module 38 (permission modes) and does not repeat it. Iterating on the result is the second page.

> **Exam guide and current product.** *What the guide states (task statement 3.4), and so what the exam keys:* plan mode is for complex tasks, large-scale changes, several valid approaches, architectural decisions and multi-file modifications; direct execution is for simple, well-scoped changes such as a single-file bug fix with a clear stack trace or a date-validation check. *What the current product does (documentation checked 2026-10-03):* the same split, with a rule of thumb: "If you could describe the diff in one sentence, skip the plan." Plan mode "tells Claude to research and propose changes without making them": Claude reads files, runs shell commands to explore, and writes a plan, "but does not edit your source". Edits stay blocked until you approve the plan, with one exception: in an interactive terminal session where bypass permissions are available the blocks are not enforced, and Claude is only instructed to plan. On the exam, the question is almost always which of two tasks needs a plan; answer from the size and the number of valid approaches, not from how important the task sounds.

## Why it matters

Two requests arrive on the same morning. One asks to add a date-validation check to a single handler. The other asks to migrate a library used in forty-five files, with two plausible target designs. If Claude starts coding the second without a plan, it can change a dozen files in one direction before anyone notices that the other design was the one the team wanted, and undoing it costs more than planning would have. If Claude is made to plan the first, the plan is longer than the change. Scenario S2 asks which gets which, and the documentation's warning is symmetrical: "Plan mode is useful, but also adds overhead."

## The idea

### What plan mode does

In plan mode Claude researches and proposes, and does not change your source. Reading is free, and shell commands are controlled by the session: where auto mode is available a classifier reviews them, otherwise commands outside the read-only set ask for approval. You enter it by pressing `Shift+Tab` until the status bar shows plan mode, by prefixing one prompt with `/plan`, or by starting the session with `claude --permission-mode plan`. A project can make it the default with `defaultMode: plan` in its settings. In a headless run with `-p`, or in an Agent SDK session, "Plan mode keeps its blocks wherever Claude Code runs without an interactive terminal".

When the plan is ready Claude presents it and asks how to proceed: approve and start editing (in auto mode, or auto-accepting edits, or approving each edit by hand), or "No, keep planning" and tell Claude what to change. Approving "exits plan mode and switches the session to the permission mode each approve option describes". `Ctrl+G` opens the plan in your editor so you can change it before Claude proceeds.

### When a plan earns its cost

| The task | Plan first | Why |
|---|---|---|
| Fix a typo, add a log line, rename a variable | No | The diff fits in one sentence |
| A bug with a clear stack trace in one function | No | One file, one approach |
| Add a date check to one handler | No | Well scoped, one valid approach |
| Migrate a library used in many files | Yes | Many files, and a wrong pattern is repeated everywhere |
| Split a service into two | Yes | Architectural, several valid approaches |
| Add a feature in code you do not know | Yes | The first need is to learn the code |

Two signals decide it: the number of files and the number of valid approaches. One of them being high is enough. The documentation lists three triggers: an uncertain approach, a change to several files and unfamiliar code; the first two are these signals and the third is the last row of the table. Importance is not a signal: a one-line change in a payment module is still a one-sentence diff.

### The four phases

The documentation's recommended workflow separates research from implementation so that the work solves the right problem:

1. **Explore.** In plan mode, read the relevant code and ask questions. Nothing changes.
2. **Plan.** Ask for a detailed plan: which files change, in which order, and how the result will be checked. Edit the plan if it is wrong.
3. **Implement.** Approve, and let Claude write the code "verifying against its plan", with tests.
4. **Commit.** Ask for a commit message and a pull request.

A hybrid is normal: explore and plan for the large change, then run the implementation of each small step directly. The decision is per task, and a session can contain both.

### The example

<!-- example: m59-refinement tabs: python,typescript -->
```python
"""Three decisions of a Claude Code session on a code-generation task: plan mode or direct execution, one message or several for a list of problems, and what a failing test run must say.

The rules are the ones the exam guide states for tasks 3.4 and 3.5 and the best-practices page confirms (read on 2026-10-03): plan when the change is large, architectural,
touches many files or has more than one valid approach; execute directly when you could describe the diff in one sentence; send interacting problems in one message and
independent problems one after another; and give the model the failing tests, with input and expected output, as the target. No model is called.
"""


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
```
```text
rename a variable in one function: implement
add a date check to one handler: implement
split a monolith into services: explore > plan > implement
migrate a library used in 45 files: explore > plan > implement
messages: [['sort-order', 'pagination'], ['typo-in-label'], ['null-date']]
2 of 3 tests fail:
- empty list: input [], expected [], got None
- null entry: input [2, None], expected [2], got [2, None]
```
```typescript
/**
 * Three decisions of a Claude Code session on a code-generation task: plan mode or direct execution, one message or several for a list of problems, and what a failing test run must say.
 *
 * The rules are the ones the exam guide states for tasks 3.4 and 3.5 and the best-practices page confirms (read on 2026-10-03): plan when the change is large, architectural,
 * touches many files or has more than one valid approach; execute directly when you could describe the diff in one sentence; send interacting problems in one message and
 * independent problems one after another; and give the model the failing tests, with input and expected output, as the target. No model is called.
 */
export type Task = { diff_in_one_sentence: boolean; files: number; architectural: boolean; approaches: number };
export type Issue = { id: string; interacts_with?: string[] };
export type Result = { name: string; input: unknown; expected: unknown; actual: unknown };

/** Phases of the work: `plan` first when the change is large, architectural, spread over files or has several valid approaches. */
export function chooseMode(task: Task): string[] {
  const small = task.diff_in_one_sentence && task.files <= 1 && !task.architectural;
  if (small) return ["implement"];
  if (task.architectural || task.approaches > 1 || task.files > 1) return ["explore", "plan", "implement"];
  return ["implement"];
}

/** Messages to send, in order: problems that interact travel together, independent ones go one at a time. */
export function groupFeedback(issues: Issue[]): string[][] {
  const parent = new Map(issues.map((i) => [i.id, i.id]));
  const find = (x: string): string => {
    while (parent.get(x) !== x) {
      parent.set(x, parent.get(parent.get(x) as string) as string);
      x = parent.get(x) as string;
    }
    return x;
  };
  for (const issue of issues) {
    for (const other of issue.interacts_with ?? []) if (parent.has(other)) parent.set(find(issue.id), find(other));
  }
  const groups = new Map<string, string[]>();
  for (const issue of issues) {
    const root = find(issue.id);
    groups.set(root, [...(groups.get(root) ?? []), issue.id]);
  }
  return [...groups.values()];
}

const show = (v: unknown): string => (v === undefined ? "None" : v === null ? "null" : JSON.stringify(v));

/** The message that closes the loop: each failing test with its input, the expected output and the actual one; nothing about the passing tests. */
export function failureReport(results: Result[]): string {
  const failing = results.filter((r) => JSON.stringify(r.actual) !== JSON.stringify(r.expected));
  if (failing.length === 0) return "All tests pass.";
  const lines = [`${failing.length} of ${results.length} tests fail:`];
  for (const r of failing) lines.push(`- ${r.name}: input ${show(r.input)}, expected ${show(r.expected)}, got ${show(r.actual)}`);
  return lines.join("\n");
}

function main() {
  const tasks: Record<string, Task> = {
    "rename a variable in one function": { diff_in_one_sentence: true, files: 1, architectural: false, approaches: 1 },
    "add a date check to one handler": { diff_in_one_sentence: true, files: 1, architectural: false, approaches: 1 },
    "split a monolith into services": { diff_in_one_sentence: false, files: 60, architectural: true, approaches: 3 },
    "migrate a library used in 45 files": { diff_in_one_sentence: false, files: 45, architectural: false, approaches: 1 },
  };
  for (const [name, task] of Object.entries(tasks)) console.log(`${name}: ${chooseMode(task).join(" > ")}`);
  const issues: Issue[] = [{ id: "sort-order", interacts_with: ["pagination"] }, { id: "pagination", interacts_with: ["sort-order"] }, { id: "typo-in-label", interacts_with: [] }, { id: "null-date", interacts_with: [] }];
  console.log("messages:", JSON.stringify(groupFeedback(issues)));
  const results: Result[] = [{ name: "keeps order", input: [3, 1, 2], expected: [1, 2, 3], actual: [1, 2, 3] }, { name: "empty list", input: [], expected: [], actual: null }, { name: "null entry", input: [2, null], expected: [2], actual: [2, null] }];
  console.log(failureReport(results));
}

if (import.meta.main) main();
```
```text
rename a variable in one function: implement
add a date check to one handler: implement
split a monolith into services: explore > plan > implement
migrate a library used in 45 files: explore > plan > implement
messages: [["sort-order","pagination"],["typo-in-label"],["null-date"]]
2 of 3 tests fail:
- empty list: input [], expected [], got null
- null entry: input [2,null], expected [2], got [2,null]
```
<!-- /example -->

## Traps

1. **"Always start in plan mode; planning can only help."** It is tempting because a plan sounds careful. The exam rejects it: plan mode "adds overhead", and for a change whose diff fits in a sentence the plan is longer than the work.
2. **"The change is critical, so plan first, however small."** It is tempting because the stakes are high. The exam rejects it: the signal is scope and the number of valid approaches, not stakes; a one-function fix with a clear trace is executed directly and checked by its tests.
3. **"Start coding the migration and rework the design if it goes wrong."** It is tempting because Claude is fast. The exam rejects it for a many-file change with several approaches, where a wrong early choice is repeated across the codebase.
4. **"Plan mode makes Claude's shell commands harmless."** It is tempting because edits are blocked. The product is more careful: Claude does run commands to explore, and what happens to a command depends on the session, from a classifier review to an approval prompt to no check at all when bypass permissions are available interactively.

## Quiz

1. A developer must replace a logging library that appears in forty-five source files, and two target designs would both work. How should the session begin?
   - **a**: Researching in plan mode, then settling the approach before any edit
   - **b**: Straight to editing, so that the first files show which design works
   - **c**: Straight to editing, then a plan once ten files are done
   - **d**: In plan mode only if the library touches a security-sensitive area

2. A stack trace points at one function that divides by zero when a list is empty. How should the fix be handled?
   - **a**: In plan mode, because every bug fix needs an approved plan
   - **b**: In plan mode, because dividing is a risky operation
   - **c**: Directly, then confirm with a test of that edge case
   - **d**: After an interview about the feature that owns the function

<details>
<summary>Answer key</summary>

1. **a**. Many files and several valid approaches both argue for planning first. *b* is ruled out because the table says of a many-file change that "a wrong pattern is repeated everywhere", so the first files would commit the whole codebase. *c* is ruled out because the documented workflow "separates research from implementation so that the work solves the right problem", which a late plan cannot do. *d* is ruled out because "Importance is not a signal", and the signals are scope and the number of approaches.
2. **c**. One file with a clear trace is the guide's example of direct execution. *a* is ruled out because "Plan mode is useful, but also adds overhead", and a small clear fix does not repay it. *b* is ruled out because "Two signals decide it: the number of files and the number of valid approaches", and risk is neither. *d* is ruled out because an interview suits a large unclear feature, and here "If you could describe the diff in one sentence, skip the plan".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
