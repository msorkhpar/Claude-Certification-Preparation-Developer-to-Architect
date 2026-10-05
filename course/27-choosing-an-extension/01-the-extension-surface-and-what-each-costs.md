# The extension surface and what each piece costs

**Level:** Developer · **Module 27:** Choosing an extension · **Page 1 of 3**
**Exams:** DV5, DV7

**After this page you can** name the extension points of Claude Code, say what each one is for, say when each one loads into
context and what it costs there, and pick the feature that a stated trigger calls for.

Checked against the Claude Code documentation (Extend Claude Code) on 2026-10-03. This page is reading and quiz only: no code runs,
and the page describes the documented behaviour, not a run in the course container. Page 3 closes the module with a practice.

## Why it matters

Claude Code works well with nothing added, and teams still extend it: conventions, shared procedures, access to a database, a
check after every edit. Each extension solves a different problem, and each costs context in its own way. A team that puts everything in
one instruction file gets a slow, confused assistant. The exam gives a need and asks which feature fits.

## The idea

### The extension layer

The documentation says "The built-in tools cover most coding tasks. This guide covers the extension layer: features you add to
customize what Claude knows, connect it to external services, and automate workflows." The pieces:

| Feature | What it does | When to use it |
|---|---|---|
| CLAUDE.md | "Persistent context loaded every conversation" | project conventions and "always do X" rules |
| Output style | sets Claude's role, tone and response format for a whole session | a voice, length or format wanted in every response |
| Skill | "Instructions, knowledge, and workflows Claude can use" | reusable content, reference docs, repeatable tasks |
| Subagent | "Isolated execution context that returns summarized results" | context isolation, parallel tasks, specialized workers |
| MCP | connects Claude to external services | external data or actions |
| Hook | a script, HTTP request, MCP tool call, prompt or subagent run on an event | automation that must run on every matching event |
| Plugin | a bundle of skills, hooks, subagents and MCP servers | the same setup in several repositories, or to share it |

Two more appear in the same table: code intelligence, which connects Claude to a language server for symbol navigation and live type
errors, and dynamic workflows, scripts that Claude writes to run many subagents in the background. The course names them and does not
build with them.

The same choice exists one level down, in the Messages API. A tool you define yourself is a user-defined client tool: "You write the
schema, you execute the code, you return the results." A tool that a remote MCP server offers can be reached from the API through the
MCP connector, which lets you "Connect to remote MCP servers from the Messages API without a separate MCP client." A built-in or
Anthropic-schema tool such as `bash` or `text_editor` needs no schema of your own. The choice is the same question each time: who
owns the capability, who runs it, and what it costs to describe to the model on every request.

The sentence on skills is worth knowing: "Skills are the most flexible extension. A skill is a markdown file containing knowledge,
workflows, or instructions." A skill can be invoked with a command such as `/deploy`, or Claude can load it when it is relevant.
Plugins are "the packaging layer": "A plugin bundles skills, hooks, subagents, and MCP servers into a single installable unit", and
plugin skills are namespaced (like `/my-plugin:review`) so that several plugins can coexist.

### What each one costs in context

Every feature "consumes some of Claude's context". Too much "can also add noise that makes Claude less effective". The documentation's
table of loading:

| Feature | When it loads | What loads | Context cost |
|---|---|---|---|
| CLAUDE.md | session start | the full content | every request |
| Output styles | session start, and when you switch | the active style's full instructions | every request |
| Skills | session start, and when used | descriptions at start, full content when used | low; the descriptions are in every request |
| MCP servers | session start | tool names; full schemas on demand | low until a tool is used |
| Code intelligence | after file edits and on demand | diagnostics after edits; symbol locations on lookup | low; it reduces file reads elsewhere |
| Subagents | when spawned | a fresh context, with the skills it lists | isolated from the main session |
| Hooks | on trigger | nothing, since they run externally | zero, unless the hook returns context |

Three consequences follow. CLAUDE.md is paid for on every request, so "Keep CLAUDE.md under 200 lines." and move reference material to
skills, "which load on demand". A skill with side effects should use `disable-model-invocation: true`: "This saves context and ensures
only you trigger them." And a hook costs nothing in context unless its output is added as messages, which makes it the right place
for a linter run or a log line.

