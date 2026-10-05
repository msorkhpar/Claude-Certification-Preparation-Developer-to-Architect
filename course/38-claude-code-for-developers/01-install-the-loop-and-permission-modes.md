# Install, the working loop and permission modes

**Level:** Developer · **Module 38:** Claude Code for developers · **Page 1 of 3**
**Exams:** DV7; A3

**After this page you can** install Claude Code and check it, run the explore, plan, code and commit loop, name the six permission modes and what each lets run without a prompt, say which mode a session starts in, and resume, fork or rewind a session.

Checked on 2026-10-03 against the Claude Code documentation (setup, how Claude Code works, common workflows, permission modes and the sessions pages), which mention behaviour up to Claude Code v2.1.286. Nothing on this page was run against a live session: the course environment has no key and no network, and the commands are described and not run. The practice at the end of the module checks the configuration files offline.

## Why it matters

Module 35 drove the Claude Code loop from code. This module is about the person who uses it daily and the repository that has to be ready for them. The exam asks developer questions: how a session starts, what a permission mode lets through, where an instruction belongs, and what to do when a long conversation goes wrong. Each answer is a fact about the product, so each is quoted.

## The idea

### Install and check

The setup page recommends the native installer for your platform. "When the installer finishes, open a new terminal window and run `claude --version`." A working installation prints a version number. If the shell says `claude` is not found, the install directory is not on the `PATH` yet. Homebrew and WinGet installations "do not auto-update", while "Native installations automatically update in the background", so a team that pins a version chooses a package manager and one that wants the newest chooses the native installer. After installation you open a terminal in the project and start `claude`.

### The loop: explore, plan, code, commit

Claude Code works on a whole project and not on a single file. When you ask it to fix a bug, "it searches for relevant files, reads multiple files to understand context, makes coordinated edits across them, runs tests to verify the fix, and commits the changes if you ask." A habit that fits this has four steps.

1. **Explore.** Ask Claude to read and explain before it changes anything. It reads files in the session's context window, so you can ask where a behaviour lives.
2. **Plan.** For a change you want to review before it touches disk, switch to plan mode: "Claude reads files and proposes a plan but makes no edits until you approve." Press `Shift+Tab` until the status bar shows `⏸ plan mode on`, or start with `claude --permission-mode plan`.
3. **Code.** Approve the plan, let Claude edit, and have it run the tests. A test run is the evidence that the change works, so name the command in the project's memory file (page 2).
4. **Commit.** Ask for the commit when the change is reviewed. Claude commits the changes only "if you ask".

Two limits shape the loop. The context window fills up. "Claude compacts automatically, but instructions from early in the conversation can get lost", so "Put persistent rules in CLAUDE.md", and use `/context` to see what fills the window and `/compact` to summarise it. And file edits are reversible: "Before Claude edits a file, it snapshots the current contents. If something goes wrong, press `Esc` twice to rewind to a previous state, or ask Claude to undo." The command `/rewind` rolls code and conversation back to a checkpoint. Checkpoints "only cover file changes", and "Actions that affect remote systems (databases, APIs, deployments) can't be checkpointed", so a pushed commit or a changed database is not undone by a rewind.

### Sessions

"Sessions are independent." Each new session starts with a fresh context window and none of the earlier conversation. Claude Code saves each conversation locally, and `claude --continue` or `claude --resume` reopens one "under the same session ID and appends new messages to the existing conversation." Forking with `--fork-session` or `/branch` "copies the history into a new session ID, leaving the original unchanged", which is how you try a second approach without losing the first. `/clear` starts fresh on a new task while keeping project memory.

### Permission modes

A mode "sets which actions Claude can take in a session without asking you first." There are six, and the table is the part to know by heart.

| Mode | What runs without asking | Best for |
|---|---|---|
| `default` (Manual) | Reads only | Reviewing every action yourself |
| `acceptEdits` | Reads, file edits, and common filesystem commands | Iterating on code you review |
| `plan` | Reads, plus classifier-approved commands when auto mode is available | Exploring before changing |
| `auto` | Everything, with background safety checks | Long tasks, fewer prompts |
| `dontAsk` | Reads and pre-approved tools; anything that would prompt is denied | Locked-down CI and scripts |
| `bypassPermissions` | Everything | Isolated containers and VMs only |

