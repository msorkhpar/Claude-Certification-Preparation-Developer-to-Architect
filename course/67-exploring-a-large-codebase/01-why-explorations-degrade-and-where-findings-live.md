# Why long explorations degrade, and where their findings should live

**Level:** Architect · **Module 67:** Exploring a large codebase · **Page 1 of 2**
**Exams:** A5.4; S4

**After this page you can** recognise the signs of a context that has been filled by exploration, keep findings in a scratchpad file that outlives the context window, delegate a narrow question to a subagent so that its reading never enters the main context, use the built-in Explore subagent for read-only questions about code, and carry the summary of one phase into the next.

Checked on 2026-10-04 against the exam guide's task statement 5.4 and scenario S4, the Claude Code documentation pages "Best practices for Claude Code" (the context window, subagents for investigation, compaction), "Create custom subagents" (the built-in Explore subagent) and the Claude Agent SDK page on subagents. Nothing here called a model: the example is a crash and its recovery over an in-memory file system (`examples/67-state-manifest`, which page 2 uses), and the practice is graded by test suites. This page and the next one deepen module 56, where the built-in tools were chosen for exploration, and module 51, where a saved session was resumed. They do not repeat them.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* in extended sessions the context degrades, "models start giving inconsistent answers and referencing 'typical patterns' rather than specific classes discovered earlier"; the remedies are scratchpad files that persist key findings across context boundaries, subagents that investigate specific questions ("find all test files", "trace refund flow dependencies") while the main agent keeps the high-level picture, summaries of one exploration phase injected into the next phase's subagents, and `/compact` when the context fills with verbose discovery output. *What the product documents now (read 2026-10-04):* "Claude's context window fills up fast, and performance degrades as it fills", and a codebase exploration "might generate and consume tens of thousands of tokens"; subagents "run in separate context windows and report back summaries"; the built-in Explore subagent is "a fast, read-only agent optimized for searching and analyzing codebases" whose Write and Edit tools are denied and which returns only a summary. The documentation states the cause and the mechanisms. The specific symptom (generic "typical patterns") is the guide's description, and the exam keys it. On the exam, a session that has started to answer generally has the answer: write findings down, delegate the reading, compact.

## Why it matters

An agent spends two hours mapping a payments service. Early on it learned exactly where retries happen, which class owns idempotency keys and what the refund flow touches. Two hours later a developer asks about retry handling and the agent replies with a fluent paragraph about how payment systems typically retry. Nothing was deleted: the specific classes are somewhere in a window that is now full of file contents, and the model is answering from the general pattern. The scenario (S4) is a developer-productivity agent, and it tests whether the design keeps the specifics somewhere that does not decay.

## The idea

### What a filled context looks like

Exploration is the most context-hungry thing an agent does. Each file read, each search result, each command output stays in the window. As it fills, performance degrades: instructions are forgotten earlier than they should be, answers become inconsistent from one turn to the next, and specific names give way to plausible generalities. The signal on the exam is the last one: the agent talks about "typical patterns" where it earlier found a specific class. A reminder in the prompt does not bring back a class that has left the window, and a bigger window only moves the problem later.

### A scratchpad outlives the window

The first remedy is a file. As the agent finds something worth keeping (the file that owns a concept, a confirmed assumption, a risk, the next thing to check) it appends it to a scratchpad: a plain file in the repository's working area, in a form that a later turn can reread. Later questions begin by reading the scratchpad, so the answer comes from what was recorded and not from what is left in the window. Three properties make a scratchpad work: findings are specific (a path and a line, not "the auth code"), they are recorded once (a fact written twice is noise), and they are grouped (by area, so that a question about billing reads the billing section). The practice's `add_finding` and `render_scratchpad` are those three properties.

The scratchpad is the agent's own working memory. It is not a replacement for the project's instructions file, which holds what is true of the project for every session; and it is not a transcript, which is the thing that filled the window in the first place.

### Delegate the reading

