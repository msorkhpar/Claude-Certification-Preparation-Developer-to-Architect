# Examples, tests, the interview and feedback

**Level:** Architect · **Module 59:** Plan mode and iterative refinement · **Page 2 of 2**
**Exams:** A3.5; S2

**After this page you can** make a vague request precise with input and output examples, drive an implementation with tests that Claude runs, use the interview pattern for a large feature with unclear requirements, send several problems in the right number of messages, and recognise when to clear the session instead of correcting again.

Checked on 2026-10-03 against the Claude Code documentation page "Best practices for Claude Code" (verification, specific prompts, the interview, course correction), documenting behaviour up to Claude Code v2.1.286. Nothing here was run against a live session: the example is a model of the decisions, written as plain code over task and test data. This page deepens module 24 (prompt engineering for applications) and module 38 (Claude Code for developers) and does not repeat them. The choice between planning and direct execution is the first page.

> **Exam guide and current product.** *What the guide states (task statement 3.5), and so what the exam keys:* for iterative refinement it names concrete input and output examples when prose is interpreted inconsistently, test-driven iteration (write the tests first and share the failures to guide the next step), the interview pattern (Claude asks questions to surface considerations before implementing), and one message for interacting problems against sequential messages for independent ones. *What the current product does (documentation checked 2026-10-03):* all of it, with sharper wording. The check is "anything that returns a signal Claude can read in the conversation": a test suite, a build exit code, a linter, a script that diffs output against a fixture. For large features, "have Claude interview you first" with the `AskUserQuestion` tool, write a spec, and "start a fresh session to execute it". And after two failed corrections, "`/clear` and write a better initial prompt incorporating what you learned". On the exam, a question about inconsistent results answers with examples or tests, and a question about a large unclear feature answers with the interview.

## Why it matters

A developer asks for "a function that cleans up phone numbers". Claude writes one; the developer finds it keeps the plus sign; the next version drops it; a third strips the area code. Each round is a correction, and each correction is a guess at what was meant. The cure is to show what was meant: three examples with the exact input and the exact output. Scenario S2 asks, in several guises, which message to send next, and the answer is nearly always the one that removes a guess.

## The idea

### Show the transformation

When a description is interpreted in different ways, give "concrete input and output examples". "Normalise phone numbers" can mean ten things; `"(555) 010-2030"` becomes `"+15550102030"`, `"555.010.2030"` becomes `"+15550102030"` and `"12"` is refused is one thing. Two or three examples, including one that is an edge case, are enough. Examples are the first step because they cost a few lines and settle the question; they are what module 61 does for the prompt of a reviewer, and the same logic.

### Let the tests lead

Test-driven iteration closes the loop: "Claude does the work, runs the check, reads the result, and iterates until the check passes." The developer's part is to write, or have Claude write, the tests first and to give the target as the failing tests. A failure report is most useful when it names each failing test with its input, the expected output and the actual one, and says nothing about the tests that pass; that is the message the example builds. The same documentation lists stronger gates for unattended work (a goal condition, a Stop hook), and for a second opinion a separate subagent that tries to refute the result, so that the agent doing the work is not the one grading it.

A test written from the same misunderstanding as the code verifies nothing, which is why the first test cases come from a person or from the examples, and not from the code under test.

### The interview for a large feature

For a feature whose requirements are unclear, do not start with a long prompt that guesses. Start with a minimal description and ask Claude to interview you with `AskUserQuestion`: technical implementation, edge cases, concerns, tradeoffs, "Don't ask obvious questions, dig into the hard parts I might not have considered", and finally write the result to a spec file. The documentation then advises a fresh session for the implementation, so that the context holds the spec and not the whole conversation. A good spec names the files and interfaces, says what is out of scope and ends with an end-to-end check.

The interview is for a person at the keyboard. An unattended run cannot answer questions, so it states its assumptions in the output instead, a distinction that returns in module 61.

### One message or several

A list of problems can be sent two ways, and the rule is about interaction. Problems that interact, such as a sort order that breaks pagination, are fixed together in one message, because fixing one in isolation can undo the other. Independent problems, such as a typo in a label and a null date, go one after another, because each gets a clean result and a clean test run. The example groups issues by their `interacts_with` links and returns the messages in order.

### When to clear

Corrections accumulate in the context. The documentation's rule: "If you've corrected Claude more than twice on the same issue in one session, the context is cluttered with failed approaches." Run `/clear` and start again with a prompt that holds what you learned, such as the examples and the failing tests. Another tool for a wrong turn is `/rewind` (or pressing Escape twice), which restores the conversation, the code or both to an earlier checkpoint.

### The example

