# A fixed chain or an adaptive plan

**Level:** Architect · **Module 50:** Task decomposition · **Page 1 of 2**
**Exams:** A1.6

**After this page you can** name the two ways to decompose a task that the Architect exam contrasts (a fixed sequential chain and a plan that is built from findings), say what makes a task fit each, choose between them from two facts about a task (whether its steps are known, and whether its items affect each other), and explain why a review of many files is split into one pass per file and one pass across files.

Checked on 2026-10-03 against Anthropic's engineering article on building effective agents, the Claude Code documentation page "Best practices for Claude Code", and the exam guide for the Architect Foundations exam (version 1.0, July 2026). The example runs offline in Python and TypeScript with hand-written stand-ins for the model, so it shows how the control flow behaves and nothing about how a model would review code. This page builds on module 34 (the workflow patterns), module 46 (the coordinator) and module 48 (steps that must happen in order).

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* there are two ways to break a complex job down, a fixed sequential pipeline (prompt chaining) and a dynamic decomposition that generates subtasks from what each step discovers. A large review is split into a local pass for each file and a separate cross-file pass, "to avoid attention dilution". An open-ended task such as adding tests to a legacy codebase is mapped first, then the high-impact areas are found, then a prioritised plan is made that adapts as dependencies appear. *What the current documentation says (checked 2026-10-03):* the same two shapes are two of several patterns in Anthropic's article (it also names parallel sections, voting and an evaluator that gives feedback in a loop). The mechanism it gives for keeping each call small is that Claude's context fills up and "performance degrades as it fills", which is the documented counterpart of the guide's word "dilution". Claude Code has a built-in `/batch` command that splits a change across 5 to 30 subagents, each in its own worktree. The exam keys the guide's two-way framing; an option that names a third pattern is right only if it answers the scenario's constraint better.

## Why it matters

Two questions decide the shape of almost every agent job: do you know the steps in advance, and does each item need to be seen next to the others? A job with known steps wants a fixed chain, because the chain is cheap, predictable and testable. A job whose steps depend on what you find wants a plan that changes as the findings come in. Choosing the second shape for the first job pays for flexibility that it never uses, and choosing the first for the second job produces a pipeline that marches through steps that no longer make sense. The exam asks for this choice in scenarios, and it asks about the most common concrete case, the large code review.

## The idea

### A fixed chain

The article defines it in one sentence: "Prompt chaining decomposes a task into a sequence of steps, where each LLM call processes the output of the previous one." It names the condition for using it: "This workflow is ideal for situations where the task can be easily and cleanly decomposed into fixed subtasks." You write the steps. Each is a call with a prompt that suits it, and your code passes the output of one to the next. Checks can sit between steps to see that the process is still on track, in the same way as the gates of module 48.

What the chain gives you is what a program gives you: you can test each step alone, you can see where it went wrong, and the cost is known before it runs. What it cannot do is change its mind. If step two finds that step three is pointless, the chain runs step three.

A code review of a change with a known set of aspects is a chain: first the security pass, then the tests pass, then the style pass. The steps come from the review policy, not from the change. A review of a set of files can be a chain of a different kind, which is the next section.

### A plan built from findings

The other shape has no fixed steps. The article describes it for orchestrator-workers: "a central LLM dynamically breaks down tasks, delegates them to worker LLMs, and synthesizes their results", well suited "for complex tasks where you can't predict the subtasks needed". The wider description of agents is the open-ended problem "where it's difficult or impossible to predict the required number of steps, and where you can't hardcode a fixed path."

Three things make it work, and the practice of this module implements all three.

1. **The plan is made again after each step.** The planner is given the goal and the steps done so far, and returns either "done" or the next subtask. The loop is yours: it calls the planner, runs the worker, records the step and asks again.
2. **Each step brings ground truth.** The article says that during execution it is crucial for the agents to get ground truth from the environment at each step, such as tool call results or code execution. A step that returns a test run or a file listing gives the planner something real to plan from, and a plan that is made from the model's own previous guesses drifts.
3. **There are guards.** The article's advice is to "include stopping conditions (such as a maximum number of iterations) to maintain control". The next page adds the stuck cases.

The price is stated in the article too: "Agentic systems often trade latency and cost for better task performance." The advice that follows is the one the exam uses to reject over-built answers: "We recommend finding the simplest solution possible, and only increasing complexity when needed."

### The two questions

| | The steps are known | The steps depend on what is found |
|---|---|---|
| **The items are independent** (each can be handled alone) | A fixed chain, run once for each item | An adaptive plan |
| **The items affect each other** | A pass for each item, then a pass across items | An adaptive plan |

