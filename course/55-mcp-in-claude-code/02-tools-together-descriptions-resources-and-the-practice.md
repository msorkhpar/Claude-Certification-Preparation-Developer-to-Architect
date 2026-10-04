# Tools together, descriptions that steer, resources and the practice

**Level:** Architect · **Module 55:** MCP in Claude Code · **Page 2 of 2**
**Exams:** A2.4; S4

**After this page you can** say how the tools of several servers reach the model and what tool search changes, write a tool description that makes the model choose an MCP tool over a built-in one and survives truncation, decide between a tool and a resource for a catalog, choose between an existing server and a custom one, name the limits on what an MCP tool returns, and write the module's practice: the files that connect a team's servers.

Checked on 2026-10-03 against the Claude Code documentation page "Connect Claude Code to tools via MCP" (tool search, output limits, resources, permissions) and "Scale to many tools with tool search", the "Configure permissions" page of the Agent SDK documentation, and the MCP specification version 2026-07-28 (Tools and Resources), with `claude-agent-sdk` 0.2.163 and `@anthropic-ai/claude-agent-sdk` 0.3.287. The practice is a set of files graded by Python and TypeScript test suites, offline; the checks run on the course's model of the documented rules (`examples/55-mcp-config`), not on the product. This page deepens modules 32 and 33 (what a server declares) and the tool-design rules that module 52 teaches, and it does not repeat them.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* tools from all configured MCP servers are discovered at connection time and are available simultaneously to the agent; MCP resources expose content catalogs (issue summaries, documentation hierarchies, database schemas) to reduce exploratory tool calls; tool descriptions should be enhanced so that the agent does not prefer built-in tools such as Grep over more capable MCP tools; and existing community servers are chosen over custom ones for standard integrations such as Jira. *What the current product does (documentation checked 2026-10-03):* all configured servers are connected, but with tool search, which is on by default, "only tool names and server instructions load at session start", and a definition is loaded when Claude needs it, so the tools are available but not all in context at once. Servers connect in the background; when a request needs a server that is still connecting, Claude waits for it inside the tool-search call. `alwaysLoad` keeps a server's tools in context from the start. Descriptions and server instructions are truncated at 2,048 characters by default. On the exam, key "available simultaneously" and "descriptions steer selection"; in practice, put the decisive sentence first and keep `alwaysLoad` for the few tools used on every turn.

## Why it matters

Once a server is configured, the work moves to what the model sees of it. The exam's questions are the ones a production team asks: why does the agent keep using Grep when a better tool exists, why does it explore for five calls before a query, and how many servers is too many. In S4, an agent that explores unfamiliar codebases, the built-in tools and several MCP servers compete in the same turn, and the description is the only thing that tells the model which to prefer.

## The idea

### All servers, one set of tools

Every configured server contributes its tools, and the model chooses among all of them. Their names carry the server: a tool is called `mcp__<server>__<tool>`, so `search_docs` on the `docs` server is `mcp__docs__search_docs`, which is the name a permission rule or a hook matcher uses.

Where the guide says that tools are "available simultaneously", the product adds a layer. "Tool search keeps MCP context usage low by deferring tool definitions until Claude needs them. Only tool names and server instructions load at session start." The SDK page puts the reason in numbers: "Tool selection accuracy degrades with more than 30-50 tools loaded at once", and 50 tools "can use 10-20K tokens". Claude Code "doesn't impose a fixed per-server tool cap; the practical limit is your context window budget." Tool search is the default, with exceptions such as a non-first-party `ANTHROPIC_BASE_URL`, and `ENABLE_TOOL_SEARCH` turns it off, always on, or into a threshold (`auto:N`, a percentage of the context window). With fewer than about ten tools whose definitions fit comfortably, loading everything upfront is typically faster, because every search is an extra round trip.

