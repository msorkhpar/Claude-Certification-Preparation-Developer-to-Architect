# Practice: an exploration that writes down what it learns and can be resumed after a crash

A team lets agents explore a large codebase for hours. The main conversation fills with file contents until its answers talk about "typical patterns" instead of the classes it found an hour
ago; when a worker crashes, the whole exploration starts again from nothing; and the manifest the coordinator wrote lists a worker as finished although its notes were never saved. In this
practice you write the pieces that keep an exploration alive outside any one context window: the scratchpad of findings, the manifest of agent state, the plan for resuming after a crash,
the prompt that continues an agent from its exported state, and the `/compact` command that tells compaction what to keep. The model is not called. It is in Python, TypeScript, Java and
Kotlin;

Names are Python's (`add_finding`, `render_scratchpad`, `build_manifest`, `resume_plan`, `resume_prompt`, `compact_command`); TypeScript has the camel-case names; Java has the same
camel-case names as static methods of `Recovery` with the records the starter defines (`Finding`, `AgentEntry`, `Manifest`, `Action`); Kotlin has top-level functions and data classes.

## What is already written, and what you write

The starter is a working exploration recovery helper with six gaps cut out of it. Everything that is plumbing is written and correct: the records, the order of the areas, the sorting of the manifest and the join of the lines. Each gap is marked `TODO k of N` with a comment that says what it receives and returns, with one example, and the cases it unlocks. A gap leaves a neutral value (nothing added, an empty list, `null`, the unchanged input), so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file: a run shows the logged lines under the failing case. Write the gaps in this order (the TypeScript, Java and Kotlin names are the camel-case forms where a name is given):

1. One finding per area and fact (unlocks `m1`): a finding whose area and fact are already recorded is not added again; the first one stays, in first-seen order, and the list given is never changed.
2. The grouping by area (unlocks `e1`): the scratchpad has one `## area` block per area, in first-seen order, and each block lists only the findings of that area as `- fact (location)`.
3. The bad manifest input (unlocks `e2`): two agents with the same name, or an agent with a status other than `done`, `running` or `failed`, are refused with an error.
4. The resume actions (unlocks `e3`, `e4`, `e5`): an agent whose state file is missing is restarted from scratch (`restart`); a finished agent with its state file is reused (`reuse`) and not run again; a running or failed agent with a state file is resumed from it (`resume`).
5. The resume prompt (unlocks `e6`): the prompt is the task, then a blank line, `State from the last run:`, one `- line` per state line and `Continue from the first unfinished step.`, and nothing else; with no state lines it is the task alone.
6. The compact command (unlocks `e7`): `/compact` alone when there is nothing to keep, otherwise `/compact Focus on ` and the things to keep joined with `, `.

`m1` needs gap 1. About fourteen lines in all. The steps below describe the whole helper, so you can see how your gaps are used.

## What to write

- `add_finding(findings, area, fact, location)` returns a new list (the input is not changed). A finding is `{area, fact, location}`. The same `fact` in the same `area` is recorded once, whatever
  its location; the same text under another area is a different finding. Findings stay in the order they were first added.
- `render_scratchpad(findings)` returns Markdown: for each area, in the order the areas first appear, a heading `## <area>` and one line `- <fact> (<location>)` for each of its findings in order;
  the blocks are separated by one blank line. No findings give an empty string.
- `build_manifest(agents)` takes agents `{name, state_file, status}` with status `done`, `running` or `failed`, and returns `{version: 1, agents: [...]}` with the agents sorted by name and
  each one holding exactly those three fields. A duplicate name or an unknown status is an error.
- `resume_plan(manifest, existing_files)` returns `(name, action)` for every agent of the manifest, in its order: `restart` when the agent's state file is not among the existing files (whatever its
  status), otherwise `reuse` for a `done` agent and `resume` for any other.
- `resume_prompt(task, state_lines)` returns the task alone when there are no state lines. Otherwise it returns the task, a blank line, `State from the last run:`, one line `- <line>` for each state
  line and a last line `Continue from the first unfinished step.`
- `compact_command(keep)` returns `/compact` when there is nothing to keep, otherwise `/compact Focus on ` followed by the items joined by `, `.

## Why each part is there, and what you should see

1. **The scratchpad.** Findings written to a file survive the context window that found them, and a fact recorded twice is noise. *You should see* each fact once, grouped by area.
2. **The manifest.** The coordinator needs one place to learn which agent did what. *You should see* every agent with its state file and status, in a stable order, and a bad status refused.
3. **Trust the file, not the status.** A manifest that says `running` or even `done` is only a claim until the state file exists. *You should see* `restart` for an agent whose file is missing.
4. **Reuse what is done.** A finished agent is not run again; its exported state goes into the next prompt. *You should see* `reuse`, and `resume` for the agent that stopped half-way.
5. **Inject state, not transcripts.** A resumed agent gets the lines it exported and an instruction to continue, and nothing from the old conversation. *You should see* exactly that prompt.
6. **Steer compaction.** `/compact` accepts instructions about what to keep. *You should see* the command carry them.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A finding is recorded once per area and fact, in first-seen order |
| `e1` | The scratchpad groups findings under their area |
| `e2` | The manifest lists every agent with its state file and status, and refuses bad input |
| `e3` | A finished agent with its state file is reused and not run again |
| `e4` | A running or failed agent with a state file is resumed from it |
| `e5` | An agent whose state file is missing is restarted from scratch |
| `e6` | The resume prompt carries the task and the state lines and nothing else |
| `e7` | The compact command names what to keep |
