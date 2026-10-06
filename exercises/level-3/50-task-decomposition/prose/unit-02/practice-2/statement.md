# Practice: decompose a task in code

A big review does not have to be one request, and an open-ended investigation cannot be planned in advance. This practice has you write the
control flow for both: a review that looks at each file alone and then looks across files, an investigation loop in which a planner chooses
each step from the result of the last, and the rule that picks between the two. The model is a function you are given (the tests script it),
so the practice grades the part that is yours: what each call is given, what is kept, what stops the work and what is reported when something
goes wrong. It is in Python, TypeScript, Java and Kotlin;

Names are Python's (`review_changes`, `run_adaptive`, `choose_strategy`); TypeScript has `reviewChanges`, `runAdaptive`, `chooseStrategy` and the
argument `maxLines` for `max_lines`, `maxSteps` for `max_steps`; Java has `Decompose.reviewChanges`, `Decompose.runAdaptive` and
`Decompose.chooseStrategy` with an interface `FilePass`; Kotlin has top-level functions of the same names and the type aliases `FilePass`,
`CrossPass` and `StepPlanner`. Results are maps, as the examples show.

## What is already written, and what you write

The starter is a working task decomposer with nine gaps cut out of it. Everything that is plumbing is written and correct: the loops over the files and over the steps, the result maps, the calls to the worker, the step limit and the check of the task's fields. Each gap is marked `TODO k of N` with a comment that says what it receives and returns, with one example, and the cases it unlocks. A gap leaves a neutral value (nothing added, an empty list, `null`, the unchanged input), so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file: a run shows the logged lines under the failing case. Write the gaps in this order (the TypeScript, Java and Kotlin names are the camel-case forms where a name is given):

1. The call of the file pass unlocks `m1`, `e1`: each part of a file goes to the file pass alone, with the path, the part's text, the part number and the number of parts; its findings are collected and its summaries joined.
2. The blank file unlocks `e2`: a file that holds only blank lines is added to `skipped` and never sent to the file pass.
3. The number of parts unlocks `e2`: a file of N lines is reviewed in ceil(N / max_lines) parts.
4. The failing file unlocks `e3`: a file whose pass throws is recorded in `failed` with the error message, left out of `files`, and does not stop the other files.
5. The two file rule unlocks `e3`: the cross pass runs only when at least two files were reviewed.
6. The call of the cross pass unlocks `m1`, `e1`: the cross pass gets one entry `{path, summary}` per reviewed file, never the file text.
7. The history for the planner unlocks `e4`: the planner is asked again after each step with a copy of the steps so far.
8. The stuck rules unlocks `e5`: an empty next step ends with `stuck` and "no next step"; a subtask already done (ignoring case) ends with `stuck` and "repeated subtask: NAME".
9. The strategy unlocks `e6`: `adaptive` when the steps are not known, `per_item_then_cross` when they are known, there are two items or more and they interact, otherwise `fixed_chain`.

`m1` needs gaps 1 and 6. About eighteen lines in all. The steps below describe the whole decomposer, so you can see how your gaps are used.

## Build it in three steps

### Step 1: `review_changes(files, file_pass, cross_pass, max_lines=40)`

`files` is a list of `{"path", "text"}`. `file_pass(path, text, part, parts)` is one model call about one piece of one file and returns
`{"findings": [text], "summary": text}`. `cross_pass(summaries)` is one model call that receives a list of `{"path", "summary"}` and returns a list
of findings.

- Review the files in order, each in its own call, and give the call the text of that file only. A file of more than `max_lines` lines goes in
  parts of at most that many lines, `part` counting from 1 and `parts` the number of parts; the findings of the parts are joined in order and so
  are the summaries, with a space between them. A file whose text is blank is skipped and listed.
- A call that raises an exception puts that file in the failed map (its path and the message) and the other files go on.
- When at least two files were reviewed, call `cross_pass` once with the path and summary of each reviewed file, in order, and never with the
  text. A failure of the cross pass leaves an empty list of findings and its message in `cross_error`.
- Return `{"files": {path: {"findings", "summary", "parts"}}, "cross", "failed", "skipped", "cross_error"}`.

**Why the exam cares.** The Architect exam asks for a per-file pass and a separate cross-file pass to avoid attention dilution, and it offers answers
that put the whole change in one request or that give the second pass everything again. Your code is where the isolation is real: it decides what
each call sees. **What you should see when it works:** the file pass tests record exactly one file per call, the cross pass records only
summaries, a seven-line file shows up as three labelled parts, and a failed file is named without stopping the rest.

### Step 2: `run_adaptive(planner, worker, goal, max_steps=6)`

`planner(goal, steps)` returns `{"done": bool, "summary": text, "next": text}`; `steps` is the list of `{"subtask", "result"}` done so far.
`worker(subtask)` does one subtask and returns its result text.

- Ask the planner before every step, with a copy of the steps so far (the first call gets an empty list). When it answers `done`, return status
  `done` with its summary.
- Otherwise run the worker on `next` (stripped) and record the step. A worker that raises records the result `ERROR: <message>`, and the planner
  decides what that means.
- A reply that is not a map or has no boolean `done` returns `bad_plan`. A blank `next` returns `stuck` (reason `no next step`). A `next` that
  repeats an earlier subtask, ignoring case and outer spaces, returns `stuck` (the reason names it). When `max_steps` steps have run and the planner
  still wants another new one, return `step_limit`; a `done` after the last allowed step is still `done`.
- Return `{"status", "summary", "steps", "reason"}`.

**Why the exam cares.** Dynamic decomposition lets the findings of one step set the next, which is the point of an open-ended investigation, and the
exam distinguishes it from a fixed pipeline. A loop with no guard is the other half of the lesson: the planner can repeat itself, give up or
answer nonsense, and the step limit must be a backstop with its own status. **What you should see when it works:** the planner call counts are 0,
1, 2 steps in its history, a repeated subtask stops the run at once as `stuck`, and an endless planner stops after exactly `max_steps` worker runs
with one more planner call.

### Step 3: `choose_strategy(task)`

`task` is `{"steps_known": bool, "items": whole number, "items_interact": bool (optional)}`.

- `adaptive` when the steps are not known, whatever the number of items.
- `per_item_then_cross` when the steps are known, there are at least two items and they interact.
- `fixed_chain` otherwise.
- Anything else (a missing or non-boolean `steps_known`, an `items` that is missing, negative or not a whole number) is refused: raise
  `ValueError` (TypeScript: throw an `Error`; Java and Kotlin: `IllegalArgumentException`).

**Why the exam cares.** Choosing the pattern is the task statement. The order of the questions matters: whether the steps can be written down comes
first, and only then does the shape of the items matter. **What you should see when it works:** an investigation of twelve interacting items is still
`adaptive` when its steps are unknown, and a single item that "interacts" with nothing is a `fixed_chain`.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Each file is reviewed alone and the cross pass reads their summaries |
| `e1` | A file pass sees only its own file and the cross pass sees summaries, never the text |
| `e2` | A long file is reviewed in labelled parts and a blank file is skipped |
| `e3` | A failing file is reported and left out of the cross pass, which needs two files |
| `e4` | The planner is asked again after each step with the steps so far and the loop ends when it says done |
| `e5` | The loop stops on a repeated subtask, no next step or an unreadable reply, and counts the step limit exactly |
| `e6` | The strategy follows what is known about the steps and whether the items interact |
