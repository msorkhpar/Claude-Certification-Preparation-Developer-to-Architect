# Stale results: the notice, the summary and the practice

**Level:** Architect · **Module 51:** Session state · **Page 2 of 2**
**Exams:** A1.7; S4

**After this page you can** explain why a resumed session can be wrong about files that changed, compute from digests whether to resume, resume with a notice or start fresh, write a notice that names the changed files, write a structured summary for a fresh session, and write the module's practice: the plan, the notice, the summary and the options around the SDK's session controls.

Checked on 2026-10-03 against the Claude Code documentation pages "Work with sessions" and "Sessions", with `claude-agent-sdk` 0.2.163 and `@anthropic-ai/claude-agent-sdk` 0.3.287. The practice runs offline in Python and TypeScript through the course's stand-in for the Claude Code binary; no model and no network are used. The record of digests, the thresholds, the notice and the layout of the summary are this course's own design, built on the documented controls and labelled as such: neither the exam guide nor the documentation specifies them.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* when the earlier context is mostly valid, resume; when the earlier tool results are stale, start a new session with an injected structured summary, which is more reliable than resuming on stale results; and when you do resume after code changes, tell the agent which files changed so that it re-analyses those and does not re-explore everything. *What the current product does (documentation checked 2026-10-03):* the sessions guide gives the reason in one line, "Sessions persist the conversation, not the filesystem", and, for moving across machines, the same advice as the guide: "Don't rely on session resume. Capture the results you need (analysis output, decisions, file diffs) as application state and pass them into a fresh session's prompt." It does not define a summary format or a staleness test. On the exam, pick the option that names the specific changed files for a resume and the structured summary for a fresh start; in your own code, decide staleness from data you recorded (the practice uses file digests), not from the model's impression.

## Why it matters

A session is a record of what the agent saw, and what it saw stops being true the moment someone edits a file. The agent that mapped a service on Friday still "knows" the Friday version on Monday, and it will say so with the confidence of a fresh read. Nothing in the session marks the old tool results as old. The exam tests the two honest responses: tell the resumed agent exactly what changed so that it re-reads that, or stop trusting the old conversation and begin again from a summary of its conclusions. The wrong responses are the two lazy ones, trusting everything or throwing everything away.

## The idea

### Why a resumed session can be wrong

"Sessions persist the conversation, not the filesystem." The tool results inside a session are snapshots: the contents of a file at the minute it was read, the output of a test run, a directory listing. A resume restores them as they were, and the model reasons from them as if they were current. The Edit tool protects a write (it matches the current text of the file, and the documentation says that "Claude reads the file again before editing" when an anchor is stale, module 56), but nothing protects a conclusion: a finding about `auth.py` that the agent wrote on Friday stays in context after `auth.py` was rewritten on Saturday. That is the failure the guide's task statement names, and it is silent: the answer is fluent and based on code that no longer exists.

### Record what the session analysed

You cannot tell staleness from the conversation, so record it where you can compute it: when a session ends, store the id and, for each file the agent analysed, a digest (a hash of the content, or the commit and path). On the next sitting, compute the digests again and compare. Three lists fall out, the files that changed, the files that are gone and the files that are new, and a share: how much of what the session analysed is no longer true.

The course's plan (the practice builds it) uses these rules:

| Situation | Action |
|---|---|
| No saved session | Fresh |
| More than half of the analysed files changed or are gone, or the session was idle for more than 7 days | Fresh, with a summary |
| Some files changed, are gone or are new | Resume, with a notice |
| Nothing differs | Resume |

The thresholds are a design choice, and the table says so: "mostly valid" in the guide has no number. What matters is that the rule is written down and testable. Half is a reasonable place to put the boundary, because a notice can correct a few files but cannot correct most of them, and new files do not count toward it, since they are not things the session believes. A session idle for a week is treated as stale whatever the digests say, because what the agent decided around the code (the plan, the open questions) may have moved on.

### Tell the session what changed

When the plan says resume with a notice, the first prompt begins with the notice and then the task. Name the files that differ, so the session re-reads only those:

```text
Since your earlier analysis:
- changed: auth.py, session.py
- deleted: legacy.py
- new: tokens.py
Re-read these files before relying on earlier conclusions about them. Every other file is unchanged.
```

The notice is as specific as the diff, because the alternative is expensive. "Some files changed, please re-check" sends the agent back through the whole codebase, which is the cost the resume was meant to avoid; naming the files is "targeted re-analysis rather than requiring full re-exploration". It also does not paste the new contents: the session reads them with its own tools, and the text it reads is then fresh in the conversation. It lists what changed and says what did not, so the agent can keep trusting the rest.

### Start fresh with a summary

