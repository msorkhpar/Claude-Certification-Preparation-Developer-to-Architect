# Per-item passes, adaptive guards and the practice

**Level:** Architect · **Module 50:** Task decomposition · **Page 2 of 2**
**Exams:** A1.6

**After this page you can** design the per-file and cross-file passes of a large review so that each call sees only what it needs, handle a file that is too long and a pass that fails, run an adaptive investigation with guards that stop it when it is done, stuck, confused or out of steps, plan an open-ended task by mapping, ranking and re-planning, and write the module's practice: a review, an adaptive loop and the choice between them.

Checked on 2026-10-03 against Anthropic's engineering articles on building effective agents and on its multi-agent research system, the Claude Code documentation page "Best practices for Claude Code", and the exam guide for the Architect Foundations exam (version 1.0, July 2026). The practice is offline in Python, TypeScript, Java and Kotlin: the model is a function that the tests script, so the tests grade the control flow. The shapes of the results (`files`, `cross`, `failed`, `status`, `reason`) and the strategy names are this course's own design, and the statement says so.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* prompt chaining is shown with a review that analyses each file on its own and then runs an integration pass across files; the adaptive form is shown with "add comprehensive tests to a legacy codebase", where the plan changes as dependencies are discovered. *What the current documentation says (checked 2026-10-03):* for work that is spread over many files, Claude Code's best-practices page describes a fan-out in which a script loops over a list of files and calls `claude -p` for each, and tells you to "Test on a few files, then run on all of them". It also advises that a plan is worth its overhead when "the change modifies multiple files" and is skipped when "you could describe the diff in one sentence". Neither source says how a long file or a failed pass should be handled; those are design decisions, taken in this practice, that the exam only touches when an option asks what a pass should be given.

## Why it matters

The first page chose the shapes. This page is about their edges, which is where real jobs break. A file of four thousand lines does not fit a pass of its own. One of twelve file passes fails. The cross pass is given the text of every file "to be thorough" and becomes the single request that the split was meant to avoid. The adaptive planner asks for the step it has just done, or answers with something that is not a plan, and the loop runs to its limit and reports success. These are failures of control flow, they are tested like any other code, and the exam describes them as scenarios.

## The idea

### What each pass is given

The rule behind the split is simple to state and easy to break: **a pass is given what it needs, and nothing else.**

| Pass | Is given | Is not given | Returns |
|---|---|---|---|
| A file pass | One file (or one part of one), its path, its position in the file | Any other file, any other pass's result | Findings about the file, and a summary |
| The cross pass | The path and summary of each file that was reviewed | The text of any file | Findings about relations between files |

The summary is the interface between the two. The cross pass needs what each file pass concluded about what the file offers and what it expects from others. A file pass is asked to write it for the cross pass: what the file offers (the functions and types others use, with their parameters), what it expects of others, and what it changed. A summary that says "reviewed, looks fine" starves the cross pass. A summary that copies the file brings the problem back. In the example of page 1 the mismatch was found because one summary said that a function passes an id and another said that its counterpart looks a row up by name.

The same rule decides what a pass should be told about the rest. A file pass does not need to know how many other files there are or what they found. Giving it the earlier findings "for context" lets one pass's mistakes become the next one's premises, which module 46 described for subagents.

### A file that is too long

A file that does not fit a pass of its own is cut into parts of a fixed number of lines, and each part is a pass that knows its position: part 2 of 3. The findings of the parts are joined in order, and the summaries are joined into one, so that the cross pass still sees one entry for the file. Three details are worth testing:

- A file that is exactly as long as the limit is one part, not two.
- The parts do not overlap, so a finding is not reported twice. If a construct spans the cut, the part summaries are where a later pass learns of it, and a team that cares cuts at function boundaries.
- A file of blank lines is not sent at all. A call with nothing to review costs money and returns an answer that sounds as if it reviewed something.

### A pass that fails

The file passes are independent, so a failure in one is a fact about that file. The practice records it (path and message) and goes on with the others, and it leaves the failed file out of the cross pass. Two decisions are in that sentence. The cross pass sees only the files that were reviewed, because a summary that is empty or invented would be read as "nothing to report". And it runs only when at least two were reviewed, since a relation needs two ends. The result names every failed and skipped file, so that nobody reads a quiet report as full coverage (module 46: failures are not findings).

### Run them side by side, then join

