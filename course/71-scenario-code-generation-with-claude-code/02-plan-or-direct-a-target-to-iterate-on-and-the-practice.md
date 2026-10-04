# Plan or direct, a target to iterate on, and the practice

**Level:** Architect · **Module 71:** Scenario: code generation with Claude Code · **Page 2 of 2**
**Exams:** A3; S2

**After this page you can** choose between plan mode and direct execution for a task and give the reason, give a task a check that Claude can run, use the interview for a large feature, decide when to clear a session and start again, and write the module's practice: a complete setup for a small project, graded by tests.

Checked on 2026-10-04 against the Claude Code documentation pages "Choose a permission mode" and "Best practices for Claude Code", and the Architect exam guide (version 1.0, scenario 2 and its sample questions). The practice is offline in Python, TypeScript, Java and Kotlin: the tests read the files you write, and nothing starts Claude Code. The practice's checks are this course's own, built on the documented behaviour, and the statement says so.

## Why it matters

Page 1 placed the team's instructions. The other half of the scenario is how a developer works with Claude Code inside that setup: whether to let it plan, what to give it to aim at, and when a session has become more of a problem than a help. The exam asks these as judgement questions with a task in the stem. A restructuring across dozens of files. A rename in one function. A feature nobody has specified. The right answer is a mode and a reason, and the wrong answers are the mode that would be right for a different task.

## The idea

### Plan mode, and when it earns its cost

The permission-modes page describes plan mode in one sentence: "Plan mode tells Claude to research and propose changes without making them." Claude reads files, can run commands to explore, writes a plan, and does not edit the source until a plan is approved. The best-practices page names the cost and the rule for paying it: plan mode "adds overhead", it is most useful "when you're uncertain about the approach, when the change modifies multiple files, or when you're unfamiliar with the code being modified", and "If you could describe the diff in one sentence, skip the plan."

The test the course teaches follows from those lines. Ask two questions about the task: is the design still open, and is the change spread? If either answer is yes, plan first. If the scope is known and the diff is small, edit directly. The exam's sample for this scenario is a restructuring of a monolith into services, with changes across dozens of files and decisions about boundaries. It is plan mode, for three reasons that the stem hands you: many files, several valid designs, and decisions about what depends on what.

Two wrong answers are worth naming because they sound like caution. Starting directly and "letting the implementation reveal the boundaries" risks rework when a dependency turns up late. Starting directly and switching to plan mode only if complexity appears ignores that the complexity is in the stem already.

### Give the task a target

The best-practices page puts the idea first among its recommendations: "Give Claude a check it can run: tests, a build, a screenshot to compare." Its reason is that "Claude stops when the work looks done", and without a check "'looks done' is the only signal available". For code generation the check is usually one of three things.

- **Tests that exist before the code.** A failing test that describes the behaviour is a target. Claude writes the code, runs the tests and iterates until they pass.
- **Examples of input and output.** Two or three cases with their expected results say what a paragraph of description leaves open. The page's example is a validation function with named test cases: one valid address, one invalid, and one edge.
- **A criterion that can be run:** a build exit code, a linter, a script that compares output with a fixture.

Where a check must gate the end of the work, the page names stronger forms than a request in the prompt, a Stop hook among them, which "runs your check as a script and blocks the turn from ending until it passes". That is the same step from request to code that the whole course repeats.

### The interview, for what nobody has specified

For a large feature with unclear requirements the page recommends reversing the roles: "have Claude interview you" with the `AskUserQuestion` tool, about "technical implementation, UI/UX, edge cases, concerns, and tradeoffs", and write the result to a spec file. Then "start a fresh session to execute it", so that the new session has a clean context "focused entirely on implementation". This is the case for a plan that starts with questions: the missing piece is not a design that Claude could find in the code, it is a decision only the people can make.

### When to clear

Context fills, and the page's first sentence about practice is that "performance degrades as it fills". Two rules follow. Between unrelated tasks, clear the session. And after two failed corrections of the same mistake, clear it and write a better first prompt, because the context is now full of failed approaches: "A clean session with a better prompt almost always outperforms a long session with accumulated corrections."

