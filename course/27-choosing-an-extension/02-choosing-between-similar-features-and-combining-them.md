# Choosing between similar features and combining them

**Level:** Developer · **Module 27:** Choosing an extension · **Page 2 of 2**
**Exams:** DV5, DV7

**After this page you can** tell a skill from a subagent, CLAUDE.md from a skill, a hook from a skill, MCP from a skill, and a
subagent from a dynamic workflow; say which of them can enforce a rule and which can only request it; and combine features the way the
documentation's patterns do.

Checked against the Claude Code documentation (Extend Claude Code) on 2026-10-03. This module is reading and quiz only.

## Why it matters

Several of the features can hold the same instruction, and a reasonable-looking choice can be wrong in a way that shows only later: a
guardrail that Claude sometimes forgets, a reference file that fills every request, an MCP server for what was only a procedure. The
exam gives a short scenario and asks which feature, and the reason is always a difference in loading, isolation or guarantee.

## The idea

### Skill or subagent

"Skills are reusable content you can load into any context. Subagents are isolated workers that run separately from your main
conversation." The deciding questions are about the context window:

| Aspect | Skill | Subagent |
|---|---|---|
| What it is | reusable instructions, knowledge or workflows | an isolated worker with its own context |
| Key benefit | "Share content across contexts" | context isolation: "only summary returns" |
| Context window impact | "Adds to your main window" | "Uses a separate window with its own input and output tokens" |
| Best for | reference material, invocable workflows | tasks that read many files, parallel work, specialized workers |

Use a subagent "when you need context isolation or when your context window is getting full": it may read dozens of files while the
main conversation receives a summary. They also combine: a subagent can preload skills through its `skills:` field, and a skill can run
in isolated context with `context: fork`. A skill can be a reference (an API style guide) or an action (`/deploy`).

### CLAUDE.md or skill

Both store instructions, and they differ in loading. CLAUDE.md loads "Every session, automatically"; a skill loads "On demand". Only a
skill can trigger a workflow "with `/<name>`". "Put it in CLAUDE.md if Claude should always know it: coding conventions, build commands,
project structure, "never do X" rules." "Put it in a skill if it's reference material Claude needs sometimes (API docs, style guides)
or a workflow you trigger with `/<name>`." A middle layer exists: `.claude/rules/` files can be "scoped to file paths", so that
a language-specific guideline loads only when matching files are opened.

### Output style or CLAUDE.md

"CLAUDE.md carries what Claude should know, and an output style sets how Claude responds." Facts and rules about the project go in
CLAUDE.md. A role, tone or response format that you might want off again goes in an output style, of which one is active at a time. The
two combine, and the documentation adds a warning that applies to both: "Claude follows both as instructions, so neither is enforced."

### Hook or skill

A hook and a skill differ in what happens when the event comes. A hook "Always fires on its event; the trigger is guaranteed". With a
skill, "Claude interprets the instructions; outcome can vary." The documentation's guidance:

- **Use a hook** "when the action must happen the same way every time and doesn't need Claude to think": formatting on save, rejecting
  `rm -rf /`, a Slack message when a session ends.
- **Use a skill** "when Claude should decide how to apply the steps, or when the content is knowledge rather than a script".
- "**Put guardrails in hooks.**" An instruction such as "never edit `.env`" in CLAUDE.md or a skill "is a request, not a guarantee. A
  `PreToolUse` hook that blocks the edit is enforcement."

The effects line up: a hook's output lands in context as text Claude reads (a linter's results), and a `/fix-lint` skill tells Claude how
to resolve them. The two feed each other.

### MCP or skill

"MCP connects Claude to external services. Skills extend what Claude knows, including how to use those services effectively." They
provide different things: MCP provides "Tools and data access"; a skill provides "Knowledge, workflows, reference material". "These
solve different problems and work well together": MCP "gives Claude purpose-built tools for an external system, with the connection and
authentication handled by the server", and a skill can hold the team's database schema and query patterns.

### Subagent or dynamic workflow

Both do work outside the main conversation. "With subagents, Claude decides turn by turn what runs next. In a workflow, the script
decides." Use a subagent for "a quick, focused worker". Use a dynamic workflow when a job "outgrows a handful of subagents", or when
findings should be cross-checked before you see them, such as a codebase-wide audit or a large migration.

### Combine features

"Each extension solves a different problem: CLAUDE.md handles always-on context, skills handle on-demand knowledge and workflows, MCP
handles external connections, subagents handle isolation, and hooks handle automation." The patterns from the documentation:

| Pattern | How it works | Example |
|---|---|---|
| Skill + MCP | MCP provides the connection; a skill teaches how to use it well | MCP connects to the database, a skill documents the schema |
| Skill + Subagent | a skill spawns subagents for parallel work | an `/audit` skill starts security, performance and style subagents |
| CLAUDE.md + Skills | always-on rules in one, reference material loaded on demand in the other | CLAUDE.md says "follow our API conventions", a skill holds the guide |
| Hook + MCP | a hook triggers external actions through MCP | a post-edit hook sends a Slack notification |

### A decision aid

A short chain of questions covers most scenarios:

1. Must it happen every time, with no judgment? A hook.
2. Does Claude need to know it in every session? CLAUDE.md, kept short; a rule file if it depends on paths.
3. Is it knowledge or a procedure needed sometimes? A skill.
4. Is it a connection to a system outside the machine? MCP, with a skill for how to use it well.
5. Would the work flood the conversation? A subagent, or a workflow when it outgrows a few.
6. Does another repository need all of it? A plugin.

