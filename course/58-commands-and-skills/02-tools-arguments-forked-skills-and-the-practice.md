# Tools, arguments, forked skills and the practice

**Level:** Architect · **Module 58:** Commands and skills · **Page 2 of 2**
**Exams:** A3.2; S2

**After this page you can** tell a pre-approval from a removal in a skill's frontmatter, fill a skill's text from the arguments typed after its name, run a skill in a separate subagent and know what that subagent sees, put each piece of guidance where it loads the way it is used, and write the module's practice.

Checked on 2026-10-03 against the Claude Code documentation page "Extend Claude with skills", documenting behaviour up to Claude Code v2.1.286. The practice is a set of files graded by Python and TypeScript test suites, offline, on the course's model of the documented rules (`examples/58-skill-model`); nothing in it starts Claude Code. This page deepens module 39 and module 56 (where the pre-approval point was first made) and does not repeat them. Names, locations and who may start a skill are on the first page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* it describes configuring `allowed-tools` in skill frontmatter "to restrict tool access during skill execution", for example to limit a skill to file writes and so prevent destructive actions; it describes `context: fork` as running a skill in an isolated sub-agent context so that verbose output does not pollute the main conversation; and it lists `argument-hint` as the prompt for required parameters. *What the current product does (documentation checked 2026-10-03):* `allowed-tools` lists "Tools Claude can use without asking permission during the turn that invokes this skill"; it "does not restrict which tools are available: every tool remains callable, and your permission settings still govern tools that are not listed". Restriction is a separate field, `disallowed-tools`, which removes tools "from Claude's available pool while this skill is active". `context: fork` and `argument-hint` behave as the guide says. On the exam, the guide's word "restrict" is the exam's word, so a question that offers `allowed-tools` as the restriction keys it; in a real skill, use `disallowed-tools` or deny rules for a restriction.

## Why it matters

The team's review skill must read a pull request and report, and must never change files. Someone writes `allowed-tools: Read Grep` and believes the skill is now read-only. It is not: Edit and Write remain callable, and the permission settings decide whether Claude is asked. The same confusion repeats with arguments (a placeholder that is spelled wrong silently becomes text) and with forks (a skill that holds only guidelines is handed to a subagent that has nothing to do). Each is a field that means something narrower than its name suggests.

## The idea

### Pre-approve, remove, deny

| Field or rule | What it does |
|---|---|
| `allowed-tools` | Pre-approves the listed tools for the turn that invokes the skill; the grant "clears when you send your next message"; nothing is restricted |
| `disallowed-tools` | Removes the listed tools from the available pool while the skill is active; it clears at the next message |
| A deny rule in permission settings | Blocks the tool or path whatever skill is running |

The patterns in `allowed-tools` are as exact as the rules in module 38: `Bash(git tag *)` pre-approves `git tag v1.2.0` and not `git push --force`, and a bare `Bash` pre-approves every shell command, which is rarely what a release skill should do. In `disallowed-tools`, which works "Like deny rules", only a bare name removes a tool: the permissions page says "A bare tool name like `Bash` removes the tool from Claude's context entirely", so `Edit` takes Edit away, while a scoped entry such as `Edit(src/**)` is a narrower rule that leaves the tool in place. A project skill's `allowed-tools` is applied even in a headless run in a folder never trusted, so review the field of every skill committed to a repository before you run Claude Code there; an organisation that sets `allowManagedPermissionRulesOnly` has the field ignored in project and personal skills.

So a read-only review skill is written in two lines: pre-approve what it needs (`Bash(gh pr view *) Bash(gh pr diff *)`) and remove what it must not have (`disallowed-tools: Edit Write`).

### Arguments

Text typed after the skill name is available to the body:

| Placeholder | Receives |
|---|---|
| `$ARGUMENTS` | All arguments, as typed |
| `$ARGUMENTS[N]`, `$N` | The argument at index N, counting from 0 |
| `$name` | A name declared in `arguments: [name, ...]`, by position |

