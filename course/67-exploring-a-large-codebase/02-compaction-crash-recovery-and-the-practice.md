# Compaction, crash recovery and the practice

**Level:** Architect · **Module 67:** Exploring a large codebase · **Page 2 of 2**
**Exams:** A5.4; S4

**After this page you can** steer `/compact` and automatic compaction so that what an exploration needs survives them, choose between compacting and clearing, design the state export and the manifest that let a coordinator recover after a crash, read a manifest against the files that exist and decide for each agent whether to reuse, resume or restart it, build the prompt that continues an agent from its exported state, and write the module's practice.

Checked on 2026-10-04 against the exam guide's task statement 5.4 and scenario S4, the Claude Code documentation pages "Best practices for Claude Code" (compaction, `/clear`, summarising from a checkpoint) and "Manage costs effectively" (compaction instructions in `CLAUDE.md`), and the Claude Agent SDK page on subagents (resuming a subagent). Nothing here called a model: the example is a crash and a recovery over an in-memory file system, and the practice is graded by Python, TypeScript, Java and Kotlin test suites, offline. The first page covered why explorations degrade and where findings go; this page covers what survives compaction and what survives a crash.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* use `/compact` to reduce context usage "during extended exploration sessions when context fills with verbose discovery output"; for crash recovery, "each agent exports state to a known location, and the coordinator loads a manifest on resume", injecting the exported state into the agents' prompts. *What the product documents now (read 2026-10-04):* Claude Code "automatically compacts conversation history when you approach context limits"; `/compact` accepts instructions ("`/compact Focus on the API changes`"); a project instructions file can steer compaction with a sentence such as "When compacting, always preserve the full list of modified files and any test commands"; Esc then Esc (or `/rewind`) opens a checkpoint from which to summarise either forwards or backwards; `/clear` "resets the context window entirely"; and a custom subagent can be resumed with its full history, while the built-in `Explore` and `Plan` agents are one-shot. The product has compaction and subagent resumption. It has no coordinator manifest: the manifest, the state files and the decision on resume are yours. On the exam, a question about recovery after a crash has the answer: state exported to a known place, a manifest, and a coordinator that reads the manifest and injects the state.

## Why it matters

Six agents explore six modules of a large service for most of an afternoon. The machine restarts. Without a design, the coordinator has nothing: six transcripts that were in memory, a conversation that no longer exists, and an afternoon to repeat. With a design, each agent wrote what it learned to a file as it went, the coordinator wrote one line per agent into a manifest, and on restart it reads that file, sees that four agents finished, one was half-way and one had not started, and runs only the work that is missing, handing the survivors' findings to the agents that continue. The same session may also hit the window limit long before any crash, and then the question is what compaction keeps.

## The idea

### Steer compaction

Compaction replaces the conversation with a summary. Left to itself the summary keeps what looks important, which in an exploration is rarely the list of files already read, the commands that run the tests, or the open questions. Three controls exist:

- run `/compact` with instructions: `/compact Focus on the list of files read and the open questions`;
- put standing instructions in the project's instructions file ("When compacting, always preserve the full list of modified files and any test commands"), which also steers the automatic compaction that happens near the limit;
- summarise only part of the conversation, from a checkpoint, when the early context is the part to keep.

Turning compaction off is not a remedy: the window then fills, and performance degrades. Compaction is not the same as `/clear`. Compaction continues the same work with a shorter memory of it. `/clear` starts over and is right between unrelated tasks, and wrong in the middle of an exploration whose findings are only in the window. The reliable version of "keep this" is the scratchpad of page 1, since a summary is a lossy copy and a file is not. The practice's `compact_command` builds the command with its focus.

### State export and the manifest

A crash does not warn. The design that survives one has two parts written as the work goes.

**Each agent exports its state to a known location**, a file per agent whose path is fixed by the design (`state/<agent>.md`), holding its findings in the scratchpad form of page 1: specific, once, grouped. It is written as the agent learns and not only at the end, so that a crash half-way leaves something.

**The coordinator keeps a manifest**: one entry per agent with its name, its state file and its status (`running`, `done`, `failed`). It writes an agent's entry when the agent starts, so that a crash leaves a trace of work that began and has nothing to show. The manifest is small, readable by a person, and the one thing the coordinator reads first.

### Reading the manifest on resume

On restart the coordinator loads the manifest and compares it with the files that exist. The rule of the practice has three branches and one principle:

| State file | Status | Action |
|---|---|---|
| Missing | any | `restart`: there is nothing to continue from, whatever the manifest says |
| Exists | `done` | `reuse`: do not run it again; its findings go into the next prompts |
| Exists | `running` or `failed` | `resume`: continue from the exported state |

The principle is that the file outranks the status. A status is a claim written at one moment; a state file is evidence. A manifest that says `done` for an agent whose file is missing was written by a crash between two steps, and trusting it loses that agent's work without anyone noticing. The same discipline protects against a manifest that says `running` for an agent that finished and wrote its file just before the crash: the file is there, so the work is kept.

A resumed or restarted agent gets a prompt that carries its task, the state lines it exported, and one instruction: continue from the first unfinished step. It does not get the old transcript, which is what filled the first window. The practice's `resume_prompt` is that shape. When the agent is a custom subagent that was resumable in the same session, resuming it with its own history is an alternative, but a crash that ended the session leaves only the files.

