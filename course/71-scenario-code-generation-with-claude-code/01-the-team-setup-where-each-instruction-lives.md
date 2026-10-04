# The team setup: where each instruction lives

**Level:** Architect · **Module 71:** Scenario: code generation with Claude Code · **Page 1 of 2**
**Exams:** A3; S2

**After this page you can** place each requirement of the code generation scenario in the mechanism that provides it (the root memory file, a path-scoped rule, a shared command, a permission rule, a skill), say why each tempting wrong placement fails, and audit a team's setup with a checklist that reads its files.

Checked on 2026-10-04 against the Claude Code documentation pages "How Claude remembers your project", "Extend Claude with skills", "Best practices for Claude Code" and "Choose a permission mode" (the pages name Claude Code version 2.1.286), and against the Architect exam guide (version 1.0, scenario 2 and its sample questions). The example runs offline in Python, TypeScript, Java and Kotlin and reads two small folders of plain files; it does not start Claude Code. This page is a capstone: it uses modules 49 and 56 to 59 and puts them in the order the exam asks about.

## Why it matters

Scenario S2 of the Architect exam is a team that uses Claude Code for generation, refactoring, debugging and documentation, and wants to fit it into its workflow with slash commands, memory files and a rule for when to plan first. The questions are almost all of one kind: a requirement is stated, four places are offered, and one of them gives the requirement exactly. A command every developer must get on clone. Conventions that depend on the kind of file and not on the folder. A restriction that must hold whatever the model decides. Each has a mechanism built for it, and the wrong options are the mechanisms built for something else.

## The idea

### The scenario in plain words

A team generates code, refactors, debugs and writes documentation with Claude Code. It wants three things from the setup. The team's conventions should reach Claude without anyone retyping them. Common tasks, such as a review against the team checklist, should be one slash command that every developer has. And the team should know when to let Claude plan before it edits, and when to let it just do the work.

### The requirement decides the mechanism

| The requirement | The mechanism | Why the others fail |
|---|---|---|
| Every developer has the `/review` command after a clone or a pull | A command file in the project, `.claude/commands/review.md`, committed to the repository (or a skill at `.claude/skills/review/SKILL.md`) | A command in the home folder is personal and is not shared. The root memory file holds context, not command definitions. A `commands` array in a config file is a mechanism that does not exist |
| Conventions that depend on the kind of file, wherever the file sits (tests next to the code they test) | Rule files in `.claude/rules/`, each with a `paths` list of globs | One root file with a section per area leaves Claude to infer which section applies. Skills load when they are invoked or chosen, not because a path matched. A memory file per folder is bound to its directory, and test files are spread over many |
| What every task needs: the test command, the repository's etiquette | The root `CLAUDE.md`, short | Nothing else loads in every session at the same priority |
| A preference of one developer | `CLAUDE.local.md` (kept out of version control) or a file in the home folder | The shared root file is for the team |
| A rule that must hold whatever Claude decides: never read `.env` | A permission rule or a hook | A memory file is context: Claude "treats them as context, not enforced configuration" |
| Domain knowledge needed only sometimes | A skill, which loads on demand | The root file loads in every session and costs context each time |

The sixth row and the fifth carry the course's recurring distinction: a sentence in a memory file is a request that the model weighs, and a permission rule or a hook is code that runs. The documentation draws the line on the memory page itself: "To block an action regardless of what Claude decides, use a PreToolUse hook instead."

### What loads when

The memory page gives the loading rules that make the table work.

- The root `CLAUDE.md` and the rule files without a `paths` list load at the start of every session, at the same priority.
- A rule with a `paths` list loads when Claude works with a file that matches one of its globs, so a convention costs context only in the sessions that touch its files.
- A `CLAUDE.md` in a subdirectory is not loaded at launch. In the page's words: "Files in subdirectories load on demand when Claude reads files in those directories."
- Size counts. The page's target is "under 200 lines per CLAUDE.md file", because "Longer files consume more context and reduce adherence", and it adds that imports "help you organize a long file but don't reduce its context cost, because imported files also load at launch". Splitting a long file into imported pieces changes nothing about the load. Moving the area-specific parts into path-scoped rules does.

The practice of this module uses a stricter limit of 25 lines for the root file of a small project. That number is the course's, chosen to make the point testable. The documented target is 200.

### What a glob matches

A `paths` entry is a glob. `**` crosses folders and `*` stays inside one, so `src/api/**/*.ts` reaches every TypeScript file below `src/api/`, and `**/*.test.tsx` reaches test files in any folder. A bare folder name such as `src/api` is not a glob and matches no file. The pages of module 57 give the full rules; the example below uses these three forms only.

### The audit: a checklist that reads the files

A setup is a set of files, so a checklist can read it. The example implements the one this course uses for the scenario, on two projects beside the example: `project-before`, where everything sits in one root file, and `project-after`, where each requirement has its mechanism. For each project the audit reports:

