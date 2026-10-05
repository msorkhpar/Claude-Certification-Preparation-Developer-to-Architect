# Sessions, environment variables and the practice

**Level:** Architect · **Module 73:** Scenario: developer productivity · **Page 2 of 2**
**Exams:** A1; A3; S4

**After this page you can** decide whether to resume a session, fork it or start fresh with a summary, write the team note that makes a shared setup startable by a new developer, and correct a draft setup in the module's practice: five files, graded by tests, with no program to write.

Checked on 2026-10-04 against the Claude Code documentation pages "Configure permissions", "Connect Claude Code to tools via MCP" and "Best practices for Claude Code", and against the Architect exam guide (version 1.0, scenario 4 and its task statement on session management). The practice is offline in Python, TypeScript, Java and Kotlin: the tests read the files you write, and nothing starts Claude Code or a server. The practice's checks are this course's own, built on the documented behaviour, and the statement says so.

## Why it matters

Page 1 configured what the helpers may do. The other half of developer productivity is what happens over days: the investigation that continues tomorrow, the analysis that two designs start from, the code that changed overnight, and the new colleague who clones the repository and wants to start. The exam asks these as judgement questions with a situation in the stem, and the answer is one of three session choices. A setup that works only on the machine of the person who wrote it is not shared, so the second half of the page is the note that closes the gap.

## The idea

### Resume, fork or start fresh

A new session starts with a fresh context window and none of the earlier conversation. Module 51 gives the controls: `claude --resume` (or `--continue`) reopens a saved conversation under the same session id, and `--fork-session`, used with a resume, copies the history into a new session and leaves the original unchanged. The choice is about what the old context is worth.

- **Resume** when the work continues and what the session learned is still true. Yesterday's investigation, the files as they were.
- **Fork** when several directions start from one shared analysis. Each branch has its own history, and the analysis is not repeated.
- **Start fresh, with a summary** when the old session's tool results describe files that have changed. Tool results in an old session describe files as they were, and the model has no way to notice. Starting a new session and injecting a short structured summary of the findings that still hold is more reliable than resuming with stale results.

The exam keys exactly that third case: a session that read a module, a refactor overnight, and a developer who wants to continue. Resuming gives confident answers about code that is gone. A short summary of the conclusions, and a fresh read of the files that matter, gives answers about the code that exists.

### The environment is part of the setup

A committed `.mcp.json` refers to variables and cannot hold their values. A colleague who clones the repository and starts Claude Code has the servers, and not the tokens, so the ticket tools fail with an authorization error that looks like a product problem and is a missing variable. The remedy is a team note that lists each variable the file refers to and says which are optional. The ticket address has a default and is optional. The token has none, because a default for a token would be a credential in the file, and it is each person's own. The note also holds the table of the three session choices above for the situations the team meets, so nobody decides them afresh each time. The practice asks for both.

A note like this is cheap to check, and the practice checks it: every variable that appears in `.mcp.json` has to appear in the note. The check turns a convention into a rule that fails when someone adds a variable and forgets the note.

### The practice: correct a draft setup

