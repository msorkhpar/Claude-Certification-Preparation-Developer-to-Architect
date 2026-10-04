# Permissions that bound the tools, exploring a codebase and the practice

**Level:** Architect · **Module 56:** The built-in tools · **Page 2 of 2**
**Exams:** A2.5; S4

**After this page you can** say which permission rule covers which built-in tool, write rules that actually bind an explorer, tell a pre-approval from a restriction in the SDK options and in a skill's frontmatter, explore an unfamiliar codebase one step at a time instead of reading it all, and write the module's practice: the settings, the agent file and the plan of an explorer.

Checked on 2026-10-03 against the Claude Code documentation pages "Tools reference" (permission rules and hooks for each tool), "Configure permissions" and "Give Claude custom tools" of the Agent SDK documentation, "Extend Claude with skills" and "Best practices for Claude Code", with `claude-agent-sdk` 0.2.163 and `@anthropic-ai/claude-agent-sdk` 0.3.287. The practice is a set of files graded by Python and TypeScript test suites, offline; the checks run on the course's models of the documented rules (`examples/38-settings-layers` and `examples/56-builtin-tools`), not on the product. This page deepens module 38 (permission rules), module 35 (tools and permissions in the SDK) and module 47 (what a subagent may use), and it does not repeat them. Skills themselves belong to modules 57 and 58.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* in its task on skills (3.2) it describes configuring `allowed-tools` in skill frontmatter "to restrict tool access during skill execution", for example limiting a skill to file writes to prevent destructive actions; and for built-in tools it describes a developer-productivity agent given Read, Write, Bash, Grep and Glob. *What the current product does (documentation checked 2026-10-03):* a skill's `allowed-tools` grants permission and does not restrict. It lists "tools Claude can use without asking permission during the turn that invokes this skill", and the grant clears when you send your next message; the page says "It does not restrict which tools are available: every tool remains callable, and your permission settings still govern tools that are not listed." To take tools away while a skill is active there is a separate field, `disallowed-tools`. The same holds in the SDK: `allowedTools` pre-approves, and a tool is made unavailable by omitting it from `tools` or by a bare name in `disallowedTools`. On the exam, if an option says `allowed-tools` restricts the tools of a skill, it is the guide's wording and the exam keys it; when you build the control, remove the tools, or deny them, and do not rely on a pre-approval list.

## Why it matters

A tool's permissions decide what an agent can do when the model is wrong, manipulated or merely curious, and the exam asks them as scenario questions about S4: how to let an explorer read widely and change nothing, how to keep it away from credentials, and why a rule that looks right protects nothing. The failures come from a small set of facts, and several of them are surprising. A rule written for the wrong tool name is never matched. A list that pre-approves tools does not remove the others. A permission on Grep is invisible in a session where searches are Bash calls. In S4 the agent also meets codebases it has never seen, so the page ends with how to explore one without drowning the context.

## The idea

### Which rule covers which tool

All tool names are written the same way in rules: `ToolName(specifier)`. The specifier depends on the tool, and "several tools share a format".

| Rule | Applies to |
|---|---|
| `Bash(npm run *)` | Bash (and Monitor) |
| `Read(~/secrets/**)` | Read, Grep, Glob and LSP |
| `Edit(/src/**)` | Edit, Write and NotebookEdit |
| `WebFetch(domain:example.com)` | WebFetch |

Four consequences follow, and each is a trap.

1. **A path rule for `Write` is never consulted.** "`Edit(path)` rules govern all built-in tools that write files, including `Write` and `NotebookEdit`; a `Write(path)` rule is never matched by the file permission checks." Write the rule as `Edit(...)`.
2. **Reading rules cover the search tools.** One `Read(secrets/**)` rule applies to Read, Grep and Glob. A rule is written under the name of its family, and the example's `rule_tool` gives the name a call is checked under.
3. **A read denial also stops writes.** "A `Read(...)` deny rule also blocks the Edit and Write tools on the same path, including creating a new file there, because both tools change content Claude has to be able to read back." And the other direction: "An `Edit(...)` allow rule also grants read access to the same path, so you don't need a matching `Read(...)` rule." So one `Read` deny is the way to protect a folder fully, and an `Edit` deny alone protects it from writes only.
4. **Deny is checked before ask and allow.** "If a deny rule matches, the tool is blocked, even in `bypassPermissions` mode." Because of the order, an explorer can be given a broad allow and a narrow deny, and the deny wins.

Bash rules are matched as written. A scoped rule such as `Bash(rm *)` leaves `Bash` available and denies calls that match it "as written", so other spellings, `/bin/rm` among them, "fall through to the permission mode". A bare `Bash` allow rule approves every command, and a list of read-only patterns such as `Bash(git log *)` approves only those. Even without any rule, `Bash` runs a built-in set of read-only commands without prompting (the tools reference says so for default mode), and Read, Grep and Glob prompt only for paths outside the working directories.

