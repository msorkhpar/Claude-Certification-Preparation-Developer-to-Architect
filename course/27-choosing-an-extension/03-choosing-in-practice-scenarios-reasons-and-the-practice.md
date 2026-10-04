# Choosing in practice: scenarios, reasons and the practice

**Level:** Developer · **Module 27:** Choosing an extension · **Page 3 of 3**
**Exams:** DV5, DV7

**After this page you can** turn a situation into a few yes-or-no features, pick the mechanism that fits (an instruction file, a path-scoped rule, a skill, a subagent, a hook, an MCP server, a plugin, a built-in tool or a tool you define yourself), give the reason in one phrase, say what goes wrong with the tempting alternative, and write the decision function of the module's practice.

Checked on 2026-10-04 against the Claude Code documentation (Extend Claude Code, How Claude remembers your project, and the plugin pages on components and the manifest), which mention behaviour up to Claude Code v2.1.288. No Claude Code session was started for this page and nothing was installed: every statement about Claude Code is read from those pages. The decision function of the practice is the course's own model of the documented rules, not a feature of Claude Code, and the practice tests run offline in the course container, in Python, TypeScript, Java and Kotlin.

## Why it matters

Pages 1 and 2 gave the facts: what each mechanism loads, when, and what it costs. An exam scenario gives a need and four mechanisms, and three of them can all hold the sentence in question. The best answer is the one whose loading, isolation or guarantee matches the need, and each wrong answer fails in a specific way: a rule that Claude sometimes forgets, a reference that fills every request, a connection that a markdown file cannot make. This page practises the step from a situation to a choice, and from a choice to the reason.

## The idea

### Read a situation as features

A scenario is long and the decision rests on a few facts. Ask these questions of any situation, in this order:

| Question | If yes |
|---|---|
| Must it happen, or be blocked, every time with no judgment? | a hook |
| Does it need a system outside the machine, one that needs a connection or a login? | an MCP server |
| Would the work read or print a lot that nobody needs afterwards, only the conclusion? | a subagent |
| Is it something Claude must be told? Then what kind: a convention, reference material, or a procedure? | an instruction file or a path rule for a convention; a skill for the other two |
| Is it wanted in a second repository, and is it a skill, hook, subagent or server? | a plugin that carries it |
| Does a built-in tool already do it? | no extension at all |

The order is the point. A rule that must hold is a hook even when it also comes with a convention to state, because the documentation says "An instruction like "never edit `.env`" in CLAUDE.md or a skill is a request, not a guarantee." A connection to a database is an MCP server even when a procedure goes with it, because MCP "gives Claude purpose-built tools for an external system, with the connection and authentication handled by the server". Only after those two is the question about knowledge worth asking.

### Reasons, in one phrase

The practice names each choice with a reason code, and the codes are the vocabulary of an exam answer:

| Reason | What it says |
|---|---|
| `must-hold-every-time` | a hook always fires on its event, so the guarantee does not depend on Claude |
| `external-system` | MCP is how a session reaches a system that is not on the machine |
| `isolate-context` | a subagent does the reading in its own window and returns a summary |
| `scoped-convention` | a rule with `paths` loads when matching files are used |
| `always-known` | an instruction file is loaded every session, so it holds what Claude should always know |
| `on-demand-reference` | a skill's full content loads only when it is needed |
| `repeatable-procedure` | a skill is also the way to start a workflow with `/<name>` |
| `shared-setup` | a plugin bundles skills, hooks, subagents and servers for reuse |
| `built-in-covers` | the built-in tools cover most coding tasks |
| `own-schema-and-code` | in an application, you define the tool and run its code |
| `provided-schema` | in an application, the platform supplies the schema of a built-in tool |
| `remote-server` | in an application, a hosted server can be reached through the connector |

A skill with a slash name needs a word. The documentation says "Custom commands have been merged into skills. A file at `.claude/commands/deploy.md` and a skill at `.claude/skills/deploy/SKILL.md` both create `/deploy` and work the same way." The decision function has one answer for both, `skill`, and module 58 teaches the difference in the files.

