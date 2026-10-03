# Practice: set up a read-mostly explorer for an unfamiliar codebase

A developer-productivity agent has to explore a service nobody on the team knows: find where things start, follow imports, trace a function
through wrapper modules and write down what it learns. It may read and search anything except the secrets, it may write notes, and it must not
change the source or run commands that change anything. All of that is plain files. In this practice you write them for a small service, the
inventory service, and the tests read the project the way Claude Code would. Pick your language folder (`python` or `typescript`), open `starter/`
and edit the files there. The tests are the same in both languages and read only the files: they are configuration and notes, not code in a
language, so there is no Java or Kotlin edition (no JSON reader is available offline for those two here, and a second edition would test the
same files). The checks run on the course's own models of the documented rules (`examples/38-settings-layers` for permission rules and
`examples/56-builtin-tools` for the tools). Nothing here starts Claude Code or touches the network.

## What to write

- `.claude/settings.json`, the shared permissions:
  - allow the read-only git commands `git log` and `git diff` (with any arguments) and `git status`, and allow edits under `notes/`;
  - deny reading `.env` and everything under `secrets/`, and deny edits under `src/`;
  - nothing else. Use only the rule forms that are consulted: a path rule for `Write` is never matched (write it as `Edit(...)`), the search tools
    take their path rules as `Read(...)`, and a bare `Bash` allow rule approves every command.
- `.claude/agents/explorer.md`: a subagent named `explorer` with a `description` that starts a sentence with `Use when`, `tools` listing `Read`, `Grep` and
  `Glob` and nothing else, a `model`, a `maxTurns` of at most 15, and a body with its instructions.
- `agent-options.json`: the options an SDK application passes to the same agent: `tools` with the three tools, `allowedTools` pre-approving them, and
  `disallowedTools` listing `Bash`, `Edit` and `Write` by bare name. On macOS, Linux and WSL, `Grep` and `Glob` are not in the default tool set; naming
  them is what makes them available.
- `docs/exploration-plan.md`: how the agent explores, in two parts.
  - A short paragraph that says not to read every file first, then a numbered list: find entry points with `Grep`, find files by name with `Glob`
    (give a pattern in backticks), read the entry files with `Read` and follow imports one hop at a time, list the names each wrapper module exports and
    search for each exported name with `Grep` before tracing usage, and write findings to `notes/findings.md`.
  - Under the heading `## When an edit does not apply`, three numbered remedies in this order: repeat the text with more surrounding lines until it is
    unique; use `replace_all` when every occurrence should change; and as a last resort `Read` the whole file and `Write` it back with the change.
- No file holds a personal path, an email address or a key. (The starter has a personal path; find it.)

## Why each part is there, and what you should see

1. **Permission rules for an explorer.** The exam asks how permissions bound the built-in tools. *You should see* reading and searching allowed, edits
   under `src/` denied, notes allowed, anything else asking, and no whole tool approved.
2. **A read rule covers more than Read.** A `Read(...)` deny also stops `Grep` and `Glob` from searching that path and stops `Edit` and `Write` from
   changing it. *You should see* eight calls on secrets and the environment file all denied, in every tool.
3. **Only the forms that are consulted.** A rule that is never matched protects nothing. *You should see* no `Write(...)` or `Grep(...)` path rule and no bare allow.
4. **The agent's tool list.** Least privilege for a subagent is a list. *You should see* three tools, a sentence that says when to use the agent, and a turn limit.
5. **The options of an SDK application.** `allowedTools` pre-approves and does not restrict; `tools` and a bare name in `disallowedTools` decide what exists.
   *You should see* the tool set resolve to exactly Read, Grep and Glob on Linux.
6. **Incremental exploration.** The exam asks for a search for entry points, then reads that follow imports, and a search for each exported name when
   tracing through wrappers, in place of reading everything upfront. *You should see* the plan's order checked step by step.
7. **When Edit cannot apply.** Edit needs one exact match. *You should see* the remedies in the order of how little they risk: a longer anchor, `replace_all`,
   and the whole-file rewrite last.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Thirteen sample calls get the right answer from your rules: read and search freely, notes yes, source no |
| `e1` | A read rule protects secrets from reading, searching and writing |
| `e2` | Only rule forms that are consulted, and no whole tool that changes things allowed |
| `e3` | The explorer agent reads and searches, says when to use it and is bounded |
| `e4` | The SDK options make the search tools available and remove the tools that change things |
| `e5` | The plan starts with a search, then reads, and never reads everything first |
| `e6` | The edit remedies come in order: longer anchor, replace_all, whole-file rewrite |
| `e7` | No file holds a personal path, an address or a key |

Run the tests with the command in the language folder's `run.sh`.