Hooks use another spelling. "Hook `matcher` fields use bare tool names, not the parenthesized rule format." The tool names are "the exact strings you use in permission rules, subagent tool lists, and hook matchers". A matcher of `Grep` sees Grep calls, and on a default Linux session there are none to see: "the searches reach your hooks and permission rules as `Bash` calls" (page 1).

### Pre-approval is not restriction

Three controls look alike and do different things.

| Control | What it does |
|---|---|
| `allowedTools` / `allowed_tools` | Pre-approves: the listed tools run without asking |
| `tools` (and omitting a tool from it) | Decides which tools exist |
| `disallowedTools` / `disallowed_tools` with a bare name | Removes the tool from Claude's context; a scoped rule such as `Bash(rm *)` leaves the tool and denies matching calls |

The documentation spells out the first: "Other tools not listed here still exist, and calls to them that need approval fall through to the permission mode and `canUseTool`", and for `bypassPermissions`: "Setting `allowed_tools=["Read"]` alongside `permission_mode="bypassPermissions"` still approves every tool, including `Bash`, `Write`, and `Edit`." The reason is the order: "Other unlisted tools are not matched by any allow rule and fall through to the permission mode, where `bypassPermissions` approves them." For a locked-down agent pair the list with a mode that never prompts, or take the tools away. A subagent's own `tools` list is the same idea for the agents of module 47: it lists what the subagent has, and a subagent that lists `Glob` or `Grep` and leaves out `Bash` has them back even on macOS and Linux.

A skill's `allowed-tools` is a pre-approval in the same sense, which the guide's wording hides (see the box above). Skills are the subject of modules 57 and 58; the point here is that no list of allowed tools is a wall.

### Exploring an unfamiliar codebase

The Architect guide asks for incremental understanding: start with Grep to find entry points, then use Read to follow imports and trace flows, and do not read all the files upfront. It adds one trick for wrapper modules: first identify all the names a module exports, then search for each name across the codebase, because a caller may use a wrapper's name and never the original's. The product's reason is the one behind most of its advice: "Claude's context window fills up fast, and performance degrades as it fills." Its list of common failures names the pattern: "The infinite exploration. You ask Claude to 'investigate' something without scoping it. Claude reads hundreds of files, filling the context." The fixes it gives are narrower scope, or subagents "so the exploration doesn't consume your main context", which is module 47's lesson.

A workable order for an explorer, which the practice checks:

A community guide gives the rule for the opposite mistake: "Do not use filename search to find code references inside files." A name pattern finds the files called `discount`, and the callers of `applyDiscount` are lines inside files with other names.

1. Find entry points with Grep (`def main`, route decorators, command handlers), asking for file paths first.
2. Find files by name with Glob when the question is about a kind of file.
3. Read the entry files and follow imports one hop at a time.
4. Before tracing a function through wrapper modules, list the exported names and search for each.
5. Write what you learned, with the file and line of every claim, to a notes file. The agent that writes notes needs `Edit` on that folder and nothing else.

When an Edit does not apply, the order of remedies is page 1's: a longer unique string, `replace_all`, and Read plus Write last.

### The practice: a read-mostly explorer

The practice is in [`exercises/56-the-built-in-tools`](../../exercises/56-the-built-in-tools/unit-01/practice-1/statement.md). You write the settings that let an explorer read and search but not change the source and not touch secrets, an explorer subagent that lists Read, Grep and Glob, the SDK options that make the search tools available and take away the tools that change things, and an exploration plan with the edit remedies in order. It is graded by test suites in Python and TypeScript, offline, on files that are not code in any language, which is why it has no Java or Kotlin edition. The statement lists eight cases, and each says what you should see when it works.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Deny `Edit(secrets/**)`; that protects the folder."** It is tempting because the folder is only meant not to change. The exam rejects it: reading and searching the folder still work. A `Read` deny is the one rule that stops reading, searching and writing.
2. **"Write the path rule for `Write`, since the agent writes files."** It is tempting because the tool is called Write. The exam rejects it: a `Write(path)` rule is never matched. File-writing tools are governed by `Edit(...)` rules.
3. **"List `Read`, `Grep` and `Glob` in `allowedTools`, and the agent cannot do anything else."** It is tempting because the list looks like a whitelist. The exam rejects it: the list pre-approves, and every other tool still exists and falls to the permission mode. Take the others away with `tools` or a bare `disallowedTools` name.
4. **"Give the skill `allowed-tools: Read` so that it cannot write."** It is tempting because the guide says the field restricts tool access. The exam keys the guide, and the product treats it as a pre-approval: every tool remains callable. Use `disallowed-tools` or deny rules to remove tools.
5. **"Ask the agent to investigate the service by reading its files, however many."** It is tempting because more reading looks like more certainty. The exam rejects it: the context fills and quality drops. Search for entry points, follow imports, and delegate wide questions to subagents.