The same table says what happens to MCP when there are many tools. "Tool search is on by default, so idle MCP tools consume minimal
context." For a skill, the wording of its description matters: "Claude matches your task against skill descriptions to decide which
are relevant. If descriptions are vague or overlap, Claude may load the wrong skill or miss one that would help."

### Triggers: add a feature when the need shows up

The documentation advises "You don't need to configure everything up front." Each feature has a recognizable trigger:

| Trigger | Add |
|---|---|
| Claude gets a convention or command wrong twice | a line in CLAUDE.md |
| You keep asking Claude to be shorter or to answer in the same format | an output style |
| You keep typing the same prompt to start a task | a user-invocable skill |
| You paste the same playbook into chat for the third time | a skill |
| You keep copying data from a browser tab Claude cannot see | an MCP server |
| A side task floods your conversation with output you will not reference again | a subagent |
| You want something to happen every time without asking | a hook |
| A second repository needs the same setup | a plugin |

The paragraph after that table is a design rule: "A repeated mistake or a recurring review comment is a CLAUDE.md edit, not a
one-off correction in chat."

### Where features are defined

A feature can be defined at several levels: user-wide, per project, through plugins or through managed policies. The levels combine
in different ways. "CLAUDE.md files are additive": every level contributes at once. "Skills and subagents override by name": one
definition wins, by priority. "MCP servers override by name: local > project > user." And "Hooks merge: all registered hooks fire for
their matching events regardless of source."

## Traps

1. **Everything in CLAUDE.md.** It loads in full on every request. Reference material belongs in a skill, which loads on demand.
2. **An MCP server for what a skill can do.** MCP gives access to a system; if the need is knowledge or a procedure, a skill is the
   lighter choice. The next page compares them.
3. **A rule that must hold, written as a sentence.** An instruction is a request. Enforcement is a hook.

## Quiz

1. A team keeps pasting the same release checklist into the chat. Which feature does the documentation's trigger table name?
   - **a**: A line in the persistent project file, loaded in every session
   - **b**: A hook that fires on every edit
   - **c**: An output style that sets the tone of each reply
   - **d**: A skill, which holds the procedure and loads when invoked

2. A repository's CLAUDE.md has grown to 600 lines of endpoint documentation. What does the page advise?
   - **a**: Move the detail into skills, which load only when needed
   - **b**: Keep it where it is, because the file is read once per session
   - **c**: Turn the whole file into one subagent definition
   - **d**: Replace it with a hook that prints the reference on every request

3. Which feature costs no context unless its output is added to the conversation?
   - **a**: A skill whose description is loaded at the start of each session
   - **b**: An output style that applies to the whole session
   - **c**: A hook, which runs externally
   - **d**: A CLAUDE.md file with imports

<details>
<summary>Answer key</summary>

1. **d**. The trigger table says "You paste the same playbook into chat for the third time" calls for a skill. *a* is ruled out because that file is "Persistent context loaded every conversation", not a procedure kept for the moment it is needed. *b* is ruled out because a hook fits "automation that must run on every matching event", not a procedure to read and follow. *c* is ruled out because an output style is for "a voice, length or format wanted in every response".
2. **a**. The page says "Keep CLAUDE.md under 200 lines." and that reference material goes to skills, "which load on demand". *b* is ruled out because "CLAUDE.md is paid for on every request", not once per session. *c* is ruled out because a subagent is for "context isolation, parallel tasks, specialized workers", not for reference text. *d* is ruled out because a hook "costs nothing in context unless its output is added as messages", and one that prints the reference would add it to every event.
3. **c**. The table says a hook loads "nothing, since they run externally" and costs "zero, unless the hook returns context". *a* is ruled out because for a skill the "descriptions are in every request". *b* is ruled out because an output style loads "the active style's full instructions" with a cost on "every request". *d* is ruled out because "CLAUDE.md is paid for on every request", with its full content loaded at session start.

</details>
