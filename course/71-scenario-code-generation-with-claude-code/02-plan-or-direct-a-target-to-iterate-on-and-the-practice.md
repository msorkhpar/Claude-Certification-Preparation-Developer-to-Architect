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

The tests grade seven cases: each convention loads for exactly the files of its area (read through the globs against sample files), the root file is short and holds none of the area conventions, the review command is shared and read-only, the settings protect the environment file, the modes table sends open design work to plan mode and clear small work to direct, every rule has a glob that matches a file, and no file holds a personal path, an address or a key. The starter fails all seven. The reference passes them. Each of thirteen planted wrong solutions per language fails on an assertion of the case it breaks: a rule with no paths, a rule scoped too widely, test rules scoped to one folder or to one extension, a convention left in the root file, a root file padded past the limit, a command that approves the whole Bash tool or has no description, an environment file left readable, a whole-tool allow, the monolith sent to direct execution, a typo fix sent through plan mode, and a personal path.

### What the tests accept

The tests read your files with the rules below, so it pays to know them before you start. The review command passes when it has a `description`, an `allowed-tools` line that has `Read` and none of `Bash`, `Edit` or `Write` on its own, and a body that says to use `git diff`; `Bash(git diff *)` is allowed and a bare `Bash` is not. The settings pass when `Read(./.env)` is denied and no allow rule is `Bash`, `Bash(*)`, `Edit` or `Write`. The modes table passes when a typo fix, a validation function and a rename are `direct` and the monolith, the auth library and an unclear feature are `plan`. Every rule passes when its `paths` list has a star in each entry and at least one sample file matches.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"Use direct execution for the restructuring, with detailed upfront instructions."** It is tempting because instructions feel like a plan. The exam rejects it: the instructions would assume the structure, and the structure is what has to be found by exploring.
2. **"Always start in plan mode."** It is tempting because planning sounds careful. The exam rejects it for small, clear changes: the mode adds overhead, and a one-sentence diff does not need a plan.
3. **"Keep correcting in the same long session."** It is tempting because the history looks useful. The exam rejects it after two failed corrections: the history is now failed approaches, and a clean session with a better prompt does better.

## Quiz

4. Which allowed-tools line does the practice accept for the shared review command?
   - **a**: `Read Grep Glob Bash Edit Write`
   - **b**: `Read Grep Glob Bash(git diff *)`
   - **c**: `Read Edit Write Bash(git diff *)`
   - **d**: `Bash(git diff *) Grep Glob`

5. Which settings pass the practice's check on permissions?
   - **a**: Deny `Read(./.env)`, allow `Edit`
   - **b**: Deny `Read(./.env)`, allow `Bash`
   - **c**: Deny nothing, allow `Bash(npm test)`
   - **d**: Deny `Read(./.env)`, allow `Bash(npm test)`

6. Which pair of rows does the practice's modes table need?
   - **a**: A typo fix is plan and the monolith is direct
   - **b**: A typo fix is direct and the monolith is plan
   - **c**: A rename is plan and an unclear feature is direct
   - **d**: A validation function is plan and the auth library is direct

<details>
<summary>Answer key</summary>

4. **b**. A description, `Read`, and a pattern for `git diff`. *a* is ruled out because a bare shell and edit tools are refused: "`Read` and none of `Bash`, `Edit` or `Write` on its own". *c* is ruled out for its edit tools: "`Read` and none of `Bash`, `Edit` or `Write` on its own". *d* is ruled out because the line must hold the reading tool: "an `allowed-tools` line that has `Read`".
5. **d**. The environment file is denied and no allow rule approves a whole tool: "no allow rule is `Bash`, `Bash(*)`, `Edit` or `Write`". *b* is ruled out because a whole-tool allow is refused: "no allow rule is `Bash`, `Bash(*)`, `Edit` or `Write`". *c* is ruled out because the deny rule is required: "The settings pass when `Read(./.env)` is denied". *a* is ruled out because an edit allow without a path is a whole-tool allow as well: "no allow rule is `Bash`, `Bash(*)`, `Edit` or `Write`".
6. **b**. Small clear work is direct and open design is plan. *a* is ruled out because the table is the other way round: "a typo fix, a validation function and a rename are `direct` and the monolith, the auth library and an unclear feature are `plan`". *c* is ruled out by the same sentence: "a typo fix, a validation function and a rename are `direct` and the monolith, the auth library and an unclear feature are `plan`". *d* is ruled out by it too: "a typo fix, a validation function and a rename are `direct` and the monolith, the auth library and an unclear feature are `plan`".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S2, a team using Claude Code on a shared codebase. A rule file for the handlers lists `paths: src/api`, and the audit says that it matches none of the project's files. What is the likely cause?
   - **a**: Rule files are read only when a session is first started
   - **b**: The entry is a bare folder name and not a glob that ends in a double star
   - **c**: Handlers are covered by the command folder, which takes priority in every session
   - **d**: The path needs the word handlers in it for the rule to be found