> **Exam guide and current product.** *What a guide may state, and so what an exam may key:* a course on Claude Code can describe custom slash commands and skills as two separate mechanisms, with commands as saved prompts. *What the product does now (documentation checked 2026-10-04):* the two have merged, as quoted above, and a skill adds optional features such as a folder of supporting files. A scenario that offers "a custom command" and "a skill" for a repeatable procedure has one correct family; the page's answer is the skill, which is the form the documentation teaches.

### What goes wrong with the tempting choice

Every row of the bank below names the failure of the alternative. The failures fall into five kinds, and spotting the kind is faster than weighing the options:

- **Request instead of enforcement.** Anything written in text, CLAUDE.md, a skill or an output style, is followed as an instruction. The memory page says Claude "treats them as context, not enforced configuration. To block an action regardless of what Claude decides, use a PreToolUse hook instead."
- **Paid on every request.** An instruction file is "Persistent context loaded every conversation" and reference material in it is paid for on each request, so the guidance is to keep the file under 200 lines.
- **Not loaded when needed.** A skill is chosen by its description: "If descriptions are vague or overlap, Claude may load the wrong skill or miss one that would help." A convention that must always be known does not belong where it may be missed.
- **Added to the main window.** A skill "adds to your main window", so it floods the conversation with work that only a subagent keeps out.
- **Not carried.** "A `CLAUDE.md` at the plugin root isn't loaded as context", so a plugin cannot deliver a convention to another repository.

### Three worked examples

**One.** *Situation:* the team never wants `.env` or the lockfile edited, and a review found two edits last month. *Features:* must hold every time; a convention goes with it. *Choice:* a hook on `PreToolUse`, reason `must-hold-every-time`. *Why:* the hook fires on every matching call and can refuse it, and the sentence in CLAUDE.md can stay as the explanation Claude reads. *The tempting alternative:* a line in the instruction file. It is a request, and Claude may read it and still decide otherwise; the two edits last month are the proof. The practice tests this combination, because a function that checks the convention first returns the wrong answer.

**Two.** *Situation:* the support team wants Claude to query the replica of the ticket database, and also wants it to follow the team's query patterns. *Features:* an outside system; reference material. *Choice:* an MCP server, reason `external-system`, with a skill that holds the query patterns as a second, separate situation. *Why:* the documentation's pattern is "MCP provides the connection; a skill teaches Claude how to use it well". *The tempting alternative:* a skill alone with the connection string inside. A skill is knowledge. It cannot open the connection or handle the login, and a credential written in a markdown file is committed with the repository.

**Three.** *Situation:* six repositories need the same commit-message style, and also the same release skill. *Features, for the style:* a convention in six repositories. *Choice:* an instruction file, reason `always-known`, because a plugin cannot carry it. For the release skill the answer is a plugin, reason `shared-setup`. *The tempting alternative:* put both into a plugin. The style would never reach Claude's context, and the plugin would validate with a warning that tells you so.

### The scenario bank

Eighteen situations, with the choice, the reason and the failure of the alternative that tempts. The practice asks for the first two columns of each row, expressed as features.