## Traps

1. **Guardrail as a prompt.** A sentence in CLAUDE.md or a skill is a request. If the rule must hold, use a hook.
2. **A skill where isolation is needed.** A skill "adds to your main window". A task that reads dozens of files belongs in a subagent.
3. **MCP for knowledge.** An MCP server connects to a system. Procedure and reference material live in a skill.

## Quiz

1. A team must make sure that Claude never touches a file named `.env`. Which feature gives the guarantee?
   - **a**: A line in CLAUDE.md that forbids the edit
   - **b**: A hook on the `PreToolUse` event that rejects it
   - **c**: A skill that explains why the file is private
   - **d**: An output style that tells Claude to refuse

2. A research task reads dozens of files and only the conclusions matter to the main conversation. Which feature fits?
   - **a**: A line in CLAUDE.md, which is loaded in full at the launch of each session
   - **b**: A skill, which adds everything that it reads to the main window as well
   - **c**: A hook, which runs a script once per session when the work starts
   - **d**: A subagent, which works in its own window and hands back only a summary

3. A team connects Claude to its database and wants the model to use the tables and queries well. What does the page suggest?
   - **a**: Only an MCP server, since it provides everything that the work needs
   - **b**: MCP for access, and a skill for the schema and patterns
   - **c**: Only a skill, since a skill can run the queries itself
   - **d**: A plugin, because plugins replace both of them

<details>
<summary>Answer key</summary>

1. **b**. The page says "A `PreToolUse` hook that blocks the edit is enforcement." *a* is ruled out because such an instruction "is a request, not a guarantee". *c* is ruled out because with a skill, "Claude interprets the instructions; outcome can vary." *d* is ruled out because "Claude follows both as instructions, so neither is enforced."
2. **d**. The page says to use a subagent "when you need context isolation or when your context window is getting full". *b* is ruled out because a skill "adds to your main window". *c* is ruled out because a hook is for "when the action must happen the same way every time and doesn't need Claude to think". *a* is ruled out because the page says "Put it in CLAUDE.md if Claude should always know it", and conclusions of one task are not such a rule.
3. **b**. The page says "These solve different problems and work well together", with MCP for the connection and a skill that "can hold the team's database schema and query patterns". *a* is ruled out because MCP provides "Tools and data access", not knowledge of the schema and patterns. *c* is ruled out because a skill provides "Knowledge, workflows, reference material", and the connection to the external system comes from MCP. *d* is ruled out because "Each extension solves a different problem", and a plugin only packages them.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A team wants a linter to run after every file edit, with no reliance on Claude remembering. Which feature fits, and why?
   - **a**: A skill, because Claude reads it before each change
   - **b**: A hook, because it fires on its event and the trigger is guaranteed
   - **c**: A line in the project file, because it is read at the start of a session
   - **d**: An output style, because it applies to the whole session

2. A team uses the same set of skills, hooks and MCP servers in six repositories. What does the documentation suggest?
   - **a**: Package them as one plugin that each repository installs
   - **b**: Copy the files into each repository by hand and keep them in step
   - **c**: Put the whole set into one long CLAUDE.md in each repository
   - **d**: Convert every item into a subagent that is spawned for each task

3. A workflow that deploys to production must never start unless a person types its name. Which setting does the page give?
   - **a**: `disable-model-invocation: true`, which hides it until you invoke it yourself
   - **b**: A longer description, which makes Claude hesitate before it considers using it
   - **c**: A line in CLAUDE.md that asks Claude to wait for a person to approve the run
   - **d**: An output style that makes every reply shorter and more formal for the user

4. A project has an API style guide of 3,000 lines that matters only when endpoints are written. Where does it belong?
   - **a**: In CLAUDE.md, so that it is available in every session
   - **b**: In an output style, so that it shapes each response
   - **c**: In a skill, which loads the full content only when used
   - **d**: In an MCP server, so that Claude can open it as a connection

<details>
<summary>Answer key</summary>

1. **b**. The page says a hook "Always fires on its event; the trigger is guaranteed". *a* is ruled out because with a skill, "Claude interprets the instructions; outcome can vary." *c* is ruled out because an instruction in the project file "is a request, not a guarantee". *d* is ruled out because "Claude follows both as instructions, so neither is enforced."
2. **a**. The page says "A plugin bundles skills, hooks, subagents, and MCP servers into a single installable unit", for "the same setup in several repositories, or to share it". *b* is ruled out because the trigger is "A second repository needs the same setup", which calls for a plugin. *c* is ruled out because CLAUDE.md is paid for on "every request", and the guidance is "Keep CLAUDE.md under 200 lines." *d* is ruled out because a subagent is "an isolated worker with its own context", not a packaging unit.
3. **a**. The page says to use `disable-model-invocation: true`: "This saves context and ensures only you trigger them." *b* is ruled out because "If descriptions are vague or overlap, Claude may load the wrong skill or miss one that would help", which is no control. *c* is ruled out because a line in CLAUDE.md "is a request, not a guarantee". *d* is ruled out because an output style sets "a voice, length or format wanted in every response", and says nothing about starting skills.
4. **c**. The page says reference material goes to skills, "which load on demand", while "descriptions at start, full content when used" keeps the cost low. *a* is ruled out because "CLAUDE.md is paid for on every request", and "Keep CLAUDE.md under 200 lines." *b* is ruled out because an output style is "a voice, length or format wanted in every response", not a library of facts. *d* is ruled out because MCP "connects Claude to external services", and a local guide is knowledge, not a service: "MCP for knowledge."

</details>