Read the table by its columns. The first question is whether the steps can be written down. If they cannot, the answer is the adaptive plan, whatever the items look like. If they can, the second question is whether looking at an item alone is enough. Items that affect each other (the files of one change, which call each other) need a pass that sees the relations, and the practice calls it `per_item_then_cross`. Items that do not (forty services, each given the same checklist) need only the chain, run forty times.

The practice's `choose_strategy` is this table. The order of its tests matters, and one of its planted wrong solutions swaps them: it asks about the items before it asks whether the steps are known, so that twelve interacting items with unknown steps get a fixed two-pass pipeline.

### Why a review is split in two

The guide asks for "a per-file local analysis pass plus a separate cross-file integration pass" in a large review. The two passes find different things.

- A **file pass** has one file in front of it. It can read every line, and its findings (a missing check, a wrong default, a leaked handle) are about that file. Because its context holds one file, nothing competes with that file for attention.
- A **cross pass** looks for what no single file shows: a function that passes an id to one that expects a name, a change in one file whose callers in another file are not updated. It does not need the text of the files. It needs what each file pass concluded about what the file offers and what it expects from others.

Why not one request with everything? The documentation's account of the constraint is that context fills up and "LLM performance degrades as context fills", and adds that the model may start forgetting earlier instructions and making more mistakes. A request that holds a dozen files spends that budget on text before the review begins, and the review of the last file is made with the first eleven in the way. The exam calls the effect attention dilution: the same model, with the same files, gives shallower and less even comments when it has to hold all of them at once. The split is how you give each file a full-strength review and still see the relations.

Two design points follow from this, and they come back in the practice. The cross pass reads **summaries**, not files, because giving it the text again would bring the dilution back. And the file passes are independent of one another, which means that they can run side by side; the cross pass has to wait for all of them (module 47 showed how to emit them in one turn).

### The example

The example runs a three-file change through the two passes with scripted stand-ins, and then runs an adaptive loop whose planner reads each result. It prints what each call was given.

<!-- example: m50-decomposition-flow tabs: python,typescript -->
```python
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
```
```text
per file, then across files:
  file pass api.py: 2 lines, no other file
  file pass db.py: 2 lines, no other file
  file pass ui.py: 2 lines, no other file
  cross pass: 3 summaries, 123 characters of source withheld
  finding: api.py passes an id but db.py looks up by name

adaptive, each step planned from the last:
  step 1: list the test files -> 3 files
  step 2: run the failing test -> 1 failure in test_parse
  step 3: read the module under test -> parse() drops the last field
  status: done after 3 steps; the failure is in parse()
```
```typescript
// Two decompositions with scripted model replies: a per-file pass followed by one cross-file pass, and an adaptive loop that plans each step from the last.
//
// The "model" is a set of hand-written functions, so the output shows what each pass was given and what the control flow did with the answers, and nothing about
// how a real model would review the code. The files, summaries and findings are illustrative.
export const CHANGE: Record<string, string> = {
  "api.py": "def get_user(id):\n    return db.find(id)\n",
  "db.py": "def find(name):\n    return rows.get(name)\n",
  "ui.py": "def show(user):\n    print(user['name'])\n",
};
export const SUMMARIES: Record<string, string> = { "api.py": "get_user passes an id to db.find", "db.py": "find looks a row up by name", "ui.py": "show prints the name field" };

const lineCount = (text: string) => text.split("\n").filter((_, i, all) => i < all.length - 1 || all[i] !== "").length;

/** One call per file: it is given this file and nothing else. */
function filePass(path: string, text: string): string {
  console.log(`  file pass ${path}: ${lineCount(text)} lines, no other file`);
  return SUMMARIES[path];
}

/** One call over the summaries: the relations between files, never the text. */
function crossPass(summaries: Array<[string, string]>): string[] {
  const withheld = Object.values(CHANGE).reduce((n, s) => n + s.length, 0);
  console.log(`  cross pass: ${summaries.length} summaries, ${withheld} characters of source withheld`);
  const names = Object.fromEntries(summaries);
  return names["api.py"].includes("id") && names["db.py"].includes("name") ? ["api.py passes an id but db.py looks up by name"] : [];
}

type Step = [string, string];

/** A scripted planner: what it answers depends on what the steps so far found. */
export function plan(goal: string, steps: Step[]): { done: boolean; next?: string; summary?: string } {
  const done = steps.map(([s]) => s);
  if (steps.length === 0) return { done: false, next: "list the test files" };
  if (done.includes("list the test files") && !done.includes("run the failing test")) return { done: false, next: "run the failing test" };
  if (done.includes("read the module under test")) return { done: true, summary: "the failure is in parse()" };
  if (steps[steps.length - 1][1].startsWith("1 failure")) return { done: false, next: "read the module under test" };
  return { done: true, summary: "nothing failed" };
}

export function work(subtask: string): string {
  return ({ "list the test files": "3 files", "run the failing test": "1 failure in test_parse", "read the module under test": "parse() drops the last field" } as Record<string, string>)[subtask];
}

export function runAdaptive(goal: string, maxSteps = 5): [string, Step[], string] {
  const steps: Step[] = [];
  while (true) {
    const reply = plan(goal, [...steps]);
    if (reply.done) return ["done", steps, reply.summary ?? ""];
    if (steps.length >= maxSteps) return ["step_limit", steps, ""];
    steps.push([reply.next as string, work(reply.next as string)]);
    console.log(`  step ${steps.length}: ${reply.next} -> ${steps[steps.length - 1][1]}`);
  }
}

function main() {
  console.log("per file, then across files:");
  const summaries: Array<[string, string]> = Object.entries(CHANGE).map(([path, text]) => [path, filePass(path, text)]);
  for (const finding of crossPass(summaries)) console.log("  finding:", finding);
  console.log("\nadaptive, each step planned from the last:");
  const [status, steps, summary] = runAdaptive("find why the parser test fails");
  console.log(`  status: ${status} after ${steps.length} steps; ${summary}`);
}

if (import.meta.main) main();
```
```text
per file, then across files:
  file pass api.py: 2 lines, no other file
  file pass db.py: 2 lines, no other file
  file pass ui.py: 2 lines, no other file
  cross pass: 3 summaries, 123 characters of source withheld
  finding: api.py passes an id but db.py looks up by name

adaptive, each step planned from the last:
  step 1: list the test files -> 3 files
  step 2: run the failing test -> 1 failure in test_parse
  step 3: read the module under test -> parse() drops the last field
  status: done after 3 steps; the failure is in parse()
```
<!-- /example -->