| # | Situation | Choice | Reason | The tempting alternative fails because |
|---|---|---|---|---|
| 1 | Claude keeps running `npm` in a repository that uses `pnpm` | instruction file | `always-known` | a skill is loaded only if its description matches the task, so the mistake can repeat |
| 2 | No edit of `.env` or the lockfile, ever | hook | `must-hold-every-time` | a line in the instruction file is a request, not a guarantee |
| 3 | Run the linter after every file edit | hook | `must-hold-every-time` | a skill that says "lint after editing" is interpreted, and the outcome can vary |
| 4 | Handlers under `src/api` must validate input | path rule | `scoped-convention` | in the instruction file it is paid for in sessions that never touch the folder |
| 5 | A 3,000-line API style guide, needed when endpoints are written | skill | `on-demand-reference` | in the instruction file it is paid for on every request |
| 6 | The release checklist, pasted into chat for the third time | skill | `repeatable-procedure` | in the instruction file it loads every session though it is used once a week |
| 7 | Query the production replica for support tickets | MCP server | `external-system` | a skill is knowledge and cannot make the connection |
| 8 | Find why a test is flaky by reading sixty files; only the conclusion matters | subagent | `isolate-context` | a skill adds everything it reads to the main window |
| 9 | Rename a function in three files | built-in tool | `built-in-covers` | an extension adds cost for what the built-in tools already do |
| 10 | The ticket database wanted in six repositories, with its query procedure | plugin | `shared-setup` | copying files by hand lets the six copies drift apart |
| 11 | The changelog skill wanted in a second repository | plugin | `shared-setup` | a copy in the second repository has to be kept in step by hand |
| 12 | Post a message to the team channel when a session ends | hook | `must-hold-every-time` | an MCP server only offers tools that Claude may or may not call; nothing fires at the end |
| 13 | Read forty files and look up each ticket in the tracker | MCP server | `external-system` | a subagent alone cannot reach the tracker; once the server exists, a subagent can use it |
| 14 | The commit-message style, wanted in six repositories | instruction file | `always-known` | a plugin cannot carry it, since its instruction file is not loaded as context |
| 15 | A security-audit worker used in four repositories | plugin | `shared-setup` | defining the subagent four times lets the definitions drift |
| 16 | An application lets Claude look up an order by id in its own database | tool you define | `own-schema-and-code` | no Claude Code extension exists in an API call; the schema and the code are yours |
| 17 | An application lets Claude edit text files in a sandbox | built-in tool of the platform | `provided-schema` | writing your own schema repeats what the platform already defines |
| 18 | An application needs a vendor's hosted calendar server | remote server through the connector | `remote-server` | rebuilding it as your own tool repeats work and owns its login |

Rows 10, 11, 14 and 15 show the plugin rule: from the second repository on, a plugin is the carrier of a skill, hook, subagent or server, and only of those. Row 12 shows an order that surprises: the hook outranks the connection, and the hook can then use the server, as in the documentation's pattern "A hook triggers external actions through MCP".

### In an application

Rows 16 to 18 leave Claude Code. In an application that calls the Messages API there is no instruction file, skill or hook, and the choice is among three kinds of tool: one the platform supplies with its own schema, one a hosted server offers through the connector, and one you define. The decision keeps the same question, who owns the capability and who runs it. Module 26 covers the three kinds, and module 33 the connector.

## Traps

1. **Choosing by the noun in the scenario.** A scenario that says "database" does not mean MCP if the need is a note about the schema, and one that says "review" does not mean a subagent if the work is small. Ask the questions of the table, in order.
2. **A plugin for a convention.** The plugin carries skills, hooks, subagents and servers. An instruction file at its root is not loaded, so the convention needs its own file in each repository, or a personal one.
3. **Two answers, one chosen.** A scenario with an outside system and a procedure needs both an MCP server and a skill. The question asks for the one that provides what the situation lacks first, and that is the connection.

## The practice

The practice is in [`exercises/27-choosing-an-extension`](../../exercises/27-choosing-an-extension/unit-01/practice-1/statement.md). You write `choose`, which takes a situation described by the features above and returns the mechanism and the reason code, applying the rules in the order the page gives them. It is graded in Python, TypeScript, Java and Kotlin, offline; the statement lists seven cases, and the first holds the eighteen situations of the bank as features.

## Quiz

1. A team runs ESLint through a skill that asks Claude to lint after each change, and a review shows several edits that were never linted. What fits better?
   - **a**: A hook attached to the write event that executes the style check directly
   - **b**: A longer skill description with many more trigger words added to it
   - **c**: A line in the instruction file that repeats the same request again
   - **d**: A subagent that reviews the whole diff once at the end of each day

2. Six repositories need the same commit-message style. What does the page choose?
   - **a**: A plugin that carries the style to every project
   - **b**: An instruction file kept in each project
   - **c**: A hook that rewrites each message after the commit exists
   - **d**: A skill that each engineer starts before committing

3. A tool must let Claude read a vendor's hosted calendar from an application that calls the Messages API. Which choice does the page give?
   - **a**: A skill describing the vendor's endpoints
   - **b**: A tool the team defines, with its own schema and code
   - **c**: The remote server, reached through the connector
   - **d**: A hook that calls the vendor before each request

