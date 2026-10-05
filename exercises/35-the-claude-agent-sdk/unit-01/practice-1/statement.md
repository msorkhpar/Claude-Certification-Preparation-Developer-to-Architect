# Practice: an agent that may read, but not do harm

The Claude Agent SDK runs the same agent loop as Claude Code, as a library: your program calls `query()`, and the SDK starts the Claude
Code binary as a child process and exchanges messages with it. The model and the tools live in that binary. What you own is the part
around it: which tools exist, which calls are allowed, what a hook may veto, how long the run may last, and what you make of the stream
of messages that comes back. Write that part. Pick your language folder (`python` or `typescript`: the Agent SDK has no Java or Kotlin
edition, so this practice has none), open `starter/` and edit the file there. The options, the permission order, the hook output and the
message types come from the Agent SDK documentation and the SDK's own type definitions at the versions in `docs/VERSIONS.md`, read on
2026-10-03; the lesson pages explain them. The tests never call the model. They start `harness/fake_claude.py`, a scripted stand-in for the
binary that speaks the same protocol, through the SDK's own path option (`cli_path` in Python, `pathToClaudeCodeExecutable` in TypeScript),
so the SDK code you call is the real one.

## The given parts

| Name | Meaning |
|---|---|
| `READ_TOOLS`, `EDIT_TOOLS`, `SAFE_COMMANDS`, `STATUS` | the read tools (`Read`, `Grep`, `Glob`), the edit tools (`Edit`, `Write`), the shell words that may run (`ls`, `cat`, `pytest`), and the map from a result subtype to the course status |
| `project_dir` | the one directory the agent may touch |
| `mode` | `readonly` (the default) or `edit` |
| `cli_path` | the executable that the SDK starts; the tests pass the stand-in |

## What is already written, and what you write

The starter is the working code with nine small gaps cut out. The path helpers, the options plumbing, `decide`, the hook, `summarize` and
`run_agent` are written and correct; each gap is a small function with its signature, a comment that says what it receives and returns with
one example, and the cases it unlocks. A gap returns a neutral value, so the starter runs and fails the cases on an assertion. To debug a
gap, log its input with the `log` line at the top of the file (`log.debug(...)` in Python, `log.debug(...)` on the `logger` in TypeScript);
a run shows the lines under the failing case. The names are in Python style; TypeScript uses camelCase (`isSecretName`, `makeCanUseTool`).

1. `_is_secret_name` unlocks `e2`: which file names hold secrets.
2. `_dangerous` unlocks `e3`: `sudo` and `rm -rf`.
3. `_chained` unlocks `e3`: chaining, pipes, redirection and substitution.
4. `_command_allowed` unlocks `e3`: the first word against `SAFE_COMMANDS`.
5. `make_can_use_tool` unlocks `m1` and `e7`: the callback that turns `decide()` into the SDK's allow or deny result.
6. `_is_push` unlocks `e4`: the whole-word `git push` test.
7. `build_options` unlocks `e1`: the options that reach the CLI as flags.
8. `_denied` unlocks `e5` and `e7`: counting the tool results marked as errors.
9. `_status` unlocks `e5`: the course status of a result subtype.

The sections below describe the whole program; the parts you do not write are there so you can see how your functions are used.

## What to write

- `decide(tool_name, tool_input, project_dir, mode)` is the permission policy as plain data: `{"behavior": "allow"}` or `{"behavior": "deny",
  "message", "interrupt"}`.
  - `Read`, `Grep`, `Glob`, `Edit` and `Write` take the path from `file_path` or else `path`; a call with no path is allowed (a search of the
    project). `Edit` and `Write` in `readonly` mode: deny, `Edits are not allowed in readonly mode`, checked first.
  - A path outside the project (relative paths are taken from `project_dir`, `..` is resolved, and a sibling folder whose name merely starts
    like the project's is outside): deny, `{path} is outside the project`, with the path as given.
  - A file whose name starts with `.env`, except `.env.example`: deny, `{name} holds secrets and is never read`, with the file name only.
    An edit of a path that has a `.git` folder in it: deny, `{path} is inside .git`.
  - `Bash` looks at `command`. When it contains the word `sudo` or the text `rm -rf`: deny, `Dangerous command`, with `interrupt` true (the whole run
    stops). Otherwise when it contains `;`, `&`, `|`, `<`, `>`, a backtick or `$(`: deny, `Command not allowed: no chaining or redirection`.
    Otherwise it is allowed when its first word is one of `SAFE_COMMANDS`; else deny, `Command not allowed: only ls, cat and pytest`.
  - Any other tool: deny, `{tool} is not allowed`. `interrupt` is false for every denial except the dangerous command.
- `make_can_use_tool(project_dir, mode)` returns the callback the SDK calls for a tool that is not already decided: it turns `decide()` into the
  SDK's allow result (carrying the input back as the updated input) or deny result (message and interrupt).
- `bash_guard` is a `PreToolUse` hook. When the command of a `Bash` call runs `git push` (the two words, with any white space between,
  and as whole words: `git pushd` and `legit push` are not pushes), it returns the hook output that denies the call, with the reason
  `Nothing is pushed from an agent`; otherwise it returns an empty result. A hook runs before the permission callback.
- `build_options(project_dir, cli_path, mode)` builds the SDK options: the CLI path, `cwd` the project, the available tools
  (`Read`, `Grep`, `Glob`, `Bash`, plus `Edit` and `Write` in `edit` mode), **no pre-approved tools** (an allow list would let those tools run
  without the callback being asked), `Bash(rm *)` disallowed, at most 6 turns, a budget of 0.5 US dollars, permission mode `default`, no setting
  sources (the run does not read user or project settings), the callback and the hook for `Bash`.
- `summarize(messages)` folds the messages of one run into `{"status", "text", "tools", "turns", "cost", "denied"}`: `tools` lists the name of
  every tool call in order; `denied` counts the tool results marked as errors; `text` is the result text, or when that is empty or missing the
  last text the assistant wrote; `turns` and `cost` come from the result message (a missing cost is 0). The status is `STATUS[subtype]`, or the
  subtype itself when it is not in `STATUS`. A run with no result message is `{"status": "incomplete", ..., "turns": 0, "cost": 0}`.
- `run_agent(prompt, project_dir, cli_path, mode)` runs `query()` with those options, collects every message and returns the summary. A
  single-shot `query()` that ends on an error result (the turn limit, the budget) yields that result and then raises, because the process
  exits with a nonzero code; that raise is not a failure of the run, so a summary is still returned once a result message has arrived. An
  error with no result message before it (the process died) is not hidden: it propagates.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Every tool call goes through the permission callback, and the run is summarised |
| `e1` | The options reach the CLI as flags, and the run starts in the project |
| `e2` | File tools stay inside the project and away from secrets |
| `e3` | Bash is limited to a few commands, and dangerous ones stop the run |
| `e4` | A hook blocks a push before the permission callback is asked |
| `e5` | The messages of a run fold into a summary |
| `e6` | An error result still gives its summary, and a crash is not hidden |
| `e7` | Denied calls are counted, and the run still ends with a result |

Run the tests with the command in the language folder's `run.sh`.