The three file passes each saw two lines of one file. The cross pass saw three summaries and none of the 123 characters of source, and it found the mismatch that no file pass could see: one file passes an id, another looks up by name. The adaptive run did not follow a script: after the test run reported a failure, the planner chose to read the module under test, and after that it said it was done. Another failure would have produced another plan.

## Traps

The exam's answer options for this task statement follow a pattern. These are the wrong answers that a candidate is tempted by, and the reasons the exam rejects them.

1. **"Put every file in one request so the model sees the whole picture."** It sounds like the only way to find cross-file problems. It is rejected because the whole picture is bought with attention: the per-file depth is lost, and the cross-file findings are better made from summaries by a pass of their own.
2. **"Use an adaptive plan, since it is the more capable pattern."** It is rejected when the steps are known. The article's advice is the simplest solution that works, and an adaptive loop adds cost, latency and a way to go wrong that a fixed chain does not have.
3. **"Use a fixed pipeline for the open-ended task, with one step for each directory."** It is rejected when what is in the next directory changes what should be done next. A task such as adding tests to a legacy codebase is mapped, ranked and then planned again as dependencies appear.
4. **"Ask the model to plan every step up front, then run the plan."** It looks adaptive and is not: the plan is made before any ground truth exists. The plan has to be made again after the findings.

## Quiz

1. A team reviews a fourteen-file change by sending all the files in one request. The comments are deep for the first files and thin for the last ones, and a changed signature in one file is never matched against its callers elsewhere. Which restructuring fits best?
   - **a**: Give each source its own examination, then run a pass over what they concluded
   - **b**: Send the same single request three times and merge the answers into one report
   - **c**: Split the change into two halves of seven files and review each half in one request
   - **d**: Keep the single request and add an instruction to treat every file with equal care

2. A team must add tests to a large legacy codebase that it does not know. Which order of work does the exam expect?
   - **a**: Fix one subtask per directory in advance and run them in order
   - **b**: Ask the model to plan every test in advance, then run that plan to the end
   - **c**: Map the structure, rank the areas by impact, then re-plan as dependencies appear
   - **d**: Pick the largest files first and write tests for them until time runs out

<details>
<summary>Answer key</summary>

1. **a**. Each file gets a full review in its own context, and a second pass sees the relations. *b* is ruled out because repetition keeps what causes the problem: "A request that holds a dozen files spends that budget on text before the review begins". *c* is ruled out because halves keep the dilution and still hide the relations between the halves: "The exam calls the effect attention dilution". *d* is ruled out because an instruction does not change what fills the context: "LLM performance degrades as context fills".
2. **c**. An open-ended task is mapped first and planned from what is found. *a* is ruled out because a fixed order cannot follow the findings: "a pipeline that marches through steps that no longer make sense". *b* is ruled out because a plan made before any ground truth is a guess: "the plan is made before any ground truth exists". *d* is ruled out because size is not impact: "mapped, ranked and then planned again as dependencies appear".

</details>
