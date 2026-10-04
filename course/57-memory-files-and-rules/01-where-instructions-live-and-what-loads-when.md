# Where instructions live and what loads when

**Level:** Architect · **Module 57:** Memory files and rules · **Page 1 of 2**
**Exams:** A3.1; S2

**After this page you can** say which file an instruction belongs in (managed, user, project, local or directory), predict which memory files are in context for a session started in a given folder, write `@` imports that expand, explain when `AGENTS.md` is read, and check what loaded with `/memory` and `/context` instead of guessing.

Checked on 2026-10-03 against the Claude Code documentation pages "How Claude remembers your project" (memory), "Best practices for Claude Code" and "Use hooks"-level facts quoted from the memory page, which document behaviour up to Claude Code v2.1.286. Nothing here was run against a live session: the example is a model of the documented loading rules, written as plain code over file paths and texts. This page deepens module 38 (the memory file, its size and where the layers sit) and module 27 (what each extension costs), and it does not repeat them. Rules with `paths` and the size of the files are the second page.

> **Exam guide and current product.** *What the guide states (task statement 3.1), and so what the exam keys:* the hierarchy has three levels, user (`~/.claude/CLAUDE.md`), project (`.claude/CLAUDE.md` or the root file) and directory (CLAUDE.md files in subdirectories); user-level instructions "are not shared with teammates via version control"; `@import` keeps a file modular; `.claude/rules/` is the alternative to one monolithic file; and the `/memory` command is how you "verify which memory files are loaded". *What the current product does (documentation checked 2026-10-03):* there are more places than three. A managed policy file sits above the user file, `./CLAUDE.local.md` holds personal notes for one project, `AGENTS.md` is read in one situation, and auto memory is a fourth kind of memory that Claude writes itself. `/memory` "lists your CLAUDE.md, CLAUDE.local.md, and other memory file locations across user and project scopes", including entries "for files that don't exist yet"; the command that shows what loaded into the current session is `/context`. On the exam, answer the scoping question with the guide's three levels and its rule that a user-level file never reaches a teammate; when you debug a real session, run `/context`.

## Why it matters

A new engineer joins a team and Claude Code in her checkout ignores the team's rule that every endpoint validates its input. The rule exists: a senior engineer wrote it into his own `~/.claude/CLAUDE.md` months ago and has been getting the right behaviour ever since. Nothing in the repository carries it, so nobody else gets it. This is the exam's scenario S2 in its simplest form, and the question behind it is always the same one: at which level does an instruction live, and who receives it. The remaining failures come from the same ignorance of loading: a file in a subdirectory that has not been read yet, an import with a typo that imports nothing, and an `AGENTS.md` that is never read because a `CLAUDE.md` sits one folder up.

## The idea

### Five places, one load order

Memory files are plain Markdown that Claude reads "at the start of every session". The documentation lists them "in load order, from broadest scope to most specific, so a project instruction appears in context after a user instruction".

| Scope | Where | Who gets it |
|---|---|---|
| Managed policy | `/etc/claude-code/CLAUDE.md` on Linux and WSL, `/Library/Application Support/ClaudeCode/CLAUDE.md` on macOS | Everyone on the machine, and individual settings cannot exclude it |
| User | `~/.claude/CLAUDE.md` | You, in every project |
| Project | `./CLAUDE.md` or `./.claude/CLAUDE.md` | The team, "via source control" |
| Local | `./CLAUDE.local.md`, added to `.gitignore` | You, in this project |
| Directory | `CLAUDE.md` in a subdirectory | Whoever works in that subdirectory |

The first three rows are the guide's hierarchy plus the organisation layer the product added. The two questions the exam asks of the table are who receives the instruction and whether it travels with the repository. A user-level instruction does not, which is the reason the engineer in the scenario never shared it, and the fix is to move the rule into the project file and commit it. A personal note about one project, such as a sandbox address, belongs in the local file, which is why that file is ignored by git.