2. Scenario S2, a team using Claude Code on a shared codebase. Three specs of the flawed project get no conventions at all. What closes the gap?
   - **a**: A testing rule whose glob, such as `**/*.test.tsx`, reaches test files in any folder
   - **b**: A longer root file that names each test file under a heading of its own
   - **c**: A skill for tests that each developer starts by hand before a commit
   - **d**: A memory file placed in the one folder that holds most of the tests

3. Scenario S2, a team using Claude Code on a shared codebase. In the example's fixed project, `docs/readme.md` is opened. Which conventions load for it?
   - **a**: Only the root file's, since no rule has a glob that matches it
   - **b**: The testing rule as well, since a readme is also a kind of test
   - **c**: The components rule, as the nearest rule by folder
   - **d**: None at all, since the root file loads only for source files

4. Scenario S2, a team using Claude Code on a shared codebase. The audit of the flawed project prints `env-readable`. What removes that finding?
   - **a**: A rule file whose glob matches `.env`
   - **b**: A line in the root file that asks Claude not to read `.env`
   - **c**: A deny rule for reading `.env` in the settings
   - **d**: A review command with a read-only list of tools

<details>
<summary>Answer key</summary>

1. **b**. A bare folder name is not a glob: "write a folder as `src/api/**`". *a* is ruled out because path-scoped rules load when a matching file is read: "A rule with a `paths` list loads when Claude works with a file that matches one of its globs". *c* is ruled out because the command folder holds commands and not conventions: "A command file in the project, `.claude/commands/review.md`, committed to the repository". *d* is ruled out because the match is by glob on file paths and not by a word: "`**` crosses folders and `*` stays inside one".
2. **a**. A rule with a glob reaches test files wherever they sit: "`**/*.test.tsx` reaches test files in any folder". *b* is ruled out because the root file leaves the choice to the model: "One root file with a section per area leaves Claude to infer which section applies". *c* is ruled out because a path starts no skill: "a skill is invoked, or chosen by the model from its description, and a path match does not start it". *d* is ruled out because a folder file does not reach the others: "each file is bound to its directory, and test files next to their sources are in all of them".
3. **a**. The output shows `docs/readme.md <- CLAUDE.md only`: no rule reaches it, and the root file loads in every session. *b* is ruled out because the testing rule has globs for test files: "`**/*.test.tsx` reaches test files in any folder". *c* is ruled out because a match is by glob and not by nearness: "`**` crosses folders and `*` stays inside one". *d* is ruled out because the root file is not conditional: "The root `CLAUDE.md` and the rule files without a `paths` list load at the start of every session".
4. **c**. The finding says that the settings do not deny the file: "`env-readable` and `bare-bash-allowed`: the settings do not deny reading `.env`, or allow the whole Bash tool." *b* is ruled out because a sentence in a file is a request: "a sentence in a memory file is a request that the model weighs". *a* is ruled out because a rule loads conventions and denies nothing: "A rule with a `paths` list loads when Claude works with a file that matches one of its globs". *d* is ruled out because the command's tools are another matter: "a shared review command with a description and read-only tools".

</details>

Adapted from the sample questions of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the questions are Anthropic's. The account of restructuring a monolith on this page follows the guide's sample question on plan mode, rewritten here.