The second remedy keeps the reading out of the main context altogether. The coordinator hands a narrow question to a subagent: "find all test files that cover refunds", "trace the refund flow and list its dependencies". The subagent starts with a fresh window, does the reading, and returns a summary; "intermediate tool calls and results stay inside the subagent; only its final message returns to the parent". The coordinator's context grows by a paragraph and not by a hundred files, and it keeps the high-level picture.

Two conditions make the delegation work. The question must be specific: a subagent told to "explore the codebase" returns a long account and the coordinator has gained nothing. And the brief must carry what the subagent needs, because it does not inherit the conversation: it receives its own prompt and project instructions, and not "the parent's conversation history or tool results" or the files already read (module 47).

### The built-in Explore subagent

Claude Code ships an Explore subagent for exactly this work: a read-only agent for file discovery, code search and codebase exploration, with Write and Edit denied, which returns only a summary to the main conversation. The caller can ask for a thoroughness (quick, medium or very thorough). A project can override its model by defining its own subagent named Explore, for example on a cheaper model. Read-only matters: an exploration that can edit can also damage, and the permissions from module 56 apply to it as to any subagent. The built-in Explore agent is one-shot (it cannot be resumed), which suits a question with a self-contained answer.

### Summarise a phase, then inject it

A large exploration has phases: map the modules, then trace the flows that matter, then check the risks. The findings of one phase are the starting point of the next. The coordinator summarises the first phase's findings (from the scratchpad) and puts that summary into the initial prompts of the second phase's subagents. They begin knowing what is known, they do not rediscover it, and they do not receive the first phase's transcripts.

### The example

The example on this module's second page follows the exploration through a crash. It is introduced there with the manifest.

## Traps

1. **"Remind the agent in the prompt to stay specific."** It is tempting because it is one sentence. The exam rejects it: the specific findings have left the window, and a reminder cannot recall them. Write findings to a scratchpad and reread it.
2. **"Switch to a model with a bigger window so that the exploration fits."** It is tempting because the limit is the symptom. The exam rejects it: performance degrades as the context fills, so a larger window only moves the same decline further out. Delegate the reading.
3. **"Ask a subagent to 'explore the codebase' and report everything."** It is tempting because delegation sounds like the fix. The exam rejects it: a broad brief returns a broad report. Delegate a specific question, such as finding the test files or tracing one flow.
4. **"Start phase two from scratch; the subagents will find it again."** It is tempting because it needs no plumbing. The exam rejects it: the phase-one summary is injected into the next subagents' prompts so that the work is not repeated.

## Quiz

1. Scenario S4, developer productivity with Claude. After two hours on a payments codebase, the agent answers a question about retries by describing how such systems typically retry, though it read the actual retry classes an hour earlier. Which practice would have prevented this?
   - **a**: Writing specific findings to a notes file and consulting it for later queries
   - **b**: Switching to a model with a bigger context window for the whole of the session
   - **c**: Raising the output limit so that each answer can quote more of the code
   - **d**: Telling the agent in its prompt to avoid generic answers from here onward

2. Scenario S4, developer productivity with Claude. A coordinator must list every test file that covers refunds in a very large repository, then trace the refund flow. It does both in its own conversation, and the context fills with listings and file contents. Which structure fits?
   - **a**: Hand the whole job to one helper with the brief to explore everything and report it all
   - **b**: Keep the reading in the coordinator and add a notes file for what it finds
   - **c**: Give each question to a read-only exploring helper and keep just the summaries
   - **d**: Use a helper with every tool, so that it can save the lists to files

<details>
<summary>Answer key</summary>

1. **a**. The specifics are kept in a file that outlives the window and are read again when a later question needs them. *d* is ruled out because "A reminder in the prompt does not bring back a class that has left the window". *b* is ruled out because "a bigger window only moves the problem later". *c* is ruled out because the symptom is that "answers become inconsistent from one turn to the next, and specific names give way to plausible generalities", which more output does not repair.
2. **c**. Each narrow question goes to a read-only subagent, and the coordinator receives only a summary. *a* is ruled out because "a broad brief returns a broad report". *b* is ruled out because "Each file read, each search result, each command output stays in the window", so the coordinator's own reading keeps filling it. *d* is ruled out because "Read-only matters: an exploration that can edit can also damage".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