The managed layer has a companion that is easy to confuse with it. A managed CLAUDE.md is for behaviour ("Code style and quality guidelines", "Data handling and compliance reminders"); blocking a tool, a command or a path is done with managed settings, because "Claude treats them as context, not enforced configuration".

### What loads at launch and what loads later

Files in the directory hierarchy "above the working directory are loaded at launch". Start in `web/ui/` and Claude loads `web/ui/CLAUDE.md`, `web/CLAUDE.md` and the root file, plus any `CLAUDE.local.md` beside them. The files are not alternatives: "All discovered files are concatenated into context rather than overriding each other", ordered from the filesystem root down to the working directory, so "instructions closer to where you launched Claude are read last". In each folder the local file comes after the shared one.

Files below the working directory behave differently. A CLAUDE.md in a subdirectory "load[s] on demand when Claude reads files in those directories". Start in the repository root, and `web/CLAUDE.md` is not in context until Claude reads something under `web/`. Two consequences follow. A convention that must hold from the first message cannot live in a subdirectory file, and an instruction can seem to appear halfway through a session, which is the file loading the moment Claude opened a file there. The same on-demand behaviour is why the documentation tells you, after `/compact`, that nested files "reload as Claude reads files they apply to" while the root file is re-read from disk at once.

### Imports keep a file modular and save nothing

A CLAUDE.md "can import additional files using `@path/to/import` syntax". Imported files "are expanded and loaded into context at launch alongside the CLAUDE.md that references them". Relative paths "resolve relative to the file containing the import, not the working directory", and imports can import others "with a maximum depth of four hops". Import parsing "skips Markdown code spans and fenced code blocks", so writing `` `@README` `` mentions a file without importing it, and writing `@README` outside backticks imports it.

Two properties matter for the exam. An import changes where text is kept and not how much of it is loaded: imports "help you organize a long file but don't reduce its context cost, because imported files also load at launch". A path that names no file imports nothing and says nothing about it, so a typo is a silent loss; the example's `unresolved_imports` finds it, and in a real session `/context` shows what actually loaded. Imports that point outside the project (a file in your home folder) trigger an approval dialog the first time, because a committed file could otherwise pull in anything.

### AGENTS.md is read in one situation

Claude Code "can read `AGENTS.md` as your project instructions", with a rule that surprises people: "By default, Claude reads `AGENTS.md` only when you have no `CLAUDE.md` in your working directory or above it." A `CLAUDE.md`, a `.claude/CLAUDE.md` or a `CLAUDE.local.md` in the working directory or any directory above it counts, so adding a personal local file to a repository that relies on `AGENTS.md` quietly stops Claude from reading it. The user file and the managed file do not count. The documented ways out are to import it (`@AGENTS.md` in the CLAUDE.md, which then reads both) or to set **Project instructions** to `claude-md-and-agents-md` in `/config`. A version note belongs with the claim: reading `AGENTS.md` directly "requires Claude Code v2.1.277 or later".

### Context, not enforcement

Memory is advice with good delivery. The documentation says it plainly: "CLAUDE.md content is delivered as a user message after the system prompt, not as part of the system prompt itself. Claude reads it and tries to follow it, but there's no guarantee of strict compliance, especially for vague or conflicting instructions." For an instruction that "must run at a specific point, such as before every commit or after each file edit", the documented answer is a hook, and for a path or command that must not be touched it is a permission rule (module 38, module 49). If two files give different guidance for the same behaviour, "Claude may pick one arbitrarily", so the review of memory files includes a search for contradictions across the levels.

Auto memory is the other kind. Claude writes its own notes to `~/.claude/projects/<project>/memory/`, loads "the first 200 lines of `MEMORY.md`, or the first 25KB, whichever comes first" at the start of every conversation, and keeps them on the machine: the files "are not shared across machines or cloud environments". It is a place where Claude records what it learned, and not a place for the team's rules.

### Seeing what loaded