<!-- example: m59-refinement tabs: python,typescript -->
```python
"""Three decisions of a Claude Code session on a code-generation task: plan mode or direct execution, one message or several for a list of problems, and what a failing test run must say.

The rules are the ones the exam guide states for tasks 3.4 and 3.5 and the best-practices page confirms (read on 2026-10-03): plan when the change is large, architectural,
touches many files or has more than one valid approach; execute directly when you could describe the diff in one sentence; send interacting problems in one message and
independent problems one after another; and give the model the failing tests, with input and expected output, as the target. No model is called.
"""
```
<!-- /example -->

This module has no practice. Its decisions are judged by the quiz, since the choice of the next message is the whole skill.

## Traps

1. **"Describe the transformation more carefully in prose."** It is tempting because it is the least effort. The exam rejects it: when prose is interpreted inconsistently, the guide's remedy is concrete input and output examples.
2. **"Write all the tests after the implementation, then ask for a review."** It is tempting because the code exists to test. The exam rejects it for iteration: the tests lead, and their failures are the target Claude iterates against.
3. **"Send one message with every problem in the list."** It is tempting because it saves round trips. The exam rejects it for independent problems, which go one at a time; only problems that interact travel together.
4. **"Keep correcting; the context already holds what Claude got wrong."** It is tempting because the history looks like useful learning. The documentation rejects it: after two failed corrections the context is cluttered with failed approaches, and a clean session with a better prompt does better.

## Quiz

1. A conversion routine keeps being read in different ways, and each new version differs from the last. What should the next message contain?
   - **a**: A longer prose description with more adjectives
   - **b**: The same description plus a request to be more careful
   - **c**: Two or three exact inputs paired with the outputs required
   - **d**: A request to reproduce the previous version with small changes

2. The requirements of a large feature are unclear, and the developer is present at the keyboard. How should the work start?
   - **a**: Let Claude question them, then record a spec for a fresh session
   - **b**: Write one long prompt that covers every possibility
   - **c**: Have Claude pick the likeliest design and build it at once
   - **d**: Ask for the implementation, then repair whatever was misunderstood

<details>
<summary>Answer key</summary>

1. **c**. Exact examples remove the ambiguity that causes inconsistent reading. *a* is ruled out because the guide's remedy for prose that is read in different ways is "concrete input and output examples". *b* is ruled out because care does not say what is wanted, and "each correction is a guess at what was meant". *d* is ruled out because small changes to a version that was misread repeat the misreading, while "the answer is nearly always the one that removes a guess".
2. **a**. The interview surfaces the requirements before any code, and the spec carries them into a clean session. *b* is ruled out because "do not start with a long prompt that guesses" at what the requirements are. *c* is ruled out because choosing alone skips the documented step: "ask Claude to interview you with AskUserQuestion". *d* is ruled out because repair means repeated correction, and "after two failed corrections the context is cluttered with failed approaches".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. A scheduled job starts Claude Code with `-p` and `--permission-mode plan`. What happens when Claude tries to edit a source file?
   - **a**: It goes ahead, because a headless run skips permission modes
   - **b**: It is refused, since plan restrictions hold without a terminal too
   - **c**: It goes ahead, because a scheduled run counts as pre-approved
   - **d**: It prompts the job's owner by email for approval

2. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. After a refactor, one report lists a sort order that also breaks pagination, a misspelled label and a null date. How should the three be sent?
   - **a**: All three in one message, so that the context is shared
   - **b**: Three separate messages, one for each problem
   - **c**: The label first, then everything else in a second message
   - **d**: The linked pair together, the other two one after another

3. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. Claude has failed to fix the same bug in three attempts, and each correction added more history to the session. What should the developer do?
   - **a**: Send a fourth correction that repeats the earlier ones more and more forcefully
   - **b**: Ask Claude to ignore the earlier attempts and carry on in the same thread
   - **c**: Clear everything and restart with a prompt carrying the broken checks and the lessons
   - **d**: Switch to plan mode in the same thread and repeat the request

<details>
<summary>Answer key</summary>

1. **b**. Plan mode keeps its blocks in runs that have no interactive terminal. *a* is ruled out because "Plan mode keeps its blocks wherever Claude Code runs without an interactive terminal". *c* is ruled out because the exception is narrow: "in an interactive terminal session where bypass permissions are available the blocks are not enforced". *d* is ruled out because "Edits stay blocked until you approve the plan", and a headless run has no approval channel.
2. **d**. Problems that interact travel together and independent ones go one at a time. *a* is ruled out because "Independent problems, such as a typo in a label and a null date, go one after another". *b* is ruled out because the sort order and pagination interact, and "fixing one in isolation can undo the other". *c* is ruled out because that groups the independent date with the interacting pair, while the example "groups issues by their interacts_with links".
3. **c**. After more than two failed corrections the documented fix is to clear the context and write a better first prompt. *a* is ruled out because "after two failed corrections the context is cluttered with failed approaches", and repeating adds to the clutter. *b* is ruled out because an instruction does not remove history from the context, and "a clean session with a better prompt does better". *d* is ruled out because "Corrections accumulate in the context", and a mode switch in the same thread keeps them all.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
