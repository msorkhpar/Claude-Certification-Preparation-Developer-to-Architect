# Commands, headless runs, auto mode and the practice

**Level:** Developer · **Module 38:** Claude Code for developers · **Page 3 of 3**
**Exams:** DV7; A3

**After this page you can** use the built-in slash commands that matter for a working session, write a custom command as a skill, run Claude Code without a person at the keyboard with the right flags and limits, say what auto mode does, set a repository up with `/init`, and finish the module's practice.

Checked on 2026-10-03 against the Claude Code documentation (commands, skills, headless, CLI reference and permission modes), which mention behaviour up to Claude Code v2.1.286. No command on this page was run: the course has no key and no network. The practice tests read the files you write, using the course's models of the documented rules.

## Why it matters

Interactive habits make a developer fast, and unattended runs make a team consistent. The exam asks about both: which command to reach for, and which flags make a scripted run safe and bounded. A run with no person present cannot ask, so its limits must be written down in advance.

## The idea

### Built-in commands

Commands "control Claude Code from inside a session." A command "is only recognized at the start of your message", and the text after its name becomes its arguments. The documentation arranges them by moment in the work, and that is the easiest way to remember them.

| Moment | Commands |
|---|---|
| First session in a repository | `/init` for a starter memory file, `/memory` to refine it, `/permissions` to set rules, `/mcp` for servers |
| During a task | `/plan` for plan mode, `/model` and `/effort`, `/context` to see what fills the window, `/compact` to summarise |
| Before shipping | `/diff` for what changed, `/code-review` for the current diff |
| Between sessions | `/clear` for a fresh task, `/resume`, `/branch` |
| When something is wrong | `/rewind` to a checkpoint, `/doctor` for a setup checkup |

Starting a repository is a two-step habit. Run `/init`, which "analyzes your codebase and creates a file with build commands, test instructions, and project conventions it discovers". Then edit the result by hand, cut what Claude would have found anyway, and add what it would get wrong. Run `/permissions` and write the rules you settled on into the shared settings file so that they travel with the repository.

### Custom commands are skills

"Custom commands have been merged into skills." A file at `.claude/commands/deploy.md` and a skill at `.claude/skills/deploy/SKILL.md` "both create `/deploy` and work the same way." A skill is a folder with a `SKILL.md`: frontmatter between `---` lines, then instructions. The fields this module uses are `name`, `description`, `argument-hint` (a hint such as `[issue-number]` shown in autocomplete) and `disable-model-invocation`. The body can use `$ARGUMENTS` for what the person typed after the command name.

Setting `disable-model-invocation: true` tells it to "prevent Claude from automatically loading this skill", so only a person can start it. Use it for workflows with side effects: a command that edits code and calls a service should run when you type it and not when Claude decides it fits. Module 39 covers skills in depth, including the other fields.

### Headless runs

`claude -p` runs one prompt without a session: "Run Claude non-interactively for CI, pre-commit hooks, or batch processing." Four groups of flags make a run safe.

- **Context.** `--bare` skips "auto-discovery of hooks, skills, custom commands, subagents, installed plugins, MCP servers, auto memory, and CLAUDE.md", and the page says it is "useful for CI and scripts where you need the same result on every machine." Bare mode "never reads OAuth credentials or the system keychain", so the key must be in `ANTHROPIC_API_KEY` in the environment. Without it, a `-p` session loads whatever the working directory configures, hooks and MCP servers included, and it shows no trust dialog.
- **Output.** `--output-format` is `text`, `json` or `stream-json`. Use `json` when a script parses the result.
- **Bounds.** `--max-turns` limits the number of agentic turns, and `--max-budget-usd` caps the spend. A run that has neither can loop for as long as the model keeps calling tools.
- **Permissions.** `--permission-mode dontAsk` runs reads and pre-approved tools and denies anything that would prompt, which fits a script. `--allowedTools "Read,Bash(git diff *)"` pre-approves specific tools, in quotes and separated by commas. List patterns and not whole tools that change things. Never use `--dangerously-skip-permissions` outside "a container, VM, or the sandbox runtime".

