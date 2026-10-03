# Practice: plan a session, say what changed, and set the options

A long investigation is rarely finished in one sitting. The Agent SDK saves every session, and the code you write decides what happens on the
next sitting: resume the old conversation, resume it and tell it what changed on disk, or start a new one that carries only a short summary.
This practice has you write that decision and the pieces around it: the plan, the notice, the summary, the options that carry `resume` and
`fork_session` together, and a run that returns the session id. The plan is the course's own design, built on the documented controls; its
thresholds are stated below so that the tests can pin them. The practice is in Python and TypeScript only, because it needs the Agent SDK; a Java
or Kotlin team runs the Claude Code command line (`--resume`, `--fork-session`) as a subprocess, or keeps the message list itself as the loop of module 45
does. Pick your language folder (`python` or `typescript`), open `starter/` and edit the file there. The tests never call a model: the last case starts
the SDK against the course's stand-in for the Claude Code binary, so the SDK code that you call is the real one.

Names are Python's; the TypeScript names are in camel case (`resolveName`, `planSession`, `changeNotice`, `buildSummary`, `firstPrompt`, `sessionOptions`,
`continueOptions`, `runSession`). A saved session record is `{"id", "name", "last_used" (seconds), "files": {path: digest}}`; a plan is a map with `action`,
`session_id`, `changed`, `deleted`, `added` and `fork`. In TypeScript the SDK options are `resume`, `forkSession` and `continue`; in Python `resume`,
`fork_session` and `continue_conversation`.

## What to write

### Step 1: `plan_session(record, current, now, fork=False)`

`current` maps each file path to its digest now.

- With no record, the action is `fresh`, with no session id and no fork.
- Otherwise compare the digests of the files the session analysed with `current`: `changed` (in both, digest differs), `deleted` (analysed, gone now) and
  `added` (new), each sorted by path.
- The action is `fresh_with_summary` when more than half of the analysed files changed or are gone (counting `changed` and `deleted`, not `added`; exactly
  half is still resumed) or when the session was idle for more than 7 days (exactly 7 days is still resumed). Otherwise it is `resume_with_notice` when
  anything changed, was deleted or was added, and `resume` when nothing differs.
- The session id is the record's id for the two resume actions and null for the others. `fork` is true only when a fork was asked for and the session is
  resumed.

**Why the exam cares.** The guide asks you to choose between resuming when the earlier context is mostly valid and starting fresh with an injected summary
when the earlier tool results are stale, and to tell a resumed session about the files that changed so that it re-reads those instead of exploring everything
again. Sessions keep the conversation, not the files, so the record of digests is what makes "stale" something you can compute. **What you should see when it
works:** an untouched session resumes, one changed file gives a notice naming it, three of four changed files give a fresh start, and a week-old session
is not resumed even when nothing changed.

### Step 2: `change_notice(plan)` and `first_prompt(plan, task, summary="")`

- The notice is empty when nothing differs. Otherwise its lines are `Since your earlier analysis:`, then one line each for the sections that are not empty, in
  this order, `- changed: a.py, b.py`, `- deleted: c.py` and `- new: d.py`, then
  `Re-read these files before relying on earlier conclusions about them. Every other file is unchanged.`
- `first_prompt` puts the notice in front of the task (a blank line between) for `resume_with_notice`, the summary in front of the task for
  `fresh_with_summary`, and is just the task for `resume` and `fresh`.

**Why the exam cares.** A resumed session still believes what its old tool results said. The notice is how the new run learns which of them to distrust,
and it names files, not "some changes", so the model re-reads only what it must. **What you should see when it works:** only the files that differ are named, the
unchanged ones are not, and the summary or the notice always comes before the task.

### Step 3: `build_summary(findings, decisions, open_questions, files)`

A fresh session starts from text you wrote, so the text has a fixed layout: `## Findings`, `## Decisions`, `## Open questions` and `## Files`, each followed
by one `- item` line per item (items stripped, blanks dropped, repeats dropped, first spelling kept) or `- none`, separated by a blank line. `files` maps
a path to its digest and is listed as `- path (digest)`, sorted by path.

**Why the exam cares.** The guide says a new session with a structured summary is more reliable than a resume on stale tool results. A summary carries
conclusions and the state they rest on, never the raw tool output that went stale. **What you should see when it works:** the same four headings every time,
an empty section that says `none`, and the digests that tell the next run what the conclusions were based on.

### Step 4: `session_options(plan, **extra)`, `continue_options(sessions_in_directory, **extra)` and `resolve_name(name, index)`

- `session_options` returns the extra options plus `resume` with the plan's session id when it has one, and `fork_session` set to true together with it when
  the plan forks. A plan with no session id gets neither, even if `fork` is true.
- `continue_options` adds `continue_conversation` (TypeScript `continue`) and refuses with `ValueError` (TypeScript: an `Error`) unless the directory holds exactly one
  session, because continuing takes the most recent one.
- `resolve_name(name, index)` returns the id of the one entry of `index` (a list of `{"id", "name"}`) with exactly that name, and refuses an unknown name or a
  name shared by several sessions.

**Why the exam cares.** The guide names `--resume <session-name>` and `fork_session` as two controls; in the SDK, forking is an option you set together with a
resume id, and a name must lead to exactly one session. **What you should see when it works:** a fork carries both options, a fresh start carries neither, and
an ambiguous name or an ambiguous directory is refused instead of resuming a session you did not mean.

### Step 5: `run_session(prompt, options)` (async)

Run one single-shot `query` and return `{"session_id", "result", "error"}`: the session id and the text of the result message, and the error text when the SDK
raises (it does so after an error result, so the id of an errored run is still read).

**Why the exam cares.** The id is how you resume or fork later, and the documentation says it is "present on every result regardless of success or error". **What you
should see when it works:** a forked run reports an id different from the base session, the binary receives `--resume` and `--fork-session`, and a run that
hit its turn limit still gives you the id to resume with a higher limit.

## The cases

| Id | What it checks |
|---|---|
| `m1` | An untouched session resumes, a changed one resumes with a notice, a mostly changed one starts fresh |
| `e1` | Changed, deleted and added files are listed in order, and any difference asks for a notice |
| `e2` | More than half changed or gone starts fresh, exactly half does not, and new files do not count toward it |
| `e3` | More than a week idle starts fresh, exactly a week does not |
| `e4` | No record starts fresh, and a fork is planned only from a resumed session |
| `e5` | The notice names only what differs, and the prompt puts the notice or the summary before the task |
| `e6` | The summary has the fixed layout, with blanks and repeats dropped and files sorted by path |
| `e7` | Resume and fork go together, continue needs exactly one session, a name must be unique |
| `e8` | A run through the SDK sends `--resume` and `--fork-session` and returns the session id, also after an error |

Run the tests with the command in the language folder's `run.sh`.