`alwaysLoad: true` on a server loads all its tools at the start. It exists for "a small number of tools that Claude needs on every turn, since each upfront tool consumes context that would otherwise be available for your conversation". Putting it on every server defeats tool search. "Setting `alwaysLoad: true` also makes startup wait for the server's tools, capped at the standard 5-second connect timeout", so the wait is bounded. Two more facts matter for the exam's reliability questions. Servers can change their tool lists while connected (`list_changed`), and a failed refresh keeps the previous list. And a server that fails to connect is retried, so a missing tool can be a connection state and not a configuration error: `/mcp` shows the status and the tool count of each server.

### A description is what steers the model

The model picks a tool from its name and description, which module 52 treats as the main selection mechanism. Two facts are particular to MCP in Claude Code. "Claude Code truncates each tool description and each server's instructions at 2,048 characters by default", and the documentation's advice is "Keep them concise, and put critical details near the start." And the instructions field of a server "helps Claude understand when to search for your tools": what category of tasks the tools handle, when to search for them and what the server offers.

Take S4's case: the agent has Grep and a `search_docs` tool, and uses Grep for documentation questions because Grep is familiar. A description that says "Searches docs" gives the model no reason to switch. A description that begins "Use this instead of Grep to search the team's published documentation", says what it takes and returns, and says what it does not search, gives it one. Removing Grep is a blunt answer, because the agent then cannot search the code in the checkout. Making the server `alwaysLoad` makes the definition visible and does not say when to prefer it. The fix is wording, written with the decisive sentence first so that a truncation cannot remove it. The practice has you write that description.

### Tools and resources are different primitives

The specification gives the two different controllers. "Tools in MCP are designed to be **model-controlled**", so the model discovers and invokes them. "Resources in MCP are designed to be **application-driven**, with host applications determining how to incorporate context based on their needs." In Claude Code a resource is read by mentioning it: "Use the format `@server:protocol://resource/path`", for example `@postgres:schema://users`, and "Resources are automatically fetched and included as attachments when referenced". "Claude Code automatically provides tools to list and read MCP resources when servers support them", so the model can also read one on its own.

That makes a resource the right shape for a catalog: the issue summaries of a tracker, the page tree of a documentation site, the schemas of a database. The facts are small, stable and wanted before work starts, and a tool that lists them makes the model spend calls to learn what exists. A tool is the right shape for an action or a query whose arguments the model chooses. A hint on a tool does not change this: it describes the tool to a client and does not tell the model what exists. Nor does the scope: it decides where a server loads and not what it exposes.

### Existing servers first, and trust

For a standard integration the guide chooses an existing community server over a custom one, and keeps custom servers for team-specific workflows. The documentation points to reviewed connectors in the Anthropic Directory and attaches a condition: "Verify you trust each server before connecting it. Servers that fetch external content can expose you to prompt injection risk." The same warning applies to a project's `.mcp.json` (page 1): a server is code with access to your systems and a way into the model's context.

### Authority a job never needs

A server exposes what its author found useful, and a job needs a fraction of it. The GitHub server of the practice can delete a repository; an agent that reviews pull requests never should. Two designs follow from least privilege. First, do not hand over one general tool where several narrow ones would do: an MCP integration that exposes a whole database through a single query tool gives the model authority that cannot be scoped, since a permission rule can allow or deny a tool by name, and one tool that does every job leaves nothing to scope. Offer the reads the job needs as separate tools, and keep writes out. Second, remove what the job does not need with a control and not a request. A line in a memory file, a warning in a description or a reminder in a prompt is text that the model reads and weighs with everything else, so it is a request and not a control. A deny rule is a control: "If a deny rule matches, the tool is blocked, even in `bypassPermissions` mode." The rule names the tool as it appears to the model, `mcp__github__delete_repository`, and the specification adds the human check that stands behind it: "there SHOULD always be a human in the loop with the ability to deny tool invocations".

### What a tool may return

