# Practice: hooks that decide in code

A hook is code that the harness runs at a fixed point of an agent run, so it does not depend on the model remembering an instruction. Two
kinds are in the practice: callbacks that you pass to the Agent SDK, and a command hook that Claude Code runs as a separate process
from its settings file. Write the refund gate that runs before a tool, the normaliser that runs after another, the options that register
them, the command hook and its settings block. The practice is in Python and TypeScript only, because the callbacks belong to the Agent
SDK; a Java or Kotlin team writes the command hook in any language, since the protocol is standard input, standard output and an exit code.
Pick your language folder (`python` or `typescript`), open `starter/` and edit the file there. The tests never call a model: the last case
runs the real SDK against the course's stand-in for the Claude Code binary.

## What to write

Names are Python's; the TypeScript names are `preRefund`, `postNormalise`, `buildOptions`, `commandHook` and `settingsHooks`, and
`cli_path` is `cliPath`.

- `pre_refund(input_data, tool_use_id, context)` (async) is a PreToolUse callback for the tool `process_refund`. For any other tool it
  returns `{}`. It reads `tool_input["amount"]` and answers with
  `{"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": ..., "permissionDecisionReason": ...}}`:
  - `allow` when the amount is at most 200, `ask` when it is above 200 and at most 500, `deny` above 500. The reason of a denial names the
    amount and the limit of 500.
  - `deny` when the amount is missing, not a number, a boolean, not finite, zero or negative: a gate that cannot read its input must not let
    the call through.
- `post_normalise(input_data, tool_use_id, context)` (async) is a PostToolUse callback. `input_data["tool_response"]` is the output of the
  tool, normally a JSON object as text. Return
  `{"hookSpecificOutput": {"hookEventName": "PostToolUse", "updatedToolOutput": <JSON text>}}` with the object changed:
  - `created`, a number of seconds since 1970, becomes the UTC date `YYYY-MM-DD`; a number above 100 billion is milliseconds.
  - `status`, a whole number, becomes a word: `0` is `pending`, `1` is `approved`, `2` is `declined`, anything else is `unknown`.
  - `amount_cents`, a whole number, is removed and replaced by `amount`, a text with two decimals (`12950` becomes `"129.50"`).
  - Every other field stays as it was. When nothing changes, or the output is not a JSON object, return `{}`, so a second run changes
    nothing.
- `build_options(cwd, cli_path=None)` returns the SDK options with `cwd`, the allowed tools `get_order` and `process_refund`, permission
  mode `default`, no setting sources, and two hooks: PreToolUse with the matcher `process_refund` and `pre_refund`, PostToolUse with the
  matcher `get_order|get_refund` and `post_normalise`, each with a timeout of 5 seconds. A matcher is matched against the tool name.
- `command_hook(stdin_text)` is what a command hook does with the JSON that Claude Code writes to its standard input. It returns
  `{"exit": code, "stderr": text}`. Exit code `2` blocks the call and the text on standard error goes to the model; exit code `0` lets it
  go on; exit code `1` would be a non-blocking error, so a guard never uses it. For the tool `Bash`, a command that contains `git push` or
  `rm -rf` blocks, with a reason that says why. Every other call exits `0`. Input that is not a JSON object blocks with exit `2`.
- `settings_hooks(script="python3 .claude/hooks/guard.py", timeout=10)` returns the `hooks` block of `.claude/settings.json`: a PreToolUse
  entry with the matcher `Bash` and one hook `{"type": "command", "command": script, "timeout": timeout}`.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The gate allows a small refund, asks about a middle one and denies a large one |
| `e1` | Each limit belongs to the lower tier and the next cent goes up |
| `e2` | A missing or invalid amount is denied and other tools are left alone |
| `e3` | The output gets a date, a status word and a decimal amount |
| `e4` | Output that is not JSON or is already readable is left alone |
| `e5` | The hooks are registered on tool name matchers with a timeout |
| `e6` | A command hook blocks with exit two and a reason, and fails closed on bad input |
| `e7` | The settings block runs the command hook on Bash with a timeout |
| `e8` | A run through the SDK denies the large refund and shows the model a readable order |

Run the tests with the command in the language folder's `run.sh`.
