# Practice: one shared setup for exploring a legacy system and generating boilerplate

A team uses Claude Code on a legacy system. They want an explorer that only reads, a scaffolder that writes boilerplate in one folder, a documentation server and a ticket server that
agents may read but never change, and a note that tells the next person which environment variables to set and which kind of session to start. The files of the setup already exist
as a draft, and the draft is wrong in several places: a token is typed into a file, a subagent inherits every tool, a permission rule approves a whole tool, a subagent names a server that
is not configured. In this practice you correct the files. There is no program to write and no model is called: the tests read your files. The names, paths and rules the tests check are the
documented ones of Claude Code (checked 2026-10-04) plus this course's own conventions for the note. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and
edit the files there; each language folder holds its own copy of the files.

## What is already written, and what you write

The starter is the draft of the setup with five gaps cut out of it: the files exist and are wrong or empty. The `docs` server is written. Each gap is marked by a `TODO n of 5` comment in the file where the format allows one (`.mcp.json` and `settings.json` are JSON and take none) that says what is missing, with one example and the cases it unlocks. Delete the comment when the file is done. A file holds no program, so there is no logger to write here: the tests read the files and report the failing case with its message. Write them in this order:

1. `.claude/agents/explorer.md` unlocks `m1` and `e2`: a read-only explorer that names only configured servers.
2. `.claude/agents/scaffolder.md` unlocks `e3`: the tools of the scaffolder, with no `Bash`.
3. `.claude/settings.json` unlocks `m1`, `e3`, `e4` and `e5`: the allow and deny rules.
4. `.mcp.json` unlocks `e1`: the ticket address with a default and the token read from the environment.
5. `docs/team-setup.md` unlocks `e6` and `e7`: the variables, the three sessions and no personal path.

About ten lines in all. The sections below describe the whole setup.

## What to write

The project folder holds five files.

- `.mcp.json`: the servers `docs` (a local one) and `tickets` (a remote one). The ticket address may have a default, written `${TICKETS_URL:-https://tickets.example.com/mcp}`. The token comes
  from the environment as `Bearer ${TICKETS_TOKEN}` and has no default.
- `.claude/agents/explorer.md` and `.claude/agents/scaffolder.md`: project subagents. Each has a `description` and a `tools` line.
- `.claude/settings.json`: the `permissions` with `allow` and `deny` lists.
- `docs/team-setup.md`: the note for the team.

Rules the files must follow:

- A tool of an MCP server is written `mcp__<server>__<tool>`, in a subagent's `tools` and in a permission rule. The server must exist in `.mcp.json`.
- The explorer's description starts with `Use when`, and its tools are only `Read`, `Grep`, `Glob` and `mcp__docs__search`.
- The scaffolder has `Edit` and no `Bash`. Every allow rule that edits or writes is limited to `src/generated/`.
- Agents may read tickets (get, list, search) and may not create or delete them.
- `Read(./.env)` is denied, and no allow rule approves a whole tool (`Bash`, `Edit`, `Write`, `Read`, or the whole ticket server).
- The note lists every environment variable that `.mcp.json` refers to, and has a table with a row for each of three situations: continuing yesterday's migration when the files are as they were
  (`resume`), comparing two designs that start from the same analysis (`fork`) and starting after the code was rewritten (`fresh`).
- No file holds a home path, an address other than `example.com`, or a key.

## Why each part is there, and what you should see

1. **A reference should point at something the project defines.** A subagent that lists `mcp__wiki__search` when no `wiki` server is configured names a server that `.mcp.json` does not have. *You should see* every
   reference resolve to a configured server.
2. **A credential belongs to the person, not to the repository.** *You should see* the token read from the environment, with no default that would end up in version control.
3. **A subagent that omits `tools` inherits every tool.** *You should see* both subagents list exactly what they need, the explorer reading only and the scaffolder editing only one folder.
4. **A deny rule is the last line.** *You should see* the ticket tools that change data denied, and the environment file protected.
5. **A setup nobody can start is not shared.** *You should see* a note that names the variables and says when to resume, fork or start fresh.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Every MCP tool reference, in the subagents and in the permission rules, names a configured server |
| `e1` | No literal credential; the token is a `${VAR}` reference with no default |
| `e2` | The explorer is read only, lists its tools and says when to use it |
| `e3` | The scaffolder has no `Bash` and every edit or write rule is limited to the generated folder |
| `e4` | Create and delete ticket tools are denied and only read tools of the ticket server are allowed |
| `e5` | The environment file is denied and no allow rule approves a whole tool |
| `e6` | The note lists every variable and maps three situations to resume, fork or fresh |
| `e7` | No personal path, address or key in any file |