A complete safe review script is five lines. It pipes the diff to `claude -p`, adds `--bare` and `--output-format json`, sets `--max-turns` and a dollar cap, uses `dontAsk`, and lists `Read` and read-only `git` patterns. The key is not in it.

### Auto mode

Auto mode replaces the prompt with a reviewer. "In auto mode, a second model, the classifier, reviews actions instead of you." Most calls run, the classifier blocks what looks risky, and a person is asked only when it cannot decide. It suits a long task where prompts interrupt the work. It is not a substitute for rules: deny rules still apply in it, and the requirements include a supported model. In a terminal on a recent version it is the built-in starting mode, so a team that wants Manual must set it.

### The practice

The practice sets up a small repository, the invoice API, for Claude Code. You write the project's memory file, its shared settings, a personal settings file, a custom command and a headless review script. Tests then read your files and ask the questions this module raised: does the memory file stay short and import the architecture notes, do sixteen sample calls get the right answer from the merged rules, does the shared file avoid modes it cannot set, does the personal file stay local, can only a person start the command, and does the script have limits and no key. The files are language-neutral, so the module has Python and TypeScript test suites and no Java or Kotlin edition: no YAML or JSON library is available offline for Java or Kotlin here, and a second edition would test the same files. The statement lists each cases id, and the starter fails every test.

## Traps

1. **Running `claude -p` without `--bare` in CI.** The run loads the working directory's hooks and servers, and a teammate's setup changes the result.
2. **Leaving a headless run unbounded.** Without `--max-turns` and a budget cap, nothing stops a loop.
3. **Pre-approving whole tools.** `--allowedTools "Bash"` approves every command. List `Bash(git diff *)` and its like.
4. **Letting Claude start a command that has side effects.** A skill that edits code and calls a service needs `disable-model-invocation: true`.

## Quiz

1. A CI job runs `claude -p` to review each diff and must give the same result on every runner. Which flag is the key part?
   - **a**: `--bare`, because it skips the local setup that discovery finds
   - **b**: `--output-format json`, because it gives a script structured text to parse
   - **c**: `--max-turns`, because it limits how many agentic turns a single run takes
   - **d**: `--max-budget-usd`, because it caps the amount that a single run may spend

2. A custom command edits code and then calls an issue tracker. Which setting makes sure only a person can start it?
   - **a**: The value true on disable-model-invocation in its frontmatter
   - **b**: A hint in the argument field of the command's frontmatter block
   - **c**: A deny rule for the command in the project settings file of the team
   - **d**: Placing the file in the user directory instead of in the project

3. A script lists `--allowedTools "Bash"` so that a headless review can run `git diff`. What is wrong with it?
   - **a**: It fails to start at all, since a tool name must carry a pattern
   - **b**: It is ignored by the run, because bare mode never reads the tool list
   - **c**: It approves every shell command, and a pattern limits the scope
   - **d**: It asks for approval, because a bare name is treated as an ask rule

<details>
<summary>Answer key</summary>

1. **a**. The page says `--bare` is "useful for CI and scripts where you need the same result on every machine." *b* is ruled out because "Use `json` when a script parses the result", which shapes the output and does not remove local configuration. *c* is ruled out because "`--max-turns` limits the number of agentic turns", which bounds a run and does not make two runners agree. *d* is ruled out because "`--max-budget-usd` caps the spend", which is a bound on cost and not on configuration.
2. **a**. The page says setting `disable-model-invocation: true` tells it to "prevent Claude from automatically loading this skill", so only a person can start it. *b* is ruled out because `argument-hint` is "a hint such as `[issue-number]` shown in autocomplete" and controls no one. *c* is ruled out because the page says such a command "should run when you type it and not when Claude decides it fits", and gives the frontmatter field for that. *d* is ruled out because a skill's location sets who has it, and the page says "Custom commands have been merged into skills" and work the same way.
3. **c**. The trap says "`--allowedTools "Bash"` approves every command. List `Bash(git diff *)` and its like." *a* is ruled out because the page says "List patterns and not whole tools that change things", as a recommendation, and a bare `Read` is accepted. *b* is ruled out because bare mode "never reads OAuth credentials or the system keychain", and the tool list is passed with the flag and is not discovered. *d* is ruled out because "`--permission-mode dontAsk` runs reads and pre-approved tools", and a pre-approved tool does not ask.

