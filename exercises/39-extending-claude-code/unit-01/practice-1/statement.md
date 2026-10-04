# Practice: a release plugin with a guard hook

A team wants to share one set of extensions: a skill that drafts release notes, a skill that publishes a release, a subagent that reviews
the changelog, and a hook that stops destructive commands. Claude Code packages these as a plugin. In this practice you write the
plugin's files for a plugin called `release-kit`, and the team's settings that install it. Tests read the files, and run your hook
script the way Claude Code does, as a process that reads one JSON event on standard input.

Pick your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the files there. The tests are the same in all
four languages, and the hook script is a Python file in all of them: a hook is a separate program, and Python is the one runtime every editor has
here, so the Java and Kotlin tests start it as a process too. The rest are files, not code in your language. The checks use the course's own models
of the documented rules (`examples/39-hook-gate`, in your language); the Java and Kotlin tests read JSON with Jackson and the YAML front matter with
its YAML module. Nothing here starts Claude Code or touches the network.

## What to write

- `scripts/guard.py`, a `PreToolUse` hook. It reads one event, a JSON object with `tool_name` and `tool_input`.
  - For `Bash`, it looks at the command in every spelling and refuses with the structured answer: exit code 0, and a JSON object on
    standard output whose `hookSpecificOutput` has `hookEventName` `PreToolUse`, `permissionDecision` `deny` and a non-empty
    `permissionDecisionReason`. It refuses `git push` (also with options before the subcommand such as `git -C . push`, behind
    an environment assignment, a path to `git`, `&&`, `;` or `sh -c`), a recursive forced `rm` (`-rf`, `-fr`, `-r -f`, a path to `rm`,
    inside `sh -c`), and a download piped into a shell (`curl ... | sh`, `wget ... | bash`). It gives no opinion on `git status`,
    `git pull`, `echo push`, `rm x.txt`, `rm -r build`, a download written to a file, or a pipe into `grep`. No opinion is exit code 0
    with nothing printed.
  - For `Edit`, `Write` and `MultiEdit`, it blocks a path that contains `.env`, `package-lock.json`, `.git/` or `secrets/`, after
    turning backslashes into `/`. Blocking is exit code 2, nothing on standard output and the reason on standard error.
  - Any other tool gets no opinion. An event that is not valid JSON, is empty, is not an object or has no `tool_name` blocks with exit
    code 2 and a reason on standard error. A `Bash` event with no command gives no opinion.
- `hooks/hooks.json`: one `PreToolUse` group with the matcher `Bash|Edit|Write` and one `command` handler that runs the script
  through `${CLAUDE_PLUGIN_ROOT}`, so it works wherever the plugin is installed.
- `skills/release-notes/SKILL.md`: `name` is `release-notes`. Its `description` says what it does and starts a sentence with
  `Use when`. Claude may start it. `allowed-tools` pre-approves `Read`, `Grep` and the pattern `Bash(git log *)`, and no bare `Bash`.
- `skills/publish/SKILL.md`: `name` is `publish`. Only a person may start it (`disable-model-invocation: true`). Its `allowed-tools`
  pre-approves the patterns `Bash(git tag *)` and `Bash(gh release create *)`. Its body uses `$ARGUMENTS`.
- `agents/changelog-reviewer.md`: a subagent named `changelog-reviewer` with a `description`. It reads and does not edit or run
  commands: `tools` lists only `Read`, `Grep` and `Glob`. It has `memory: project`, a `maxTurns` between 1 and 10, and a `model`.
  A plugin's agents ignore `permissionMode`, `hooks` and `mcpServers`, so leave them out. The body is the agent's instructions.
- `.claude-plugin/plugin.json`: `name` `release-kit`, a semantic `version`, a `description`, and one dependency, `secrets-vault`
  at `~2.1.0`, which takes patch updates only. Use the object form `{"name", "version"}`. No other keys than the ones the manifest
  documents for this purpose (`author`, `license` and `keywords` are allowed).
- `.claude/settings.json` (the team's file): register a marketplace from a GitHub repository (`owner/name`) under
  `extraKnownMarketplaces`, and turn the plugin on under `enabledPlugins` as `release-kit@<that marketplace>`.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The guard refuses pushes, forced recursive deletes and piped downloads in every spelling, with a reason, and leaves safe commands alone |
| `e1` | Edits to protected paths stop with exit code 2 and a reason, in any of three tools and with Windows separators |
| `e2` | An unreadable event blocks the call, while other tools and a Bash event with no command are left alone |
| `e3` | The hook is registered once for the three tools it guards and found through the plugin root |
| `e4` | The skills have the right invocation rules, approve only patterns and say when to use them |
| `e5` | The subagent only reads, is bounded, and uses only fields a plugin agent honours |
| `e6` | The manifest names the plugin and pins the dependency to patch updates |
| `e7` | The team settings register a marketplace and enable the plugin from it |

Run the tests with the command in the language folder's `run.sh`.
