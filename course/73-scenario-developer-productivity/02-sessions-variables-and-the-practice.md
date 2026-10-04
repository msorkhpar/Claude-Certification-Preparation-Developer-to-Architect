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

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"Resume the old session; the model remembers the analysis."** It is tempting because it saves the work of reading again. It is wrong when the files changed: the old tool results are stale and nothing marks them so.
2. **"Run two separate fresh sessions and repeat the analysis for each design."** It is tempting because the branches stay independent. A fork keeps them independent without repeating the analysis.
3. **"Put a working token in a gitignored copy of the project file."** It is tempting because the committed file then has no secret. The file exists to be shared, so a second private copy defeats it; a reference to the environment is the way.

## Quiz

4. Yesterday's work studied a module, and overnight a refactor rewrote it. What is the next step?
   - **a**: Reopen yesterday's session and trust the results it holds
   - **b**: Open a new session and give it a short summary of what still holds
   - **c**: Fork yesterday's session and ask it to look again at the module
   - **d**: Continue the previous session and tell it that the code changed

5. A developer wants two migration designs, both starting from one study of a module. Which approach fits?
   - **a**: Keep one session and ask for the designs one after the other
   - **b**: Resume the one session twice and have both designs added to it
   - **c**: Start two sessions from nothing and repeat the analysis in each
   - **d**: Fork the session after the analysis, one branch for each option

6. A new colleague clones the repository, and the ticket tools answer with an authorization error. The shared configuration is committed. What should the repository hold to prevent this?
   - **a**: A default for the token, written into the shared project file
   - **b**: A team note that names every variable that the servers need to start
   - **c**: A private copy of the project file with a working token
   - **d**: A line in the root memory file that holds a shared token

<details>
<summary>Answer key</summary>

4. **b**. Tool results in an old session describe files as they were. *a* is ruled out because nothing marks stale results: "the old tool results are stale and nothing marks them so". *c* is ruled out because a fork copies the history, stale results included: "copies the history into a new session". *d* is ruled out because a notice does not refresh the results: "the model has no way to notice".
5. **d**. A fork gives each design its own history and keeps the analysis. *b* is ruled out because a resume appends to the original: "reopens a saved conversation under the same session id". *c* is ruled out because it repeats the analysis: "A fork keeps them independent without repeating the analysis." *a* is ruled out because one history mixes the designs, which is what a fork avoids: "Each branch has its own history".
6. **b**. A note makes the missing variables visible. *a* is ruled out because a default for a token is a credential in the file: "a default for a token would be a credential in the file". *c* is ruled out because a second copy defeats the shared file: "The file exists to be shared". *d* is ruled out because the memory file is committed context, not a place for a secret: "A committed `.mcp.json` refers to variables and cannot hold their values."

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S4, a helper that explores a legacy system and generates boilerplate. A check reports `unknown-server: wiki (explorer)`. What does the finding mean?
   - **a**: A header holds a credential that was typed in by hand
   - **b**: A reference points at an entry that the project file never defines
   - **c**: A subagent has no tools line and so inherits whatever is available
   - **d**: A permission rule approves the whole tool and not a path

2. Scenario S4, a helper that explores a legacy system and generates boilerplate. A project definition file has a description and a prompt but omits its list of permitted tools. What can it call?
   - **a**: Everything that is available to subagents
   - **b**: Only the reading tools, as the minimum set
   - **c**: No tool at all until a rule approves one
   - **d**: The tools of the parent that were listed first

3. Scenario S4, a helper that explores a legacy system and generates boilerplate. Exploring a big module would fill the main conversation with file contents. What keeps the main context small?
   - **a**: A read-only subagent does the reading and hands back its result
   - **b**: A longer root memory file that summarises the module in advance
   - **c**: A forked session that begins with the history of the main one
   - **d**: A rule that denies the shell tool during every exploration

4. Scenario S4, a helper that explores a legacy system and generates boilerplate. A team writes the header as `Bearer ${TICKETS_TOKEN:-dev-token}` so the tools work on a fresh clone. What is wrong with it?
   - **a**: A default is applied to the address and never to a header
   - **b**: Expansion in headers handles only the plain form of a reference
   - **c**: The default puts a usable credential into a committed file
   - **d**: A file that carries a default cannot be shared through version control

<details>
<summary>Answer key</summary>

1. **b**. The finding is about a name that is not configured. *a* is ruled out because that is another finding: "`literal-secret`: a header or environment entry whose name says token, key, secret or authorization". *c* is ruled out for the same reason: "`agent-inherits-all` and `agent-bare-bash`: a subagent with no `tools` line, or with the shell tool in it". *d* is ruled out because that is a third: "`bare-write-allowed`: an allow rule that approves the whole `Edit` or `Write` tool."
2. **a**. The omission is the widest list. *b* is ruled out because the widest list is what omission gives: "leaving the line out grants the most, not the least". *c* is ruled out because the subagent inherits tools and does not start empty: "A subagent that omits `tools` inherits every tool that is available to subagents". *d* is ruled out because the line names tools and not an order: "the `tools` line is the allowlist".
3. **a**. A subagent keeps what it reads in its own context. *b* is ruled out because a memory file does not move the reading: "the root file loads in every session and costs context each time". *c* is ruled out because a fork copies the history: "copies the history into a new session". *d* is ruled out because denying a tool does not move any reading elsewhere: "Denying the shell does not limit the file tools".
4. **c**. A default for a token is a credential in the file. *b* is ruled out because expansion has both forms: "Expansion of `${VAR}` and `${VAR:-default}` works in a server's `command`, `args`, `env`, `url` and `headers`". *a* is ruled out because a header is one of the places where it works: "`${VAR:-default}` expands to the variable if it is set and to the default otherwise". *d* is ruled out because sharing is the file's purpose: "The `.mcp.json` of a project is committed so that the team shares its servers."

</details>

Adapted from the sample scenario of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the scenario is Anthropic's. The questions here are written for this course.