</details>

## Module quiz

This quiz covers all three pages of the module.

1. A team begins a new repository and wants first project instructions quickly, then wants them trimmed. Which pair of steps fits?
   - **a**: Copy a very large template, then import it from a short file
   - **b**: Generate a draft with `/init`, then cut what Claude finds alone
   - **c**: Write every convention by hand, then run `/compact` to shorten it
   - **d**: Ask for a very long draft, then rely on the classifier to skip extras

2. A nightly script runs `claude -p` and sometimes loops on tool calls for an hour. Which pair of flags bounds it?
   - **a**: A permission mode and a tool list for the run
   - **b**: A bare switch and a JSON output format on the run
   - **c**: A turn limit and a spend cap on the run
   - **d**: A resume flag and a fork flag for the next run

3. A team wants the review script to inspect code and change nothing, with no prompt ever shown. Which setup fits?
   - **a**: `bypassPermissions`, with the runner as the only boundary
   - **b**: `auto`, with the classifier left to judge every single call
   - **c**: `acceptEdits`, with edits approved for the whole checkout
   - **d**: `dontAsk` with `Read` and read-only git patterns listed

4. A session started in Manual mode has been running for hours. The developer wants code changes to apply without a prompt while shell work still asks. Which mode fits?
   - **a**: `plan`, which runs changes after one approval has been given once
   - **b**: `acceptEdits`, which runs file edits plus filesystem commands
   - **c**: `dontAsk`, which approves changes and asks about every shell call
   - **d**: `auto`, which asks about changes and runs shell work unseen for good

<details>
<summary>Answer key</summary>

1. **b**. The page says `/init` "analyzes your codebase and creates a file with build commands, test instructions, and project conventions it discovers", then to "cut what Claude would have found anyway". *a* is ruled out because imports "don't reduce its context cost, because imported files also load at launch". *c* is ruled out because "Longer files consume more context and reduce adherence", and `/compact` summarises a conversation and not a file. *d* is ruled out because the classifier reviews "actions instead of you" and does not shorten files.
2. **c**. The page says "`--max-turns` limits the number of agentic turns, and `--max-budget-usd` caps the spend." *b* is ruled out because "Use `json` when a script parses the result", and bare mode only skips discovery, so neither bounds a loop. *a* is ruled out because "`--permission-mode dontAsk` runs reads and pre-approved tools", which decides what may run and not how long. *d* is ruled out because `--resume` reopens a session "under the same session ID and appends new messages to the existing conversation", which continues a loop and does not bound it.
3. **d**. The page says `--permission-mode dontAsk` "runs reads and pre-approved tools and denies anything that would prompt, which fits a script", with `Read` and read-only git patterns. *b* is ruled out because auto mode "reviews actions instead of you" and runs most calls, not reads alone. *c* is ruled out because `acceptEdits` runs "Reads, file edits, and common filesystem commands", which is more than reads. *a* is ruled out because `bypassPermissions` is for "Isolated containers and VMs only", and it runs everything.
4. **b**. The table gives `acceptEdits` "Reads, file edits, and common filesystem commands", and shell commands that are not filesystem commands still ask. *a* is ruled out because plan mode "makes no edits until you approve" a plan and is for exploring. *c* is ruled out because `dontAsk` "denies anything that would prompt", and so it does not ask about shell work. *d* is ruled out because auto mode has "a second model, the classifier" review calls instead of asking, and it runs far more than edits.

</details>