The file passes do not depend on one another, so they can run at the same time: the article's description of parallel work is that it is effective "when the divided subtasks can be parallelized for speed". The cross pass depends on all of them, so it starts when the last has finished. The exam's version of this is a coordinator that emits the file passes in one turn and the cross pass in a later one (module 47).

### Guards for the adaptive loop

The adaptive loop is a `while True`, which is why it needs more than a limit. The practice has four ways out, each with its own status, so that a reader of the result can tell them apart; the last row of the table is not one of them.

| Status | When | What it means |
|---|---|---|
| `done` | The planner says so | The only success; the summary is the planner's |
| `stuck` | The planner gives no next step, or gives one it has already run | The plan has stopped making progress; the reason says which |
| `bad_plan` | The reply is not a plan | The loop cannot decide what to do and does not guess |
| `step_limit` | The limit is reached and the planner still wants a new step | The backstop; the work is unfinished and says so |
| (an error in a step) | The worker raises | Not an exit: the step is recorded as `ERROR: ...` and the planner decides |

Two points of order carry the grading. The planner is asked once more after the last allowed step, so that a run which finishes on its final step is `done` and not `step_limit`; the limit counts steps run, not questions asked. And the repeated-step check ignores case and outer spaces, because a planner that has been stuck rarely repeats itself byte for byte. The limit is the one the article asks for ("include stopping conditions (such as a maximum number of iterations) to maintain control"), and it is the last of the guards, not the first: module 45 made the same point for the loop of a single agent.

The planner is given the goal and the steps done so far; take the history away and it cannot see what it has already done. An error in a step is the interesting case. Stopping the run on a failed step throws away a planner that could have chosen another route; ignoring it hides a fact the planner needs. Recording it as the step's result puts it where the planner reads, with the ground truth of the other steps.

### Open-ended work: map, rank, plan again

The guide's example is a task with no natural steps: add comprehensive tests to a legacy codebase. The plan it describes has a shape that you can use for any such task.

1. **Map.** Find the structure first: modules, entry points, how they depend on each other, what already has tests. The Claude Code documentation's version is exploration before planning: "Separate research and planning from implementation to avoid solving the wrong problem." Keep the exploration out of the main context, since "Subagents run in separate context windows and report back summaries" (module 47).
2. **Rank.** Choose where the effort goes: the areas that matter most and break most easily, which a flat walk through directories would reach last or never.
3. **Plan, then plan again.** Start on the first area. What the tests reveal (a hidden dependency, a module that cannot be tested without another) changes the next subtask, and that is the loop of this module.

The documentation warns about the failure at the first step: an investigation that is not scoped reads "hundreds of files, filling the context". The cure it gives is the one the practice builds: scope the work, or give the exploration to a subagent so that it does not consume the main context.

### The practice