The practice is `exercises/73-scenario-developer-productivity/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. The starter is a draft of five files: `.mcp.json` with a literal token, two subagent definitions, a settings file and a team note. You correct them. The explorer reads and searches only and says when to use it; the scaffolder has the edit tool and no shell; the one allow rule that writes is limited to the generated folder; the ticket tools that change data are denied and only the reading ones are allowed; `.env` is denied; the token is a reference with no default; every tool reference names a configured server; and the note lists the variables and maps three situations to resume, fork and fresh.

The tests grade eight cases: every MCP tool reference names a configured server; credentials come from the environment and the token has no default; the explorer is read only and says when to use it; the scaffolder writes only in the generated folder; the ticket server is read only for agents; the environment file is denied and no whole tool is approved; the note lists every variable and says which session to start; and no file holds a personal path, an address or a key. The starter fails all eight. The reference passes them. Each of seventeen planted wrong solutions per language fails on an assertion of the case it breaks: a tool of a server nobody configured, a literal token, a default for the token, a shell tool in the explorer, a missing tools line, a description that does not say when to use the subagent, a bare edit allow, a Write path rule that Claude Code never consults, a whole-server allow, a delete tool left open, an environment file left readable, a whole-tool allow, a variable missing from the note, a rewritten codebase sent to a resumed session, and a home path.

### What the tests accept

The scaffolder case passes when `Bash` is absent from its tools and every allow rule that edits or writes is an `Edit` rule limited to `src/generated/`; a `Write(...)` path rule fails it, because Claude Code never consults it. The tickets case passes when the create and delete tools are denied and every allowed ticket tool is a get, list or search tool. The credentials case passes when every secret-looking header or variable is a `${VAR}` reference and the token reference has no default. The explorer case passes when its description starts with `Use when`, its `tools` line exists, and the line lists only `Read`, `Grep`, `Glob` and `mcp__docs__search`.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"Resume the old session; the model remembers the analysis."** It is tempting because it saves the work of reading again. It is wrong when the files changed: the old tool results are stale and nothing marks them so.
2. **"Run two separate fresh sessions and repeat the analysis for each design."** It is tempting because the branches stay independent. A fork keeps them independent without repeating the analysis.
3. **"Put a working token in a gitignored copy of the project file."** It is tempting because the committed file then has no secret. The file exists to be shared, so a second private copy defeats it; a reference to the environment is the way.

## Quiz

4. A developer adds a header that reads a new environment name in the shared connection settings and leaves the team write-up alone. What does the practice's grading do?
   - **a**: The credentials case fails, since each new variable needs a default value
   - **b**: The documentation case fails, because one of the variables the servers use is unlisted
   - **c**: Nothing fails, since the write-up is advice that the grading never reads
   - **d**: The server case fails, since a header may not refer to any variable

5. Which allow rule for the tracker passes the practice's read-only case?
   - **a**: `mcp__tickets`
   - **b**: `mcp__tickets__*`
   - **c**: `mcp__tickets__create_ticket`
   - **d**: `mcp__tickets__get_ticket`

6. A new colleague clones the repository, and the ticket tools answer with an authorization error. The shared configuration is committed. What should the repository hold to prevent this?
   - **a**: A default for the token, written into the shared project file
   - **b**: A team note that names every variable that the servers need to start
   - **c**: A private copy of the project file with a working token in it, kept outside version control
   - **d**: A line in the root memory file that holds a shared token

<details>
<summary>Answer key</summary>

4. **b**. The note has to list every variable: "every variable that appears in `.mcp.json` has to appear in the note." *a* is ruled out because the token has no default: "The token has none, because a default for a token would be a credential in the file". *c* is ruled out because the note is checked: "The check turns a convention into a rule that fails when someone adds a variable and forgets the note." *d* is ruled out because a shared file does refer to variables: "A committed `.mcp.json` refers to variables and cannot hold their values."
5. **d**. A get tool, with the create and delete tools denied. *b* is ruled out because a wildcard allows every tool: "every allowed ticket tool is a get, list or search tool". *c* is ruled out because creating is denied: "the create and delete tools are denied". *a* is ruled out for the same reason as a wildcard: "every allowed ticket tool is a get, list or search tool".
6. **b**. A note makes the missing variables visible. *a* is ruled out because a default for a token is a credential in the file: "a default for a token would be a credential in the file". *c* is ruled out because a second copy defeats the shared file: "The file exists to be shared". *d* is ruled out because a committed file cannot hold a value: "A committed `.mcp.json` refers to variables and cannot hold their values."

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S4, a helper that explores a legacy system and generates boilerplate. A check reports `unknown-server: wiki (explorer)`. What does the finding mean?
   - **a**: A header holds a credential that somebody typed in by hand, in plain text
   - **b**: A reference points at an entry that the project file never defines
   - **c**: A subagent has no tools line and inherits everything
   - **d**: A permission rule approves the whole tool rather than one path of it

2. Scenario S4, a helper that explores a legacy system and generates boilerplate. Which entry does the check for typed-in credentials report?
   - **a**: A header named Authorization whose value is `Bearer <token>`
   - **b**: A header named Authorization whose value is `Bearer ${TICKETS_TOKEN}`
   - **c**: A header named X-Trace whose value is `abc`, sent with every request
   - **d**: A variable named REGION whose value is `eu`, passed to the local server

3. Scenario S4, a helper that explores a legacy system and generates boilerplate. Which explorer definition passes the practice's explorer case?
   - **a**: A description that starts `Use when`, and a tools line of `Read`, `Grep` and `Glob`
   - **b**: A description that says `Explores the legacy system`, and the same tools line
   - **c**: A description that starts `Use when`, and a tools line of `Read`, `Grep` and `Bash`
   - **d**: A description that starts `Use when`, and no tools line at all

4. Scenario S4, a helper that explores a legacy system and generates boilerplate. A team writes the header as `Bearer ${TICKETS_TOKEN:-dev-token}` so the tools work on a fresh clone. What is wrong with it?
   - **a**: A default is applied to the address and never to a header
   - **b**: Expansion in headers handles only the plain form of a reference
   - **c**: The default puts a usable credential into a committed file
   - **d**: A file that carries a default cannot be shared through version control

<details>
<summary>Answer key</summary>

1. **b**. The finding is about a name that is not configured. *a* is ruled out because that is another finding: "`literal-secret`: a header or environment entry whose name says token, key, secret or authorization". *c* is ruled out for the same reason: "`agent-inherits-all` and `agent-bare-bash`: a subagent with no `tools` line, or with the shell tool in it". *d* is ruled out because that is a third: "`bare-write-allowed`: an allow rule that approves the whole `Edit` or `Write` tool."
2. **a**. The check reports a secret-looking name whose value has no reference: "a header or environment entry whose name says token, key, secret or authorization and whose value has no `${` reference". *b* is ruled out because its value is a reference: "whose value has no `${` reference". *c* is ruled out because the name says nothing of a secret: "whose name says token, key, secret or authorization". *d* is ruled out for the same reason: "whose name says token, key, secret or authorization".
3. **a**. The page states the case: "The explorer case passes when its description starts with `Use when`, its `tools` line exists, and the line lists only `Read`, `Grep`, `Glob` and `mcp__docs__search`." *b* is ruled out by the first condition: "its description starts with `Use when`". *c* is ruled out by the last: "the line lists only `Read`, `Grep`, `Glob` and `mcp__docs__search`". *d* is ruled out by the second: "its `tools` line exists".
4. **c**. A default for a token is a credential in the file. *b* is ruled out because expansion has both forms: "Expansion of `${VAR}` and `${VAR:-default}` works in a server's `command`, `args`, `env`, `url` and `headers`". *a* is ruled out because a header is one of the places where it works: "`${VAR:-default}` expands to the variable if it is set and to the default otherwise". *d* is ruled out because sharing is the file's purpose: "The `.mcp.json` of a project is committed so that the team shares its servers."

</details>

Adapted from the sample scenario of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the scenario is Anthropic's. The questions here are written for this course.