"Claude Code displays a warning when any MCP tool output exceeds 10,000 tokens", and "the default maximum is 25,000 tokens". The limit can be raised with `MAX_MCP_OUTPUT_TOKENS`, and a tool that declares `anthropic/maxResultSizeChars` in its `tools/list` entry sets its own threshold for text, up to 500,000 characters. Over the limit, "Claude Code saves it to a file and replaces it in the conversation with a message that names the file path". The limit concerns the size of one result and does nothing for the number of calls, so a catalog that is too big to return is paged by the server or served as a resource, and raising the number is the last resort.

### The practice: connect a team's servers

The practice is in [`exercises/55-mcp-in-claude-code`](../../exercises/55-mcp-in-claude-code/unit-01/practice-1/statement.md). You write a project `.mcp.json` of four servers (a GitHub server, a documentation server, a small core server and a schema catalog) with credentials by reference and defaults for what is not secret, the permissions that allow the read-only servers by name and deny a destructive tool, a user-scope example for a personal server, the description of the documentation tool, and the notes that read the catalog as a resource. It is graded by test suites in Python and TypeScript, offline, on files that are not code in any language, which is why it has no Java or Kotlin edition. The statement lists nine cases, and each says what you should see when it works.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Mark every server `alwaysLoad`, so that all the tools are available."** It is tempting because the guide says the tools are available simultaneously. The exam keys that sentence, and the product keeps definitions out of context until they are needed. Loading all of them at the start brings back what tool search was avoiding: crowded context and worse selection.
2. **"Write a long description that covers every detail."** It is tempting because the guide wants input formats, examples and boundaries. The exam rejects the length: Claude Code truncates at 2,048 characters, so what you put last may be cut. Put the boundary against the competing tool first.
3. **"Remove Grep, so that the agent has to use the MCP tool."** It is tempting because the agent stops preferring the wrong tool. The exam rejects it: the agent loses the ability to search the code. Rewrite the description to say when to use the MCP tool.
4. **"Give the schema a query tool that lists the tables."** It is tempting because every capability looks like a tool. The exam rejects it for a catalog: the model then spends calls to learn what exists. Expose the catalog as a resource, which can be attached to the prompt.
5. **"Build a custom server for Jira."** It is tempting because the team's workflow is its own. The exam rejects it for a standard integration: use an existing server, after checking that you trust it, and write a custom one for what only your team does.

## Quiz

1. An agent keeps answering documentation questions with Grep on the checkout, although the team's documentation server has a search tool whose description reads `Searches docs`. What is the best fix?
   - **a**: Make the documentation server load at the start so the definition is visible
   - **b**: Lengthen the description past the cut-off so that no detail is left out
   - **c**: Remove the competing tool so that only the documentation tool remains
   - **d**: Say when it beats pattern matching and what comes back, main point first

2. A support agent calls three listing tools before each database question, only to learn which tables exist. Which change cuts those calls?
   - **a**: Raise the output limit so that each listing returns more rows at once
   - **b**: Attach the schemas as a resource that can be mentioned in the prompt
   - **c**: Mark the listing tools as read-only so that they can run side by side
   - **d**: Move the database server into the user scope so that it loads everywhere

3. A team gives the agent its whole orders database through one MCP tool that runs any SQL it is given, and the job is to report on open orders. What is the architectural concern?
   - **a**: A result above the limit is lost, so the agent never sees the rows
   - **b**: The description is cut at the limit, so the agent cannot use it
   - **c**: Tool search hides the definition until a person asks for it
   - **d**: Access cannot be narrowed, because one entry point covers every task

<details>
<summary>Answer key</summary>