The practice is `exercises/50-task-decomposition/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin, and it is written as three steps, each with the reason the exam cares and what you should see when it works. You write `review_changes` (a pass for each file, in parts when long, a cross pass over summaries, failures and skips reported), `run_adaptive` (the loop and its guards) and `choose_strategy` (the table of page 1).

Seven cases grade it: the main path, the isolation of each pass, long and blank files, failures and the two-file rule for the cross pass, the planner's history and the end of a run, the stuck and limit cases, and the strategy. The starter fails all seven, the reference passes them, and each of ten planted wrong solutions per language fails on an assertion of the case it breaks: a file pass given every file's text, a cross pass given the text beside the summaries, a long file sent in one piece, a blank file reviewed, a failed file kept among the reviewed ones, a cross pass with a single file, a planner asked with no history, a repeated subtask allowed, a step limit one too generous, and a strategy that asks about the items before it asks whether the steps are known.

## Traps

These are the wrong answers that the exam's scenarios offer for this task statement, with the reason each is rejected.

1. **"Give the integration pass the full text of every file, so that nothing is missed."** It is rejected because the integration pass would be the one overloaded request that the split removed. It is given what each file pass concluded.
2. **"Let the next file pass see the findings of the previous ones, for consistency."** It is rejected because the passes become dependent and the earlier mistakes become premises. Consistency is the job of the cross pass.
3. **"If one file pass fails, discard the review and start again."** It is rejected because the other passes are valid. The failure is reported by file, and the rest goes on.
4. **"Raise the iteration limit until the investigation finishes."** It is rejected because a limit is a backstop. A loop that runs into it needs a progress check, and the run needs a status that says it was cut off.
5. **"Plan the whole investigation first and execute the plan."** It is rejected because the plan is written before the first result exists. An open-ended task is planned again after each step.

## Quiz

3. In an adaptive investigation the step chooser keeps proposing "list the test files", which was already done, and the loop hits its step limit each time. What is the best change?
   - **a**: Double the step limit and let the planner move on by itself
   - **b**: Halt with a stuck status once a task repeats, and name the repeated one
   - **c**: Remove the planner's history and judge every request on its own
   - **d**: Let the worker skip the repeated subtask silently and carry on with the next

4. A large review runs one pass per module and then a final pass across modules, which must catch a function whose signature changed in one module while code in another module still uses the old form. What should the final pass receive?
   - **a**: The complete text of each module, laid side by side for comparison
   - **b**: The findings of the earlier passes, with no account of the modules
   - **c**: A one-line verdict from each module pass on whether it looks sound
   - **d**: For each unit, the interfaces it exposes and those it calls elsewhere

<details>
<summary>Answer key</summary>

3. **b**. A repeated subtask is a lack of progress, and a status says so. *a* is ruled out because the limit is a backstop and the cause is the lack of progress: "A loop that runs into it needs a progress check". *c* is ruled out because the history is what lets the planner see what it has done: "The planner is given the goal and the steps done so far". *d* is ruled out because a silent skip leaves no status behind: "so that a reader of the result can tell them apart".
4. **d**. A mismatch between modules is visible from what each one offers and what it uses, so that is what each pass should hand on. *a* is ruled out because the text would bring the overload back: "the one overloaded request that the split removed". *b* is ruled out because findings alone say nothing about what the files offer: "what the file offers (the functions and types others use, with their parameters)". *c* is ruled out because a bare verdict says nothing about what the module offers or uses, and a summary of that kind "starves the cross pass".

</details>

## Module quiz

This quiz covers both pages of the module.

1. A platform team runs one fixed checklist (lint, test, report) over forty unrelated systems. A colleague wants an adaptive planner for each because it is more capable. What is the best answer?
   - **a**: Give each service its own planner that picks the next step
   - **b**: Give one planner all forty services in a single shared session
   - **c**: Script the three steps as a chain and repeat it per service
   - **d**: Review each service alone, then run one pass across the group

2. A team's adaptive investigation asks the planner for each next step, passing it the goal and the list of subtasks it has chosen so far. After a few steps the plan drifts away from the true state of the code. What fixes it?
   - **a**: Ask it to restate the goal more carefully before each step
   - **b**: Plan every step up front and run the plan unchanged
   - **c**: Give it the result of each action, such as a test run
   - **d**: Switch to a fixed chain that runs the same steps every time


3. A source file has four thousand lines, which is too long for one pass. By the practice's own rule for long files, how should it be handled?
   - **a**: Send its first part only and skip the rest as too long to review
   - **b**: Split it into overlapping windows so no construct is broken at a cut
   - **c**: Send the whole file anyway and trust the pass to keep its parts connected
   - **d**: Review it in numbered slices and merge their summaries into one entry

<details>
<summary>Answer key</summary>

1. **c**. Known steps and independent items call for the simplest shape. *a* is ruled out because capability has a price: "Agentic systems often trade latency and cost for better task performance." *b* is ruled out because the steps are known, so a planner of any kind buys nothing: "pays for flexibility that it never uses". *d* is ruled out because a cross pass is for items that affect each other: "Items that do not (forty services, each given the same checklist) need only the chain, run forty times."
2. **c**. The planner needs ground truth from each step, not its own earlier guesses. *a* is ruled out because the source of the drift is what it is shown: "a plan that is made from the model's own previous guesses drifts". *b* is ruled out because an up-front plan has no results to use: "the plan is made before any ground truth exists". *d* is ruled out because the next step of an investigation depends on what the last one found, which a chain ignores: "If step two finds that step three is pointless, the chain runs step three."

3. **d**. Parts keep each pass small and the summary joins them. *a* is ruled out because the rest of the file would never be reviewed: "A file that does not fit a pass of its own is cut into parts of a fixed number of lines". *b* is ruled out because overlap reports a finding twice, and the practice cuts without it: "The parts do not overlap, so a finding is not reported twice." *c* is ruled out because the whole file brings back the problem of the split: "spends that budget on text before the review begins".

</details>
