# Two subagents and the tools each one gets

**Level:** Architect · **Module 73:** Scenario: developer productivity · **Page 1 of 2**
**Exams:** A2; A3; S4

**After this page you can** give an agent that explores a legacy system and an agent that generates boilerplate exactly the tools their jobs need, restrict file edits to one folder with a rule that Claude Code consults, let agents read an MCP server without letting them change it, and audit the three files that must agree: the servers, the subagents and the permission rules.

Checked on 2026-10-04 against the Claude Code documentation pages "Configure permissions", "Create custom subagents", "Connect Claude Code to tools via MCP" and "Best practices for Claude Code" (the pages name Claude Code version 2.1.286), and against the Architect exam guide (version 1.0, scenario 4). The example runs offline in Python, TypeScript, Java and Kotlin and reads two small folders of plain files; it does not start Claude Code or a server. This page is a capstone: it uses modules 47, 52, 54, 55 and 56 and puts them in the order the exam asks about.

## Why it matters

Scenario S4 of the Architect exam is a developer-productivity tool. It helps engineers explore unfamiliar codebases, understand legacy systems, generate boilerplate and automate repetitive tasks, with the built-in tools (Read, Write, Bash, Grep, Glob) and Model Context Protocol servers. Its primary domains are tool design and MCP integration, Claude Code configuration, and agentic architecture. The questions are about fit. Which tools belong to which job, which rule limits a write to one folder, which setting lets an agent read a ticket system without being able to close a ticket. The mistakes are in the details: a tool list left out, a rule written for the wrong tool, a server named in one file and missing in another.

## The idea

### The scenario in plain words

A team has a legacy system that nobody fully understands. It wants one helper that answers questions about how the system works and never changes it, a second helper that writes the boilerplate of a new module in a folder reserved for generated code, a documentation server and a ticket server that both helpers can reach, and a setup that any new developer gets with a clone and a few environment variables.

### The requirement decides the tools

| The requirement | The configuration | Why the tempting choice fails |
|---|---|---|
| Explore the legacy system and change nothing | A project subagent whose `tools` line lists only reading and searching tools: `Read`, `Grep`, `Glob` and the documentation search tool | Leaving the line out grants every tool. The shell tool can write files. A prompt sentence is a request, not a limit |
| Generate boilerplate, and only inside `src/generated` | A subagent with `Read`, `Glob`, `Edit` and `Write`, and one allow rule, `Edit(src/generated/**)` | A bare `Edit` rule approves every edit. A path rule written for `Write` is never consulted. A prompt that names the folder is a request |
| Read tickets, never create or delete them | Allow the get tool by name; deny the create and delete tools | An allow rule for the whole server approves the destructive tools. A deny rule with parentheses on an MCP tool is skipped |
| A token for the ticket server | `Bearer ${TICKETS_TOKEN}` in `.mcp.json`, each developer exports the variable | A literal token in the file is committed with it. A default value is a credential in the file |
| Keep `.env` out of every session | A deny rule, `Read(./.env)` | A sentence in a memory file is context, not enforcement |

The table is the capstone of four earlier modules, and each row is a sentence the documentation states. Here they are, in the order the example checks them.

### Tools are a list, and leaving it out is the widest list

A project subagent is a markdown file in `.claude/agents/` with a front matter that holds a `name`, a `description` and, optionally, `tools`. The `description` is how the main conversation decides when to delegate, so it says when to use the subagent and starts with "Use when". The `tools` line is the allowlist. A subagent that omits `tools` inherits every tool that is available to subagents, so leaving the line out grants the most, not the least. A sentence in the prompt is a request that the model weighs; the `tools` line is a list the subagent cannot go beyond. The shell tool can write files through redirections and commands, so a read-only agent has no use for it. A subagent works in its own context window and returns its result to the main conversation, which is the other reason to give exploration to one: the file contents it reads stay out of the main context. A longer memory file does not do that job: the root file loads in every session and costs context each time. Claude Code ships such a helper itself, the Explore subagent, which is read-only (module 67, "Exploring a large codebase", goes further); a project subagent of your own is the way to give the same shape the project's documentation server too.

### Writing in one folder: the rule must be about Edit

Permission rules are checked in a fixed order. In the documentation's words, "Rules are evaluated in order: deny, then ask, then allow." A matching deny rule wins over any allow rule, whatever the specificity. For files, an `Edit` rule applies to every built-in tool that edits files, so one `Edit(src/generated/**)` allow rule covers the `Edit` tool and the `Write` tool alike. A path rule written for the Write tool is accepted and never consulted, because file permissions are checked against Edit and Read path rules only; the documentation says to use `Edit(docs/**)` in place of `Write(docs/**)`. The path in a rule follows gitignore syntax and, for an allow rule, `src/generated/**` is anchored at the working directory, so it matches that folder and nothing else. A bare `Edit` rule has no path and approves every edit. Denying the shell does not limit the file tools, which have their own rules.

### MCP servers: one name in three places

A server is configured once, in the project's `.mcp.json`, and referred to in two other places by the same name. Its tools are written `mcp__<server>__<tool>`, in a subagent's `tools` line and in a permission rule. A rule can name a whole server (`mcp__tickets`), every tool of it (`mcp__tickets__*`) or one tool (`mcp__tickets__get_ticket`). A rule for an MCP tool that carries parentheses is skipped, so it matches nothing: an argument pattern is not a way to narrow an MCP tool. To make a server read-only for the agents, allow the reading tools by name and deny the others by name, and remember that a deny rule beats any allow.