When most of the earlier results are stale, resuming drags all of them along. A fork would not help: a fork "creates a new session that starts with a copy of the original's history", stale results included. The documentation's alternative for sessions you cannot or should not resume is a fresh session whose prompt carries what you captured. Write that prompt in a fixed layout, as the practice does:

```text
## Findings
- Auth issues tokens in session.py
## Decisions
- Keep the cookie format
## Open questions
- Is the cache shared between workers?
## Files
- auth.py (d1f3)
- session.py (a09c)
```

A summary carries conclusions and the state they rest on, never the raw tool output that went stale. The digests in the last section are not decoration: the next run can compare them with the files it sees and know which conclusions to trust. Items are short, dropped when blank or repeated, and an empty section says `none`, so the layout is the same on every run and a reader (or a model) always finds each part in the same place. Do not paste the old transcript in its place: that is the stale tool output again, in a longer form. The guide asks for a structured summary; the format here is this course's choice.

A summary has a price: "whatever the summary leaves out is no longer in Claude's context." That is the reason for the plan's order. A resume with a notice keeps everything that is still true and corrects the rest; a fresh start with a summary keeps only what you decided was worth writing down.

### Two approaches from one analysis

A fork copies the conversation and nothing else, so two approaches that both edit files need the file side handled too. A community guide puts the pairing in one sentence: "Forking the session without isolating the files leaves both attempts editing the same checkout; isolating files without forking the session intermingles the conversation transcripts." For two approaches from one prior state it calls a fork "preferable to resuming the original twice and trying to keep two diverging conversations straight, and preferable to copying-and-pasting context into a fresh session, which loses tool-call history." The file side is a separate git worktree for each branch; the conversation side is the fork. If a branch has already edited the shared checkout and you want the files back as they were, the official sessions page names the tool for that side: "To snapshot and revert file changes the agent made, use file checkpointing." The session does not hold the files, so only a snapshot of them can bring them back; resuming or deleting a session does not.

### A crash in the middle of a call

A third kind of stale state is a call that had no result. The documentation covers it: a tool that was still running when the process ended "doesn't finish or run again when you resume", and "Claude sees the call marked as cut off before its result was recorded and is told to check whether it took effect before running it again". The same rule belongs in your own tools. A resumed agent must not repeat a refund because its result never arrived (module 53 treats an uncertain side effect).

### The options, and the run

The plan becomes SDK options in one place. A resume sets `resume` to the saved id; a fork sets `fork_session` (TypeScript `forkSession`) together with it, and never alone, because a fork needs something to copy. A fresh start sets neither. `continue` is only offered when the directory holds exactly one session, since it takes the most recent. After the run, keep the id from the result, error or not. The course's `run_session` does that and returns the error text too, because a single-shot query raises after an error result.

### What Java and Kotlin teams use

The SDK controls are Python and TypeScript only. A Java or Kotlin service uses the same plan (digests, notice, summary are plain data) and the command line for the run: `claude -p --resume <id>` with `--fork-session` when branching, or a fresh `claude -p` whose prompt starts with the summary. The practice of this module is in Python and TypeScript only for that reason.

### The practice

The practice is `exercises/51-session-state/unit-01/practice-1/statement.md`, in Python and TypeScript, written as five steps, each with the reason the exam cares and what you should see when it works. You write `plan_session`, `change_notice` and `first_prompt`, `build_summary`, `session_options`, `continue_options` and `resolve_name`, and `run_session`.

Nine cases grade it: the main path, the lists of changed, deleted and added files, the half boundary, the week boundary, a fork only from a resumed session, the notice and the prompt, the summary layout, the options and the names, and a run through the real SDK against the stand-in. The starter fails all nine, the reference passes them, and each of sixteen planted wrong solutions per language fails on an assertion of the case it breaks: a resume that ignores changes, a new file that is not counted, a boundary that is off by one at half or at a week, new files counted toward the share, a fork planned without a session, a notice written when nothing differs, a summary placed after the task, repeats kept, files unsorted, a fork option without a resume, a continue with several sessions, a name that picks the first of several, an id read only after success, and a fork flag written as false.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Resume the session and trust it: it already analysed those files."** It is tempting because the analysis took an hour. The exam rejects it: the session holds what the files said then. Compare what you recorded with what is there now, and tell the session which files differ.
2. **"Tell it that some files changed and it should re-explore the codebase."** It is tempting because it is safe. The exam rejects it: that discards the point of resuming. Name the changed files so that the session re-reads those and keeps the rest.
3. **"Always start fresh, to avoid stale results."** It is tempting because a new session has no stale state. The exam rejects it: when the earlier context is mostly valid, a resume keeps hours of correct analysis. Start fresh when most of it is stale.
4. **"Start fresh and paste the old transcript into the first prompt."** It is tempting because nothing is lost. The exam rejects it: the transcript carries the stale tool results with it. Carry the conclusions and the digests they rest on, in a structured summary.