Three tools answer "what does Claude actually have". `/context` lists the memory files that loaded into the session under **Memory files**; if a file is missing there, "Claude can't see it". `/memory` lists the memory file locations and opens them in your editor, including ones you have not created yet. The `InstructionsLoaded` hook logs "which `CLAUDE.md` and rules files are loaded, when they load, and why", which is the way to watch lazy loading from a script. `AGENTS.md` shows in `/memory` when Claude read it, from v2.1.280.

### The example

The example is a model of the loading rules over paths and texts, in the same style as module 38's: the launch set for a folder, the files that join when Claude reads below it, whether `AGENTS.md` is read, the rules a set of touched files brings in (the second page), glob matching, and the import expansion with its hop limit and its report of references that name no file.

<!-- example: m57-memory-loading tabs: python,typescript -->
```python
"""Which instruction files are in Claude Code's context, and when: the launch set, the files that load on demand, path-scoped rules, imports and AGENTS.md.

The model follows the memory documentation read on 2026-10-03 (Claude Code v2.1.286): files in the directories above the working directory load at
launch, root first; files below it load when Claude reads there; a rule with `paths` loads when a matching file is read, written or edited; an
import expands at launch to at most four hops; AGENTS.md is read only when no CLAUDE.md file exists in the working directory or above it.
Nothing here starts Claude Code: the "project" is a list of file paths and a dict of file texts.
"""
```
<!-- /example -->

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Put the team's rule in `~/.claude/CLAUDE.md`; every developer has one."** It is tempting because the user file loads in every project. The exam rejects it: that file is personal and is "not shared with teammates via version control", so a new teammate never receives it. The rule goes in the project file, committed.
2. **"Split the 600-line file into imports; the context cost falls."** It is tempting because the root file becomes short. The exam, and the documentation, reject it: imports "don't reduce its context cost, because imported files also load at launch". Path-scoped rules, which load only on a match, are the split that saves context (page 2).
3. **"Put the convention in the subdirectory's CLAUDE.md; Claude will have it from the start."** It is tempting because the file sits next to the code. The exam rejects it: a subdirectory file loads "on demand when Claude reads files in those directories", so from the repository root it is absent until Claude opens a file there.
4. **"Add a `CLAUDE.local.md` for my notes; the repository's `AGENTS.md` still loads."** It is tempting because the local file looks additive. The product rejects it: the local file counts as a CLAUDE.md, so Claude "reads only your CLAUDE.md files" and `AGENTS.md` is skipped unless you import it or change the setting.

## Quiz

1. An engineer wants a reminder about a private test server to guide Claude in one repository on their own machine, and never to reach teammates. Where does it go?
   - **a**: The committed instruction file, under a heading for personal items
   - **b**: A git-ignored local note beside the root instruction file
   - **c**: The personal file in the home folder, applying to every project
   - **d**: The organisation policy file, which only that engineer edits

2. A session starts at the top of a project, and an instruction file sits two levels down in `web/ui/`. When do its lines reach the model?
   - **a**: At launch, together with the root file
   - **b**: Only after a restart that begins inside that directory
   - **c**: When the user runs the memory command
   - **d**: Once Claude opens something beneath that directory

<details>
<summary>Answer key</summary>

1. **b**. The local file is personal to one project and is kept out of version control. *a* is ruled out because "A personal note about one project, such as a sandbox address, belongs in the local file", and a committed file would share it. *c* is ruled out because that file reaches "You, in every project", and the reminder concerns one repository. *d* is ruled out because that layer reaches "Everyone on the machine, and individual settings cannot exclude it", so it is not a private note.
2. **d**. A file below the starting directory loads on demand. *a* is ruled out because the files that load first are the ones "above the working directory are loaded at launch", and this one is below it. *b* is ruled out because a lower file loads "on demand when Claude reads files in those directories", with no restart. *c* is ruled out because the memory command "lists your CLAUDE.md, CLAUDE.local.md, and other memory file locations" and loads nothing.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