1. **d**. The description is what the model chooses by, so it must say when to prefer the tool, with the point first. *a* is ruled out because loading at the start only makes the definition visible: "makes the definition visible and does not say when to prefer it". *c* is ruled out because "Removing Grep is a blunt answer, because the agent then cannot search the code in the checkout". *b* is ruled out because "Claude Code truncates each tool description and each server's instructions at 2,048 characters by default".
2. **b**. A catalog is wanted before work starts, and a resource can be attached to the prompt so that no discovery calls are needed. *a* is ruled out because "The limit concerns the size of one result and does nothing for the number of calls". *c* is ruled out because a hint "does not change this: it describes the tool to a client and does not tell the model what exists". *d* is ruled out because "it decides where a server loads and not what it exposes".
3. **d**. A permission rule can allow or deny a tool by name, so one tool that does everything leaves nothing to scope. *a* is ruled out because an oversized result is kept: "Claude Code saves it to a file and replaces it in the conversation with a message that names the file path". *b* is ruled out because truncation is handled by wording: "put critical details near the start". *c* is ruled out because a definition is found by the model and not by a person: "a definition is loaded when Claude needs it".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S4, a developer-productivity agent. The agent explores unfamiliar codebases with the built-in tools and several MCP servers. A team's shared `.mcp.json` starts a documentation server with the argument `${CLAUDE_PROJECT_DIR}/tools/docs_server.py`, and on every machine the server fails to start with that text unexpanded in the path. What should the entry say?
   - **a**: A fallback after the name, written with a colon and a dash
   - **b**: An absolute location from the lead developer's own checkout, fixed for all
   - **c**: The same line placed in the user-level file, where the name is set
   - **d**: The name moved into the `env` map so that the process receives it

2. Scenario S4, a developer-productivity agent. The agent explores unfamiliar codebases with the built-in tools and several MCP servers. A CI job runs `claude -p` on a fresh clone, and a pull request has added a server to `.mcp.json` that nobody on the team has seen. What happens when the job starts?
   - **a**: It stays pending until a person approves it in an interactive session
   - **b**: It is skipped, because an unattended run has no trust dialog to accept
   - **c**: It loads with no prompt, so edits to the file need the same review as source changes
   - **d**: It loads only if the repository's own settings file approves it in advance

3. Scenario S1, a customer support resolution agent. The agent's backend tools come from twelve MCP servers listed in the shared file, and sessions start with crowded context. A developer reads that tools from all servers are available simultaneously and marks all twelve servers `alwaysLoad`. What is the effect?
   - **a**: Startup waits without limit for the servers to connect
   - **b**: All their definitions return to the prompt at launch, undoing deferral
   - **c**: Only the tools that the first request needs are loaded, the rest later
   - **d**: Each tool gets the name of its server added so that names stay unique

<details>
<summary>Answer key</summary>

1. **a**. The variable is set for the server and not for Claude Code, so a path in `command` or `args` needs a fallback. *d* is ruled out because the text in `args` would still have nothing to expand from: "so `${CLAUDE_PROJECT_DIR}` in a project file has nothing to expand from". *b* is ruled out because a personal location fails on every other machine: "A value that is not secret, such as a base URL or a path, should have a default". *c* is ruled out because moving the entry does not give the variable a value in Claude Code's own environment, and the documented answer is a default: "The documentation's answer is a default such as `${CLAUDE_PROJECT_DIR:-.}`".
2. **c**. Unattended runs cannot show the approval prompt. *a* is ruled out because the prompt needs a person: "In `claude -p` runs, Agent SDK sessions, and cloud sessions, Claude Code can't show that prompt: it loads project-scoped servers without asking." *b* is ruled out by the same sentence, which says such a run loads project-scoped servers and does not skip them: "it loads project-scoped servers without asking". *d* is ruled out because "approvals committed to the project's `.claude/settings.json` are ignored in a folder you have not trusted".
3. **b**. `alwaysLoad` brings every definition into the initial context. *a* is ruled out because the wait is bounded: "capped at the standard 5-second connect timeout". *c* is ruled out because loading only what a request needs is what tool search does, and the flag turns that off: "Putting it on every server defeats tool search." *d* is ruled out because naming already carries the server: "a tool is called `mcp__<server>__<tool>`".

</details>

Adapted from CLAUDE-CERTIFICATIONS by Amey Thakur (MIT License). Page quiz question 3 is that source's item on one general database tool, rewritten for this course.