In auto mode "a second model, the classifier, reviews actions instead of you." That is a review by a model, and a person still decides the rules around it. Modes set the baseline and rules adjust it: "Deny rules block in every mode, including `bypassPermissions`." And "Allow rules have no effect in `bypassPermissions`." An action matched by an ask rule is not auto-approved in any mode.

Which mode a session starts in follows an order: the `--permission-mode` flag first, then `permissions.defaultMode` in a settings file, then the built-in default. One rule protects a clone from a hostile repository: "If you set `"auto"` in `.claude/settings.json` or `.claude/settings.local.json`, the value doesn't take effect", and Claude Code uses the built-in default instead. A `"bypassPermissions"` set in those two files "doesn't take effect either, and the session starts in Manual mode". The other values apply from any settings file. The built-in default is `auto` in a terminal with v2.1.283 or later, and `default` in `claude -p` in most sessions.

## Traps

1. **Treating plan mode as a sandbox.** It stops edits until you approve a plan, and it does not stop reads. It is for exploring, and it is not a defence against a hostile repository.
2. **Assuming a project can switch itself to auto or bypass mode.** A shared or local project file cannot set those two values. The user's own file or the command line can.
3. **Relying on a rewind for outside effects.** Checkpoints restore the files Claude edited. A pushed commit or a changed database is outside them.

## Quiz

1. A developer wants Claude to propose a change for review, with no edits written until a person approves. Which start fits?
   - **a**: The `bypassPermissions` mode, which shows each step before running it
   - **b**: The `acceptEdits` mode, which proposes and then writes each edit
   - **c**: The `dontAsk` mode, which reads the files and asks for approval
   - **d**: The `plan` mode, which reads the project and drafts steps first

2. A repository's shared settings file sets the starting permission mode to `bypassPermissions`. What happens when a teammate opens it?
   - **a**: The value is ignored, and each edit and command asks first
   - **b**: The session starts in auto mode for every teammate who opens it
   - **c**: The session starts in plan mode until the file is approved
   - **d**: Every teammate is asked once to accept the project file's mode

3. A long conversation has drifted, and early instructions seem forgotten. Where does the page say persistent rules belong?
   - **a**: In the first message of every session, typed out again by hand each time
   - **b**: In the `/compact` summary, which is then written by the person alone
   - **c**: In the project memory file, loaded when a session starts
   - **d**: In a rewind checkpoint, which restores the earlier messages in full

<details>
<summary>Answer key</summary>

1. **d**. The page says "Claude reads files and proposes a plan but makes no edits until you approve", and `claude --permission-mode plan` starts there. *b* is ruled out because `acceptEdits` runs "Reads, file edits, and common filesystem commands" without asking, so edits are written. *c* is ruled out because `dontAsk` runs "Reads and pre-approved tools; anything that would prompt is denied", and it denies and does not ask for approval. *a* is ruled out because `bypassPermissions` runs "Everything" without asking, and is for "Isolated containers and VMs only".
2. **a**. The page says a `"bypassPermissions"` set in those two files "doesn't take effect either, and the session starts in Manual mode". *b* is ruled out because that is exactly what the rule prevents, since "the value doesn't take effect". *c* is ruled out because the page names Manual mode and not plan mode: "the session starts in Manual mode". *d* is ruled out because "The other values apply from any settings file", and these two are the exceptions, so no prompt is shown.
3. **c**. The page says "Put persistent rules in CLAUDE.md", because "Claude compacts automatically, but instructions from early in the conversation can get lost." *a* is ruled out because "Each new session starts with a fresh context window", and typing rules again is what the memory file replaces. *b* is ruled out because "Claude compacts automatically, but instructions from early in the conversation can get lost", so a summary is what loses them. *d* is ruled out because "file edits are reversible" by a checkpoint, and a checkpoint restores files and is not a place to store rules.

</details>
