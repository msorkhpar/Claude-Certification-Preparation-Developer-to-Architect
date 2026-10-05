# Skills, and how they compare with the other ways to extend Claude Code

**Level:** Developer · **Module 39:** Extending Claude Code · **Page 1 of 5**
**Exams:** DV1, DV7; A3.2

**After this page you can** write a skill, say where skill files live and which one wins when two share a name, decide who may start a skill and which tools it pre-approves, choose between a skill, a memory file, a subagent, an MCP server and a hook, and diagnose a skill that does not trigger or triggers too often.

Checked on 2026-10-03 against the Claude Code documentation (skills, features overview and commands), which mention behaviour up to Claude Code v2.1.286. Nothing here was run against a live session. The checks on skill files in the module's example and practice use the course's own model of the documented rules, and run offline.

## Why it matters

A skill is the cheapest way to teach Claude Code a repeatable job, and the exam asks you to place it among the other extension points. The common mistakes are choosing the wrong extension for the job (a memory line where a hook was needed), writing a description that never matches, and pre-approving more than the job needs.

## The idea

### What a skill is

A skill is a folder with a `SKILL.md` file. "Every skill needs a `SKILL.md` file with two parts: YAML frontmatter between `---` markers that tells Claude when to use the skill, and markdown content with the instructions Claude follows when the skill runs." The directory name, or the frontmatter `name` when you set one, becomes the command you type. A skill can be started in two ways: you type `/name`, or Claude loads it by itself when your request matches its description.

Custom commands are the same thing in an older shape. "Custom commands have been merged into skills." A file at `.claude/commands/deploy.md` and a skill at `.claude/skills/deploy/SKILL.md` both create `/deploy`, and the old files keep working. Skills add a directory for supporting files and fields that control invocation.

### Where skills live

| Location | Path | Who gets it |
|---|---|---|
| Enterprise | `.claude/skills/<name>/SKILL.md` in the managed settings directory | All users on machines where the organisation deploys it |
| Personal | `~/.claude/skills/<name>/SKILL.md` | All your projects on this machine |
| Project | `.claude/skills/<name>/SKILL.md` | Sessions in this repository: "Commit it so your team gets it too" |
| Nested | `<subdir>/.claude/skills/<name>/SKILL.md` | Sessions started in or below that folder |
| Plugin | the plugin's `skills/` folder | Where the plugin is enabled |

When two skills share a name, the location decides: "Enterprise over personal, and personal over project." A skill in a subdirectory below where you started does not load at startup. It loads the first time Claude reads or edits a file there.

### The frontmatter that matters

| Field | Effect |
|---|---|
| `name` | The command name. Defaults to the folder name |
| `description` | "What the skill does and when to use it. Claude uses this to decide when to apply the skill." |
| `when_to_use` | More trigger phrases, appended to the description |
| `argument-hint` | A hint such as `[issue-number]` shown in autocomplete |
| `disable-model-invocation` | `true` stops Claude from starting it by itself |
| `allowed-tools` | Tools Claude may use without asking while the skill runs |
| `model`, `context: fork` | Run the skill on another model, or in its own subagent |

Two limits are worth remembering. The description is read often and is therefore short: "the combined `description` and `when_to_use` text is truncated at 1,536 characters in the skill listing to reduce context usage", so "Put the key use case first". And frontmatter is only read "when the opening `---` is the file's first line". If the YAML does not parse, the skill still loads with no fields set, and `/skill-name` still works, but Claude cannot match your request against a description it does not have.

### Who starts it and what it may do

Invocation is a safety decision. A skill that writes release notes is harmless and Claude can start it when asked. A skill that tags a release and calls `gh release create` has side effects, and a person should start it: add `disable-model-invocation: true`. The documentation says of such task skills: "These are often actions you want to invoke directly with `/skill-name` rather than letting Claude decide when to run them."

`allowed-tools` pre-approves tools for the turn that runs the skill, and "The grant clears when you send your next message." Pre-approve patterns and not whole tools. `allowed-tools: Bash(git tag *) Bash(gh release create *)` approves two command families. `allowed-tools: Bash` approves every shell command for that turn, which turns a narrow skill into an open one. The grant goes "through the normal permission flow", so a deny rule still wins.

### Which extension for which job

The features overview puts the choices side by side.