<details>
<summary>Answer key</summary>

1. **a**. The page says "Anything written in text, CLAUDE.md, a skill or an output style, is followed as an instruction", and the documentation's answer is that a hook "Always fires on its event". *b* is ruled out because "If descriptions are vague or overlap, Claude may load the wrong skill or miss one that would help", and a longer description is still a request. *c* is ruled out because the memory page says Claude "treats them as context, not enforced configuration". *d* is ruled out because a subagent "does the reading in its own window and returns a summary", and a daily review does not run the linter after each edit.
2. **b**. The page says "A `CLAUDE.md` at the plugin root isn't loaded as context", so a plugin cannot deliver a convention. *a* is ruled out because "a plugin cannot carry it, since its instruction file is not loaded as context", so the style would never reach Claude. *c* is ruled out because a hook "Always fires on its event", and a message rewritten afterwards does not tell Claude the style while it writes. *d* is ruled out because a skill is chosen by its description, and "If descriptions are vague or overlap, Claude may load the wrong skill or miss one that would help".
3. **c**. The page says that in an application a hosted server "can be reached through the connector", with the reason `remote-server`. *a* is ruled out because "a skill is knowledge and cannot make the connection", and the vendor's endpoints are not the connection. *b* is ruled out because rebuilding it as your own tool "repeats work and owns its login". *d* is ruled out because "no Claude Code extension exists in an API call", and a hook is one.

</details>

## Module quiz

This quiz covers all three pages of the module.

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

5. A platform team wants a note posted to the team chat whenever any Claude Code session closes, in forty repositories. Which design follows the order of questions?
   - **a**: A server for the chat service, which Claude may choose to call
   - **b**: An event-triggered script, delivered through one bundle
   - **c**: A skill with the posting steps, copied into each project
   - **d**: A line in each instruction file that asks Claude to write the note

<details>
<summary>Answer key</summary>

1. **b**. The page says a hook "Always fires on its event; the trigger is guaranteed". *a* is ruled out because with a skill, "Claude interprets the instructions; outcome can vary." *c* is ruled out because an instruction in the project file "is a request, not a guarantee". *d* is ruled out because "Claude follows both as instructions, so neither is enforced."
2. **a**. The page says "A plugin bundles skills, hooks, subagents, and MCP servers into a single installable unit", for "the same setup in several repositories, or to share it". *b* is ruled out because the trigger is "A second repository needs the same setup", which calls for a plugin. *c* is ruled out because CLAUDE.md is paid for on "every request", and the guidance is "Keep CLAUDE.md under 200 lines." *d* is ruled out because a subagent is "an isolated worker with its own context", not a packaging unit.
3. **a**. The page says to use `disable-model-invocation: true`: "This saves context and ensures only you trigger them." *b* is ruled out because "If descriptions are vague or overlap, Claude may load the wrong skill or miss one that would help", which is no control. *c* is ruled out because a line in CLAUDE.md "is a request, not a guarantee". *d* is ruled out because an output style sets "a voice, length or format wanted in every response", and says nothing about starting skills.
4. **c**. The page says reference material goes to skills, "which load on demand", while "descriptions at start, full content when used" keeps the cost low. *a* is ruled out because "CLAUDE.md is paid for on every request", and "Keep CLAUDE.md under 200 lines." *b* is ruled out because an output style is "a voice, length or format wanted in every response", not a library of facts. *d* is ruled out because MCP "connects Claude to external services", and a local guide is knowledge, not a service: "MCP for knowledge."
5. **b**. The page's table says "an MCP server only offers tools that Claude may or may not call; nothing fires at the end", so the event needs a hook, and "A hook triggers external actions through MCP". A plugin is how the same hook reaches many repositories: "A plugin bundles skills, hooks, subagents, and MCP servers into a single installable unit". *a* is ruled out because "an MCP server only offers tools that Claude may or may not call; nothing fires at the end". *c* is ruled out because "a skill that says "lint after editing" is interpreted, and the outcome can vary", and a copy in each project drifts. *d* is ruled out because "a line in the instruction file is a request, not a guarantee".

</details>