### The setup and the working habits together

The scenario's pieces fit as follows. The root file holds what every task needs, including a line that points to the team's working modes. Rules carry the area conventions and load by path. The review command is shared and read-only, so a developer can run it on any change. The permission rule keeps `.env` unreadable. And the working-modes table settles the plan-or-direct question for the common kinds of task, so that nobody decides it afresh each time. The practice asks you to write all of these.

### The practice: a complete setup for a small project

The practice is `exercises/71-scenario-code-generation-with-claude-code/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. The starter is a project whose single root file holds every convention, a review checklist and a personal note, and whose settings allow the whole Bash tool. You write the files that fix it: a short root file; four rule files, each scoped by glob to the files of its area (components, handlers, database access, and test files wherever they sit); a shared review command with a description and read-only tools; settings that deny reading `.env` and approve no whole tool; and a table that sends six described tasks to plan mode or to direct execution.

The tests grade seven cases: each convention loads for exactly the files of its area (read through the globs against sample files), the root file is short and holds none of the area conventions, the review command is shared and read-only, the settings protect the environment file, the modes table sends open design work to plan mode and clear small work to direct, every rule has a glob that matches a file, and no file holds a personal path, an address or a key. The starter fails all seven. The reference passes them. Each of fourteen planted wrong solutions per language fails on an assertion of the case it breaks: a rule with no paths, a rule scoped too widely, test rules scoped to one folder or to one extension, a bare folder name as a path, a convention left in the root file, a root file padded past the limit, a command that approves the whole Bash tool or has no description, an environment file left readable, a whole-tool allow, the monolith sent to direct execution, a typo fix sent through plan mode, and a personal path.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"Use direct execution for the restructuring, with detailed upfront instructions."** It is tempting because instructions feel like a plan. The exam rejects it: the instructions would assume the structure, and the structure is what has to be found by exploring.
2. **"Always start in plan mode."** It is tempting because planning sounds careful. The exam rejects it for small, clear changes: the mode adds overhead, and a one-sentence diff does not need a plan.
3. **"Keep correcting in the same long session."** It is tempting because the history looks useful. The exam rejects it after two failed corrections: the history is now failed approaches, and a clean session with a better prompt does better.

## Quiz

4. A developer must turn a monolith into services: dozens of files change and the service boundaries are undecided. Which approach fits?
   - **a**: Begin with direct edits, and let the boundaries show as work proceeds
   - **b**: Explore in plan mode first, and settle the design before any edit
   - **c**: Write complete instructions for each service, then execute them directly
   - **d**: Execute directly, and switch to plan mode only if the work turns complicated

5. A developer asks for a function that validates e-mail addresses and has no way yet to tell whether the first version is right. What should be added to the request?
   - **a**: A longer description of what a valid address looks like, in prose
   - **b**: A request to explain the function after writing it, line by line
   - **c**: A reminder to be careful and to check the work before it finishes
   - **d**: A few input and expected-result pairs, with an instruction to run them

6. A developer has corrected the same mistake three times in one long session, and the fourth attempt is wrong again. What is the next step?
   - **a**: Add a fourth correction with the same wording but in capital letters
   - **b**: Clear the conversation, then restart with a prompt that holds what was learned
   - **c**: Keep the session, since the earlier history helps Claude see the pattern
   - **d**: Switch to plan mode for the rest of the session so that nothing is edited at all

<details>
<summary>Answer key</summary>

4. **b**. Many files, open boundaries and design decisions point to planning first. *a* is ruled out because the boundaries are what to find first: "risks rework when a dependency turns up late". *c* is ruled out because instructions would assume the structure: "the instructions would assume the structure, and the structure is what has to be found by exploring". *d* is ruled out because the stem already holds the complexity: "ignores that the complexity is in the stem already".
5. **d**. A pair of input and expected result is a check that Claude can run. *a* is ruled out because prose leaves cases open: "Two or three cases with their expected results say what a paragraph of description leaves open." *c* is ruled out because a reminder is not a signal: "'looks done' is the only signal available". *b* is ruled out because an explanation checks nothing: "Give Claude a check it can run: tests, a build, a screenshot to compare."
6. **b**. After two failed corrections the context holds failed approaches, so a clean start with a better prompt works better. *a* is ruled out because emphasis does not clear the context: "the context is now full of failed approaches". *c* is ruled out because the history is the problem: "the history is now failed approaches, and a clean session with a better prompt does better". *d* is ruled out because the mode does not remove the clutter: "A clean session with a better prompt almost always outperforms a long session with accumulated corrections."

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S2, a team using Claude Code on a shared codebase. A rule file for the handlers lists `paths: src/api` and never seems to apply when handlers are edited. What is the cause?
   - **a**: Rule files are read only when a session is first started
   - **b**: A bare folder name is no glob, so nothing is matched
   - **c**: Handlers are covered by the command folder, which takes priority
   - **d**: The path needs the word handlers in it for the rule to be found

2. Scenario S2, a team using Claude Code on a shared codebase. A developer's habit of replying in short sentences is added to the shared root memory file, and colleagues object. Where should it go?
   - **a**: In a personal instructions document kept out of version control
   - **b**: In the review command, which each person runs in their own way
   - **c**: In a rule scoped by glob to the developer's own commits
   - **d**: In the settings as a permission rule that the others can override

3. Scenario S2, a team using Claude Code on a shared codebase. The team wants to be sure that no session ever reads the environment file, whatever the model decides. Which control gives that?
   - **a**: A deny rule in the permission settings, or a hook that blocks the call
   - **b**: A scoped instruction that repeats the prohibition for that file
   - **c**: An entry in the modes table that sends such requests to plan mode
   - **d**: A line in the root memory that forbids reading the environment file

4. Scenario S2, a team using Claude Code on a shared codebase. A large feature has unclear requirements, and the developer is unsure what to ask for. Which start fits?
   - **a**: Ask for the most likely reading to be built, then fix it after a careful review
   - **b**: Ask for three complete versions, and choose the one that looks best
   - **c**: Have the assistant interview the user, write a spec, run it in a clean session
   - **d**: Ask for the tests to be written last, so that they match what was built

<details>
<summary>Answer key</summary>

1. **b**. A bare folder name is not a glob. *a* is ruled out because path-scoped rules load when a matching file is read: "A rule with a `paths` list loads when Claude works with a file that matches one of its globs". *c* is ruled out because the command folder holds commands and not conventions: "A command file in the project, `.claude/commands/review.md`, committed to the repository". *d* is ruled out because the match is by glob on file paths and not by a word: "`**` crosses folders and `*` stays inside one".
2. **a**. A preference of one person belongs in a personal file. *c* is ruled out because a glob cannot select one developer's commits: "A preference of one developer". *b* is ruled out because the command is shared by all: "A command file in the project, `.claude/commands/review.md`, committed to the repository". *d* is ruled out because permission rules are for what Claude may do and not for style: "A permission rule or a hook".
3. **a**. Code that runs holds whatever the model decides. *d* is ruled out because a sentence is a request: "treats them as context, not enforced configuration". *b* is ruled out because a rule is the same kind of text: "To block an action regardless of what Claude decides, use a PreToolUse hook instead." *c* is ruled out because the table advises on mode and enforces nothing: "Plan mode tells Claude to research and propose changes without making them."
4. **c**. The missing piece is a decision only the people can make, so the questions come first. *a* is ruled out because a guess builds on the wrong reading: "the missing piece is not a design that Claude could find in the code, it is a decision only the people can make". *b* is ruled out because three versions do not settle what is wanted: "Claude stops when the work looks done". *d* is ruled out because tests that follow the code are no target: "A failing test that describes the behaviour is a target."

</details>

Adapted from the sample questions of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the questions are Anthropic's. The first question of this page follows the guide's sample question on restructuring a monolith, rewritten here.