### The example

<!-- example: m67-state-manifest tabs: python,typescript,java,kotlin -->
<!-- /example -->

### The practice: an exploration that can be resumed

The practice is in [`exercises/67-exploring-a-large-codebase`](../../exercises/67-exploring-a-large-codebase/unit-01/practice-1/statement.md). You write the scratchpad (findings recorded once, grouped by area), the manifest, the plan for resuming, the prompt that continues an agent and the compaction command. It is graded in Python, TypeScript, Java and Kotlin; the statement lists eight cases, each saying what you should see when it works. The outline marks this module as configuration, and the practice treats the manifest and the compaction instructions as the configuration of an exploration.

## Traps

1. **"Compact the session; the summary will keep what matters."** It is tempting because compaction is automatic. The exam rejects it: a summary keeps what looks important, so tell it what to keep, and keep the findings in a file.
2. **"Clear the context when it fills; the agent will rediscover everything."** It is tempting because it is the cheapest reset. The exam rejects it: clearing in the middle of an exploration throws away findings that exist only in the window.
3. **"The manifest says the agent is done; skip it."** It is tempting because the manifest is the record. The exam rejects it: a state file that does not exist outranks a status that says done, and the agent restarts.
4. **"After the crash, replay every agent's transcript into the new coordinator."** It is tempting because it restores everything. The exam rejects it: the transcripts are what filled the window. Load the manifest and inject the state, not the history.

## Quiz

1. Scenario S4, developer productivity with Claude. During a long exploration the team wants automatic summarising near the limit to keep the roster of sources already opened and the commands that run the tests. What should they do?
   - **a**: Turn automatic summarising off so that nothing is condensed at all
   - **b**: Add a sentence on what to preserve to the project's instructions file
   - **c**: Raise the output limit so that the summary can run longer than usual
   - **d**: Clear the conversation at each phase so that nothing needs preserving

2. Scenario S4, developer productivity with Claude. A coordinator restarts after a crash. Its manifest lists 6 agents as running, and notes written by only 4 of them can be found on disk. What should it do?
   - **a**: Restart the whole set, since a manifest written before a crash cannot be trusted
   - **b**: Resume each of the 6 on the manifest's word, giving empty notes to the 2 without any
   - **c**: Replay the 6 transcripts into the new coordinator before it continues
   - **d**: Carry on from the saved material where it exists and begin afresh where it does not

<details>
<summary>Answer key</summary>

1. **b**. A standing instruction in the project's file steers the automatic summary toward what the exploration needs. *a* is ruled out because "Turning compaction off is not a remedy: the window then fills, and performance degrades". *c* is ruled out because "Left to itself the summary keeps what looks important", and a longer summary still chooses by that. *d* is ruled out because clearing "starts over and is right between unrelated tasks", and a phase of the same exploration is not one.
2. **d**. The agents with files continue from them and the others start over, since the file outranks the status. *a* is ruled out because for an agent whose file survived, "the file is there, so the work is kept". *b* is ruled out because for a missing file there is "nothing to continue from, whatever the manifest says". *c* is ruled out because "the transcripts are what filled the window".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S4, developer productivity with Claude. A coordinator has finished mapping a service's modules and now launches subagents to trace three flows. How should the flow subagents learn what the mapping found?
   - **a**: Each of them repeats the mapping first, to be certain of the ground
   - **b**: Each of them receives the full mapping transcripts in its prompt
   - **c**: Their prompts carry a short summary of the findings from the notes
   - **d**: They inherit the coordinator's conversation without any further step

2. Scenario S4, developer productivity with Claude. A team needs a code-searching helper whose work can be continued later in the same session. Which fits?
   - **a**: The built-in Explore subagent, continued with its own history
   - **b**: A custom subagent, since the built-in Explore is a one-shot tool
   - **c**: A fresh Explore each time, handed a copy of the earlier transcript
   - **d**: A second coordinator that reads the first coordinator's conversation

3. Scenario S4, developer productivity with Claude. One agent is marked done in the manifest, yet no state file exists for it. How should it be handled?
   - **a**: Reuse it, since the manifest is the record of what finished
   - **b**: Resume it from whatever the manifest's last line says about it
   - **c**: Drop it from the plan and report its area as covered anyway
   - **d**: Run it again, as missing evidence outranks a written status

<details>
<summary>Answer key</summary>

1. **c**. The summary of the first phase goes into the next prompts, so nothing is rediscovered and no transcript is carried. *a* is ruled out because "they do not rediscover it". *b* is ruled out because "they do not receive the first phase's transcripts". *d* is ruled out because a subagent receives its own prompt and project instructions, and not "the parent's conversation history or tool results".
2. **b**. A custom subagent can be resumed with its history, and the built-in one cannot. *a* is ruled out because "The built-in Explore agent is one-shot". *c* is ruled out because "the transcripts are what filled the window". *d* is ruled out because "a crash that ended the session leaves only the files", and an ordinary session leaves a resumable subagent.
3. **d**. A status is a claim and a file is evidence, so the missing file decides. *a* is ruled out because "A status is a claim written at one moment; a state file is evidence". *b* is ruled out because for a missing file there is "nothing to continue from, whatever the manifest says". *c* is ruled out because trusting the status "loses that agent's work without anyone noticing".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