Indexed arguments use shell-style quoting, so `/my-skill "hello world" second` makes `$0` expand to `hello world` and `$1` to `second`. An indexed placeholder with no argument "stays in the content unchanged", while a named one expands to an empty string. If the user types arguments and no placeholder receives them, Claude Code appends `ARGUMENTS: <your input>` to the end of the content, so Claude still sees them. `argument-hint` only shows the expected shape in autocomplete, such as `[pr-number]`; it fills nothing.

### A skill that runs in its own subagent

`context: fork` starts a new subagent of the type named in `agent` (the default is `general-purpose`) and gives it the skill content as its prompt. "The subagent doesn't see your conversation history, so the skill's instructions have to stand on their own." The name misleads: this is not a fork of the conversation, and when the task depends on the conversation, forking the conversation is the tool. The documentation also warns that `context: fork` "only makes sense for skills with explicit instructions": a body that holds guidelines such as "use these API conventions" gives the subagent no task, and it returns nothing useful. The reason to fork is the guide's: the work reads many files and the main conversation needs only the result. In a headless run the forked skill is waited for; interactively it runs in the background unless `background: false`.

### Where each piece of guidance goes

| The guidance is | It goes in | Because |
|---|---|---|
| Needed in every task of the project | The root memory file | It loads at launch |
| About one kind of file, wherever it sits | A scoped rule with `paths` | It loads on a match |
| A procedure you start on demand | A project skill | Its body loads only when invoked |
| A procedure with side effects | A skill with `disable-model-invocation: true` | A person decides the time |
| A personal variant of a team procedure | A personal skill under a new name | Personal wins over project on a shared name |

### The example

<!-- example: m58-skill-model tabs: python,typescript -->
```python
"""What a command or skill file means to Claude Code: its slash name, who may start it, which tools it pre-approves or removes, and how arguments fill its text.

The model follows the Claude Code skills documentation read on 2026-10-03 (v2.1.286): `.claude/commands/deploy.md` and `.claude/skills/deploy/SKILL.md` both create
`/deploy`; `allowed-tools` pre-approves for the turn and does not restrict; a bare name in `disallowed-tools` removes a tool while the skill is active;
indexed arguments use shell-style quoting; an invocation whose arguments no placeholder receives gets `ARGUMENTS: <input>` appended. Reading a file is plain data work.
"""
```
<!-- /example -->

### The practice: a forked review, a manual release and a command

The practice is in [`exercises/58-commands-and-skills`](../../exercises/58-commands-and-skills/unit-01/practice-1/statement.md). You write the review skill (forked, with an explicit task, pre-approving two commands and removing the edit tools), the release skill (manual, with a named argument and two pre-approved patterns), a command file that takes an argument, a personal variant with a new name, and a placement table for five pieces of guidance. It is graded by test suites in Python and TypeScript, offline, on files that are not code in any language, which is why it has no Java or Kotlin edition. The statement lists eight cases, and each says what you should see when it works.

## Traps

1. **"Give the review skill `allowed-tools: Read Grep` so that it cannot write."** It is tempting because the guide says the field restricts tool access. The exam keys the guide, and the product treats it as a pre-approval: Edit and Write stay callable. Removal is `disallowed-tools`, or a deny rule.
2. **"Write `disallowed-tools: Edit(src/**)` to take editing away from the skill."** It is tempting because the scoped form is how deny rules are written. It fails because only a bare name removes a tool; a scoped entry leaves Edit in the pool.
3. **"Fork the skill and write the API conventions in its body."** It is tempting because forking keeps the output out of the main conversation. It fails because a forked skill's body is its prompt, and guidelines without a task give the subagent nothing to do.
4. **"Add `argument-hint: [version]` and the version reaches the text."** It is tempting because the hint is shown where the user types the argument. It fails because the hint only shows the expected shape in autocomplete; the text needs a placeholder such as `$version` (declared with `arguments`) or `$0`, and a body with none gets `ARGUMENTS: <input>` appended, which leaves Claude to guess where the version belongs.