## Quiz

3. An agent mapped a service of twelve modules on Friday. Over the weekend a developer rewrote two of them. On Monday the team resumes the session. What should the first prompt add?
   - **a**: A request to survey the whole service from scratch
   - **b**: The two changed paths and a request to re-read them
   - **c**: Nothing at all, since the earlier read still holds
   - **d**: The full text of both changed modules, pasted inline

4. Most of the files an agent analysed last month have since been rewritten, and its transcript is long. Which start is most reliable?
   - **a**: A new run that begins by pasting the earlier tool output
   - **b**: The full old transcript, with a line saying that things have changed
   - **c**: A fork of the transcript, so the original stays untouched
   - **d**: A new run that opens with a structured summary of the conclusions

<details>
<summary>Answer key</summary>

3. **b**. Naming the differing files gives targeted re-analysis and keeps everything still true. *a* is ruled out because naming the files is "targeted re-analysis rather than requiring full re-exploration", and a survey discards what the resume kept. *c* is ruled out because "Sessions persist the conversation, not the filesystem", so the weekend's edits are not in it. *d* is ruled out because the notice "does not paste the new contents: the session reads them with its own tools".
4. **d**. When most results are stale, a summary of conclusions and their state is the reliable start. *b* is ruled out because a notice can correct a few files but cannot correct most of them: "More than half of the analysed files changed or are gone, or the session was idle for more than 7 days" is the course's rule for starting fresh. *c* is ruled out because a fork "creates a new session that starts with a copy of the original's history", stale results included. *a* is ruled out because a summary carries conclusions "never the raw tool output that went stale".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S4, developer productivity with Claude. The agent is built on the Claude Agent SDK and helps engineers explore unfamiliar codebases and understand legacy systems. An engineer resumes a very large session on a Pro plan in Claude Code after a two-hour break. What should they expect from the first request?
   - **a**: A replay of the tools the session called, to refresh their results
   - **b**: A warm cache, because a resumed session keeps its earlier requests
   - **c**: A cache miss, so all of the history is read again one time
   - **d**: An automatic fork, so that the earlier thread is left unchanged

2. Scenario S4, developer productivity with Claude. The agent is built on the Claude Agent SDK and helps engineers explore unfamiliar codebases and understand legacy systems. A forked agent rewrote several modules in the shared working copy. The team wants that copy exactly as it stood before the run, and wants to keep the fork's conversation. What provides this?
   - **a**: Resuming the original session, which brings its files back
   - **b**: Deleting the fork's session so its edits are discarded
   - **c**: Forking again from the original, so the copy starts clean
   - **d**: Checkpointing, which snapshots the edits and reverts them

3. Scenario S4, developer productivity with Claude. The agent is built on the Claude Agent SDK and helps engineers explore unfamiliar codebases and understand legacy systems. A service returns to saved sessions after nightly code changes. It records a digest for each analysed file and sees that 5 of 12 changed. What should it do?
   - **a**: Fork the session, so that the stale results stay in the original
   - **b**: Start a new session that carries the earlier transcript as context
   - **c**: Continue the most recent session in the directory without a note
   - **d**: Resume the session, with a first prompt that lists the five paths

<details>
<summary>Answer key</summary>

1. **c**. The prompt cache expires after a long pause, so the next request reads the history once more. *b* is ruled out because "The session's prompt cache has expired by then, so the next request processes the full history once no matter which of the dialog's options you pick." *a* is ruled out because a resume restores the conversation and does not run tools again: a call that was cut off "doesn't finish or run again when you resume". *d* is ruled out because forking is an option you set, and a fork "creates a new session that starts with a copy of the original's history", not an automatic step of resuming.
2. **d**. Only a snapshot of the files can return them to an earlier state. *a* is ruled out because "Sessions persist the conversation, not the filesystem", so a resume restores no files. *b* is ruled out because "If a forked agent edits files, those changes are real and visible to any session working in the same directory", and removing the session does not undo them. *c* is ruled out because forking "branches the conversation history, not the filesystem", so the second copy meets the same edited files.
3. **d**. Five of twelve is under half, so a resume with a notice naming the paths keeps what is still true. *b* is ruled out because a transcript carries the stale results with it; a summary carries conclusions "never the raw tool output that went stale". *c* is ruled out because continue takes the most recent session, and with no note the session believes the old files: "Sessions persist the conversation, not the filesystem". *a* is ruled out because a fork "creates a new session that starts with a copy of the original's history", stale results included.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
