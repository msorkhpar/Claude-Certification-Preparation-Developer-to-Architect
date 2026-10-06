# Practice: four workflow patterns around a model

A workflow is code that decides the steps and calls the model inside them, as opposed to an agent, where the model decides the steps.
Most production systems that "use agents" are workflows, and the patterns are small enough to write by hand: an orchestrator that
splits a task and hands the pieces to workers, an evaluator that sends a draft back until it is good enough, a router that picks a
path, and a vote that asks the same question several times. Each has a failure that the plain version hides: a plan the model wrote
badly, a worker that fails, a judge that answers in prose, a tie. Write all four, with those failures handled. The patterns come from the Claude
documentation and Anthropic's engineering write-up on building effective agents; the lesson pages explain them and what the course
checked. Nothing here touches the network: the model is a function from a prompt (text) to a reply (text) that the tests script.

## The given parts

| Name | Meaning |
|---|---|
| `ask`, `write`, `judge` | the model: a function that takes the prompt text and returns the reply text; it may throw |
| `routes` | a map from a label to a handler, a function of the text that returns text; its key order is the label order |

## What is already written, and what you write

The starter is a working set of four workflows with nine small gaps cut out of it. The plumbing is written and correct: the two defensive
readers (`_slice`, `parse_plan`, `read_judgement`), the loops, the counting of calls and the results of `orchestrate`, `refine`, `route` and
`vote`. Each gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it
unlocks. A gap returns a neutral value, so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log`
line at the top of the file; a run shows the lines under the failing case. Write them in this order (the TypeScript, Java and Kotlin names
are the camel-case forms, `cleanSteps` and so on):

1. `_clean_steps` unlocks `e1`: the string subtasks of the plan, stripped, without empty ones or repeats.
2. `_run_worker` unlocks `m1` and `e2`: one worker call that never raises and reports `ok` or `failed`.
3. `_combine_prompt` unlocks `m1`: the combine prompt with one line per subtask.
4. `_score_ok` unlocks `e4`: which judge scores count (a number from 0 to 10, never a boolean).
5. `_writer_prompt` unlocks `e3`: the first prompt, and the revision prompt with the previous draft and the feedback.
6. `_is_better` unlocks `e4`: when a draft replaces the best one (strictly higher, so the earliest wins a tie).
7. `_error_result` unlocks `e7`: the result when the writer raises.
8. `_normalise_label` unlocks `e5`: the label read from the classifier's reply.
9. `_pick_winner` unlocks `e6`: the most frequent answer, the first seen on a tie.

About a dozen lines in all. The sections below describe the whole workflows; the parts you do not write are there so you can see how your
functions are used.

## What to write

Prompts are part of the contract, so that a test can answer by the start of a prompt. Write them exactly; `\n` is a newline.

- `orchestrate(ask, task, max_subtasks=5)` returns `{"status", "plan", "fallback", "results", "answer", "calls"}`.
  1. Plan with one call: `Plan: split the task into at most {max_subtasks} independent subtasks. Reply with a JSON array of strings
     only.\nTask: {task}`. Read the reply defensively: take the text from the first `[` to the last `]` and parse it. Keep the string
     items, stripped, dropping empty ones and repeats (the first of a repeat stays), then keep at most `max_subtasks`. When nothing is
     usable (no array, bad JSON, no string items) the plan is just `[task]` and `fallback` is true.
  2. Run one worker call per subtask, in order: `Subtask: {subtask}\nTask: {task}`. A reply that is empty or only spaces, or a
     call that throws, makes that subtask `{"subtask", "status": "failed", "error"}` (the error text is the exception's message, or
     `empty reply`) and the others still run. A good one is `{"subtask", "status": "ok", "output"}`.
  3. When every subtask failed, do not combine: status `failed`, `answer` null. Otherwise combine with one call, `Combine: write one
     answer to the task from the results.\nTask: {task}` followed by one line per subtask in plan order, `\n{i}. {subtask} ->
     {output}` or `\n{i}. {subtask} -> FAILED` (i from 1), and return status `done`, or `partial` when some failed, with the reply
     as `answer`. `calls` counts every model call made.
- `refine(write, judge, task, max_rounds=3, threshold=8)` returns `{"status", "draft", "score", "rounds", "history"}` and, on an error,
  `"error"`. Each round, the writer prompt is `Task: {task}` in round 1 and afterwards `Task: {task}\nPrevious draft: {draft}\nFeedback:
  {feedback}\nRevise the draft.` (the latest draft and the judge's latest feedback). Then judge the draft with `Judge: score the draft
  from 0 to 10 and reply with JSON {"score": n, "feedback": "..."}.\nTask: {task}\nDraft: {draft}`. Read the judge's reply the same
  defensive way, from the first `{` to the last `}`: it is readable when it is an object whose `score` is a number from 0 to 10
  (a boolean is not a number); `feedback` is its text, or empty when missing. An unreadable reply scores 0 with the feedback `The
  judge reply could not be read.` and the loop goes on. Every round adds `{"round", "score", "feedback"}` to `history`.
  - A score at or above `threshold` ends the loop: status `accepted`, the current draft, its score, the round number.
  - After `max_rounds` without that: status `max_rounds`, the best-scoring draft (the earliest on a tie), its score, `rounds` equal to
    `max_rounds`.
  - When the writer throws, stop at once: status `error`, the best draft so far and its score (both null before any draft was judged),
    `rounds` is the number of rounds completed, and `error` is the exception's message.
- `route(ask, text, routes, default)` returns `{"label", "output", "fallback"}`. Classify with one call, `Classify: {text}\nLabels:
  {labels joined by ", "}`. The label is the reply stripped of spaces, then of the punctuation `.,;:!"'` and the backtick at both ends,
  then of spaces again, in lower case. When it is not a key of `routes`, use `default` and set `fallback` to true. `output` is the
  handler's result for the text.
- `vote(ask, prompt, n=5)` returns `{"answer", "votes", "agreement"}`: ask the same prompt `n` times, count the replies stripped and in
  lower case (in order of first appearance), and answer with the most frequent one; on a tie the one seen first. `agreement` is the
  winner's count divided by `n`. With `n` of 0 the answer is null, the votes are empty and `agreement` is 0.

## The cases

| Id | What it checks |
|---|---|
| `m1` | An orchestrator plans, runs a worker per subtask and combines |
| `e1` | The plan is read from prose, cleaned, capped, and replaced when unusable |
| `e2` | One failing worker does not stop the others or the answer |
| `e3` | A draft is revised with the feedback until the judge accepts it |
| `e4` | When the rounds run out the best draft wins, and an unreadable judge scores zero |
| `e5` | A label is read from the reply, and anything else takes the default route |
| `e6` | The majority answer wins, and a tie goes to the one seen first |
| `e7` | A writer that fails ends the loop with the best draft so far |