## Quiz

1. A skill's header lists `allowed-tools: Read Grep`, and its author expects editing to be impossible while the skill is active. What actually happens?
   - **a**: Editing is removed from the available tools until the next message
   - **b**: Editing is refused because only the two listed tools are permitted
   - **c**: Editing runs without any prompt because the skill vouches for it
   - **d**: Changing files remains allowed under the usual permission settings

2. A skill's text says only 'Use these naming conventions for new endpoints', and its header sets `context: fork`. What does the isolated helper do?
   - **a**: It reads the whole conversation and applies the conventions to it
   - **b**: It gets guidelines but no assignment, and returns nothing useful
   - **c**: It asks the user which endpoint the conventions concern
   - **d**: It runs inside the main conversation as if nothing were forked

<details>
<summary>Answer key</summary>

1. **d**. The field pre-approves and restricts nothing. *a* is ruled out because removal belongs to the other field, which removes tools "from Claude's available pool while this skill is active". *b* is ruled out because the field "does not restrict which tools are available: every tool remains callable". *c* is ruled out because "your permission settings still govern tools that are not listed".
2. **b**. A forked skill's body is its prompt, and guidelines without a task give the helper nothing to do. *a* is ruled out because the helper "doesn't see your conversation history". *c* is ruled out because the helper starts from the skill text alone, which "gives it the skill content as its prompt", so it has no one to ask. *d* is ruled out because the setting "starts a new subagent of the type named in" the agent field, so it does not stay in the conversation.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. A skill explains how an old billing system works, and Claude should consult it when relevant, while typing its name would do nothing useful. Which header line fits?
   - **a**: `disable-model-invocation: true`
   - **b**: `allowed-tools: Bash(git log *) Bash(git show *)`
   - **c**: `user-invocable: false`
   - **d**: `argument-hint: [system-name]` with `context: fork`

2. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. The team's review skill must be unable to change files for the turn that runs it. Which header line does that?
   - **a**: `allowed-tools: Read Grep Glob`
   - **b**: `disallowed-tools: Edit Write`
   - **c**: `argument-hint: [read-only]`
   - **d**: `disallowed-tools: Edit(src/**)`

3. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. A command text reads 'Summarise the commits by $ARGUMENTS since yesterday', and a developer runs `/standup ana`. What does Claude receive?
   - **a**: The sentence unchanged, with the name in a separate note
   - **b**: The sentence with a blank where the name belongs
   - **c**: The sentence with the name filled in and a line `ARGUMENTS: ana` after it
   - **d**: The sentence with the name filled in and nothing appended

<details>
<summary>Answer key</summary>

1. **c**. It hides the skill from the person and leaves it available to Claude, which suits background knowledge. *a* is ruled out because it does the opposite, so that only the person starts it and "the full skill loads only when you invoke it". *b* is ruled out because a pre-approval for git commands says nothing about who may start the skill, since the field "Pre-approves the listed tools for the turn that invokes the skill". *d* is ruled out because `argument-hint` "only shows the expected shape in autocomplete", and a fork would give a background note no task.
2. **b**. A bare name in the removal field takes the tool out of the pool while the skill is active. *a* is ruled out because the pre-approval field "does not restrict which tools are available". *c* is ruled out because `argument-hint` "only shows the expected shape in autocomplete". *d* is ruled out because "only a bare name removes a tool", so the scoped entry leaves editing in place.
3. **d**. The placeholder received the input, so it is substituted and nothing is added. *a* is ruled out because the placeholder takes "All arguments, as typed" and is replaced in the text. *b* is ruled out because a blank arises only for a named placeholder without an argument, since an indexed one "stays in the content unchanged", and here the input is given. *c* is ruled out because the extra line appears only when "no placeholder receives them", and this one did.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