- `all-in-root`: three or more sections in the root file and no rule files, which means the area conventions load everywhere and Claude must infer which applies.
- `rule-loads-always` and `rule-matches-nothing`: a rule with no `paths` list, or whose globs match none of the project's files.
- `test-uncovered`: a test file that no rule reaches, which is the colocated-tests question of the exam.
- `no-shared-command`: no command file and no skill in the project.
- `env-readable` and `bare-bash-allowed`: the settings do not deny reading `.env`, or allow the whole Bash tool.

For a project with no findings, the example prints which rule files load for each of its sample files, so that the effect of the globs is visible.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* project-scoped custom commands live in `.claude/commands/` in the repository, where they are version-controlled and reach every developer; commands in the home folder are personal, and the memory file is for instructions and context. *What the product does now (the "Extend Claude with skills" page, checked 2026-10-04):* "Custom commands have been merged into skills. A file at `.claude/commands/deploy.md` and a skill at `.claude/skills/deploy/SKILL.md` both create `/deploy` and work the same way", the existing command files keep working, and when a skill and a command file share a name the skill wins. Project skills live in `.claude/skills/<skill-name>/SKILL.md` and are committed so that the team gets them. So a question that asks where to put a shared slash command is answered with the project's `.claude/commands/` folder, and in a current setup a skill in `.claude/skills/` does the same work and also carries more options (module 58).

### The example

The example loads each project and runs the checklist. It prints one line of facts per project, then the findings for the flawed one, and for the fixed one the rule files that load for each sample file. The test files of the fixed project load two rule files each: the rule of their area and the testing rule, which is what "regardless of location" means. The program and its output are the same in all four languages.

<!-- example: m71-team-setup-audit tabs: python -->
<!-- /example -->

The flawed project's root file has five sections and no rules, so the audit reports `all-in-root`, and none of its three test files is reached by a rule. It has no shared command, it does not deny `.env`, and it allows the whole Bash tool. The fixed project has none of those findings: four rule files, a shared command and settings that deny the environment file and allow one named command.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"Put all conventions in the root file under one heading per area, and let Claude work out which applies."** It is tempting because it is one file and easy to read. The exam rejects it: it relies on inference, so it is not reliable, and every area's conventions cost context in every session.
2. **"Make a skill for each kind of code."** It is tempting because skills are a modern feature. The exam rejects it for conventions that must apply automatically by path: a skill is invoked, or chosen by the model from its description, and a path match does not start it.
3. **"Put a `CLAUDE.md` in every subdirectory."** It is tempting because it matches the folder structure. The exam rejects it when the files to cover are spread over many folders: each file is bound to its directory, and test files next to their sources are in all of them.
4. **"Put the shared command in the home folder or in the root memory file."** It is tempting because both are places Claude reads. The exam rejects both: the first is personal and the second is not a place for command definitions.

## Quiz

1. A team wants a review command that every developer has after cloning the repository. Where does the file go?
   - **a**: In a settings file that holds a list of commands for Claude Code
   - **b**: In a commands folder in each developer's home directory, created by hand
   - **c**: In the root instructions file, under a heading that names the review checklist
   - **d**: In the project's own configuration folder, committed with the sources

2. React components use hooks, handlers use async/await, models follow a repository pattern, and test files sit beside the code they test in every folder. Which setup applies the right conventions automatically, test files included?
   - **a**: A rule per concern, each scoped by a glob that matches names
   - **b**: One root file with a section for each area, left for Claude to apply as it sees fit
   - **c**: A skill for each area, which developers start with a slash command
   - **d**: A memory file in each folder that holds the conventions of that folder

3. A 400-line root memory file is split into five files that the root file imports. What happens to the context cost?
   - **a**: It falls to a fifth, since each piece loads when needed
   - **b**: It doubles, as every piece is read once by the root and once again
   - **c**: It does not change, since everything referenced is read at the start
   - **d**: It falls in any session that never touches the files the pieces describe

<details>
<summary>Answer key</summary>

1. **d**. A command file in the project's own folder is committed and reaches every clone. *b* is ruled out because a home folder is personal: "commands in the home folder are personal". *c* is ruled out because the memory file holds instructions and not definitions: "the memory file is for instructions and context". *a* is ruled out because no such mechanism exists: "A `commands` array in a config file is a mechanism that does not exist".
2. **a**. Rules with globs reach files by their names, wherever they sit. *b* is ruled out because the model would have to infer the section: "One root file with a section per area leaves Claude to infer which section applies." *c* is ruled out because a path match starts nothing in a skill: "a skill is invoked, or chosen by the model from its description, and a path match does not start it". *d* is ruled out because a file is bound to its directory: "each file is bound to its directory, and test files next to their sources are in all of them".
3. **c**. Imports organise a file and do not cut its cost. *a* is ruled out because the load is at launch: "imported files also load at launch". *b* is ruled out because nothing is read twice; the page says only that they "don't reduce its context cost". *d* is ruled out because an import is not conditional on touched files; only a rule with a `paths` list is: "A rule with a `paths` list loads when Claude works with a file that matches one of its globs".

</details>

Adapted from the sample questions of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the questions are Anthropic's. The first question follows the guide's sample question on where a shared slash command goes, and the second its sample question on conventions that depend on the file type, both rewritten here.