| Feature | What it does | Use it when |
|---|---|---|
| CLAUDE.md | Persistent context loaded every conversation | Project conventions, "always do X" rules |
| Skill | Instructions, knowledge and workflows Claude can use | Reusable content and repeatable tasks |
| Subagent | An isolated context that returns summarised results | A side task that would flood the conversation |
| MCP | Connects to external services | External data or actions |
| Hook | A script triggered by events | Automation that must run on every matching event |

The same page gives triggers for adding each: add to CLAUDE.md when "Claude gets a convention or command wrong twice", capture a skill when "You paste the same playbook or multi-step procedure into chat for the third time", route a side task through a subagent when it "floods your conversation with output you won't reference again", and write a hook when "You want something to happen every time without asking". The line between a skill and a hook is the one the exam likes: a skill is instructions Claude may follow, and a hook is a program that always runs. A rule that must never be broken belongs in a hook or a deny rule, and a procedure belongs in a skill.

### Troubleshooting a skill

When Claude does not use a skill, work down the list the documentation gives: check that the description "includes keywords users would naturally say", verify the skill appears in the list of available skills, try rephrasing the request to match the description, and invoke it directly with `/skill-name`. When Claude uses a skill that you did not want, make the description more specific, or set `disable-model-invocation: true` if you only want manual invocation. Malformed frontmatter is the quiet cause of the first problem. Run with `--debug` to see the parse error, or run `claude plugin validate` on the skills directory.

## Traps

1. **Writing a description that says what the skill is and not when to use it.** Claude matches your request against the description, so it needs the words a user would say.
2. **Pre-approving bare `Bash`.** A skill's `allowed-tools` should list patterns, as `Bash(git tag *)` does.
3. **Putting a hard rule in a skill.** A skill is guidance Claude may follow. What must always happen belongs in a hook.
4. **Leaving side effects on automatic invocation.** A skill that publishes or deletes should set `disable-model-invocation: true`.

## Quiz

1. A description reads only "Release helper", and Claude never loads that skill when users ask for a changelog. What is the first fix?
   - **a**: Raise the tool grant so that the skill may run any shell command
   - **b**: Move the folder into the enterprise location so it takes priority
   - **c**: Add the value true on the model-invocation field in the frontmatter
   - **d**: State what it does and when to use it, in everyday words

2. A team requires that no agent ever pushes to the main branch. Which extension fits best?
   - **a**: A hook that blocks the command, because it always runs and cannot be skipped
   - **b**: A line in the project memory file that forbids pushing to that branch
   - **c**: A skill whose instructions tell Claude never to push to the main branch
   - **d**: A subagent that reviews each command before Claude is allowed to run it

3. A skill tags a release and creates a GitHub release. Which frontmatter is right?
   - **a**: Automatic invocation on, with the whole Bash tool pre-approved
   - **b**: Automatic invocation on, with the two command patterns pre-approved
   - **c**: Automatic invocation off, with two command patterns pre-approved
   - **d**: Automatic invocation off, with the whole Bash tool pre-approved

<details>
<summary>Answer key</summary>

1. **d**. The page says the description is "What the skill does and when to use it. Claude uses this to decide when to apply the skill." *b* is ruled out because location decides which skill wins when "two skills share a name", and has no bearing on whether Claude matches a request. *c* is ruled out because the table says `true` "stops Claude from starting it by itself", which is the opposite of what the team wants. *a* is ruled out because a tool grant is for "Tools Claude may use without asking while the skill runs", and a skill that is never loaded never runs.
2. **a**. The page says "A rule that must never be broken belongs in a hook or a deny rule", and a hook, unlike guidance, is "a program that always runs". *c* is ruled out because "a skill is instructions Claude may follow". *b* is ruled out because a memory file is guidance of the same kind, and the page says "Persistent context loaded every conversation" is for conventions and "always do X" rules. *d* is ruled out because a subagent is "An isolated context that returns summarised results" for side tasks, and not a gate that every command must pass.
3. **c**. The page says a task skill with side effects should set `disable-model-invocation: true`, and that `allowed-tools: Bash(git tag *) Bash(gh release create *)` "approves two command families". *a* is ruled out because the page says bare `Bash` "approves every shell command for that turn". *b* is ruled out because "a person should start it" when a skill has side effects. *d* is ruled out because it keeps the person in control but the page says bare `Bash` "turns a narrow skill into an open one".

</details>