The three places must agree. A subagent whose `tools` line names `mcp__wiki__search` when `.mcp.json` has no `wiki` server holds a reference that leads nowhere, and the model never finds out. Nothing in the files complains; a check that reads them does.

### Credentials come from the environment

The `.mcp.json` of a project is committed so that the team shares its servers. Expansion of `${VAR}` and `${VAR:-default}` works in a server's `command`, `args`, `env`, `url` and `headers`, so a token is written as a reference and each developer sets the variable in the shell. `${VAR:-default}` expands to the variable if it is set and to the default otherwise. A default is right for a value that is not secret, such as the ticket server's address, and wrong for a token: a default for a token would be a credential in the file. The project's servers load without a prompt in `claude -p` and SDK runs, so a pull request that changes `.mcp.json` deserves the review of a change to code (module 55).

### The audit: a checklist that reads the files

The example implements the checklist this course uses for the scenario, on two projects beside it: `project-before`, a draft that has the usual mistakes, and `project-after`, where every row of the table has its mechanism. For each project the audit reports:

- `literal-secret`: a header or environment entry whose name says token, key, secret or authorization and whose value has no `${` reference.
- `unknown-server`: a tool reference, in a subagent or in a rule, that names a server `.mcp.json` does not configure.
- `agent-inherits-all` and `agent-bare-bash`: a subagent with no `tools` line, or with the shell tool in it.
- `env-readable`: the settings do not deny `Read(./.env)`.
- `bare-write-allowed`: an allow rule that approves the whole `Edit` or `Write` tool.

For a project with no findings, the example prints the tools each subagent has.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the scenario's agent uses the built-in tools Read, Write, Bash, Grep and Glob and integrates MCP servers, and the questions about it ask which tool or server fits which job and where the configuration goes. *What the product does now (documentation checked 2026-10-04):* the file tools are Read, Edit and Write, and a permission rule for file edits is written for `Edit`, which covers Write too; a rule with a path for Write is accepted and never consulted. A subagent that lists `tools` is limited to them, and one that omits them inherits every tool. So on the exam, give each agent its narrow tool list and never rely on a prompt for a limit; in a real project, write the file rule for Edit.

### The example

The example loads each project and runs the checklist. It prints one line of facts per project, then the findings for the draft, and for the fixed project the tools of each subagent. The program and its output are the same in all four languages.

<!-- example: m73-setup-consistency tabs: python -->
<!-- /example -->

The draft has a literal token in the ticket header, an explorer that has the shell tool and names a `wiki` server nobody configured, a scaffolder with no `tools` line, a settings file that allows the whole `Write` tool and does not deny `.env`. The fixed project has none of those findings.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"One general helper with every tool, told in its prompt to explore only."** It is tempting because it is one file and the instruction is clear. The exam rejects it: a prompt is a request, and the tool list is the limit.
2. **"Allow the whole ticket server and let the agents decide what to call."** It is tempting because it is short. The exam rejects it: a whole-server rule approves the creating and deleting tools too.
3. **"Narrow the destructive tool with a pattern on its argument."** It is tempting because path rules narrow file tools. A rule for an MCP tool that carries parentheses is skipped, so it narrows nothing.
4. **"Give the token a default so that a fresh clone works."** It is tempting because the tools then work at once. The default is a credential in a committed file.

## Quiz

1. A team wants an agent that studies a legacy system and cannot change a file. What goes in its definition?
   - **a**: The shell tool, with a rule that blocks redirections
   - **b**: A prompt sentence that tells it never to modify a file
   - **c**: No tools line, as leaving it out gives the smallest set
   - **d**: A tools line that lists only reading and searching tools

2. A boilerplate agent may change files only in `src/generated`. Which settings give that?
   - **a**: An allow rule for the Edit tool, limited to that folder
   - **b**: An allow rule for the Write tool, limited to that folder
   - **c**: An allow rule for the whole Edit tool, plus a prompt naming the folder
   - **d**: A deny rule for the shell tool, since only commands can write

3. A ticket server lets a caller fetch, add and remove tickets, and agents may only look at them. Which settings give that?
   - **a**: Deny the delete tool with a rule that matches on the ticket number
   - **b**: Allow every tool of the server, and tell the agents to read only
   - **c**: Allow the get tool by name, and deny the create and delete tools by name
   - **d**: Deny the whole server, then allow its get tool by name

<details>
<summary>Answer key</summary>

1. **d**. The tools line is the limit. *b* is ruled out because a prompt does not limit tools: "A sentence in the prompt is a request that the model weighs". *c* is ruled out because the omission is the widest list: "leaving the line out grants the most, not the least". *a* is ruled out because the shell is a way to write: "The shell tool can write files through redirections and commands".
2. **a**. An Edit rule covers every built-in file-editing tool and a path keeps it to one folder. *b* is ruled out because a Write path rule is never read: "A path rule written for the Write tool is accepted and never consulted". *c* is ruled out because a bare rule has no path: "A bare `Edit` rule has no path and approves every edit." *d* is ruled out because the file tools have rules of their own: "Denying the shell does not limit the file tools".
3. **c**. Allow by name and deny the others by name. *b* is ruled out because a sentence is no limit: "A sentence in the prompt is a request that the model weighs". *a* is ruled out because the rule is skipped: "A rule for an MCP tool that carries parentheses is skipped, so it matches nothing". *d* is ruled out because a deny rule beats every allow: "A matching deny rule wins over any allow rule, whatever the specificity."

</details>

Adapted from the sample scenario of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the scenario is Anthropic's. The questions here are written for this course.