## Quiz

1. A repository's settings deny `Read(./.env)`. The agent, trying to fix a bug, calls Edit on `.env`. What happens?
   - **a**: It is refused, since a read denial also blocks changes there
   - **b**: It proceeds, since only reading calls are matched by that rule
   - **c**: A person is asked, since no edit rule names that path
   - **d**: It proceeds, since the working directory counts as trusted

2. A team's audit hook has the matcher `Grep` and logs nothing in a default Linux session, though the agent searches constantly. What explains it?
   - **a**: Hook matchers apply only to tools that come from MCP servers
   - **b**: Hooks cannot observe the search tools on any platform
   - **c**: Its lookups run through the shell tool, so only a Bash entry sees them
   - **d**: A matcher takes the parenthesized rule form, so a bare tool name never fires

<details>
<summary>Answer key</summary>

1. **a**. A read denial covers the tools that change content at the same path. *b* is ruled out because "A `Read(...)` deny rule also blocks the Edit and Write tools on the same path, including creating a new file there". *c* is ruled out because a deny is checked before anything else: "Deny is checked before ask and allow". *d* is ruled out because "If a deny rule matches, the tool is blocked, even in `bypassPermissions` mode."
2. **c**. On macOS, Linux and WSL the default set has no Grep, and its job is done through the shell tool. *a* is ruled out because the tool names are "the exact strings you use in permission rules, subagent tool lists, and hook matchers". *b* is ruled out because "the searches reach your hooks and permission rules as `Bash` calls". *d* is ruled out because "Hook `matcher` fields use bare tool names, not the parenthesized rule format."

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S4, a developer-productivity agent. The agent explores unfamiliar codebases with the built-in tools. The team wants it unable to open, search or alter the folder `secrets/`. Which single entry in the settings file achieves it?
   - **a**: `Edit(secrets/**)` under deny
   - **b**: `Write(secrets/**)` under deny
   - **c**: `Bash(cat secrets/*)` under deny
   - **d**: `Read(secrets/**)` under deny

2. Scenario S4, a developer-productivity agent. The agent explores unfamiliar codebases with the built-in tools. The agent lists TypeScript sources in a very large repository with `**/*.ts` and receives a result carrying a truncation flag. What should it do next?
   - **a**: Narrow the search to the folder that the question concerns
   - **b**: Repeat the same request until the results change
   - **c**: Take the returned files as the complete set and proceed
   - **d**: Look up each file name through a content search instead

3. Scenario S4, a developer-productivity agent. The agent explores unfamiliar codebases with the built-in tools. Asked to investigate how authentication works, it reads hundreds of files, and its answers get worse as the run goes on. What fixes it?
   - **a**: Read the whole repository once and summarise it, so that nothing is missed
   - **b**: Raise the turn limit so that the agent can finish reading before it answers
   - **c**: Start from a search for entry points and follow imports one hop at a time
   - **d**: List every file name in the repository first, then open each of them

<details>
<summary>Answer key</summary>

1. **d**. A read denial stops reading and searching, and writes at the same path. *a* is ruled out because "`Edit(path)` rules govern all built-in tools that write files", which leaves reading and searching open. *b* is ruled out because "a `Write(path)` rule is never matched by the file permission checks". *c* is ruled out because a scoped Bash rule matches calls "as written", and other spellings "fall through to the permission mode", so it stops neither the search tools nor a different spelling.
2. **a**. A capped result is a window, and a narrower request brings the rest into view. *b* is ruled out because "Results are sorted by modification time and capped at 100 files", so the same request returns the same window. *c* is ruled out because "Claude sees a truncation flag in the result and can narrow the pattern", which says the set is incomplete. *d* is ruled out because "Grep finds lines inside files, and Glob finds the files themselves", so a content search is the wrong tool for finding files.
3. **c**. The cause is the context filling with files that do not matter, and the remedy is to scope the reading. *a* is ruled out because reading everything is the failure itself: "The infinite exploration. You ask Claude to 'investigate' something without scoping it. Claude reads hundreds of files, filling the context." *b* is ruled out because more turns make the run longer and not better: "Claude's context window fills up fast, and performance degrades as it fills." *d* is ruled out because a name search returns a capped list and opening each file repeats the first failure: "If the cap is hit, Claude sees a truncation flag in the result and can narrow the pattern".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
