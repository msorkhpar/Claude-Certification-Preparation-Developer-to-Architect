# Practice: set up a repository for Claude Code

A repository is ready for Claude Code when the right instructions load every session, the permission rules say what runs freely,
what asks and what never runs, the personal settings stay on one machine, and the unattended runs are bounded. All of that is plain
files. In this practice you write them for a small service, the invoice API, and tests read the project the way Claude Code would.
Pick your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the files there. The tests are the same in all four
languages and read only the project: the files are not code, and the checks run on the course's own models of the documented rules (`examples/38-settings-layers`
and `examples/39-hook-gate`, in your language). The Java and Kotlin tests read JSON with Jackson and YAML front matter with its YAML module. Nothing here starts
Claude Code or touches the network.

## The project

`starter/` holds `docs/architecture.md` (finished), a `CLAUDE.md` that only says `TODO`, an empty `.claude/settings.json`, and a
`.claude/settings.local.json` that someone filled in with a personal path. The tests merge your files with two fixed layers: an
organisation policy that denies `Bash(sudo *)`, and a user file that sets the model to `haiku` and allows `Bash(ls *)`.

## What to write

- `CLAUDE.md`: at most 200 lines. Name the commands `make test` and `make lint` in backticks, import `docs/architecture.md` with an
  `@` line, and keep the emphasis for at most two lines. Everything in it is a fact Claude would otherwise get wrong.
- `.claude/settings.json`, the shared file:
  - `model` is `opus`, the team default;
  - `permissions.defaultMode` is `acceptEdits`;
  - allow `make test`, `make lint` and the read-only git commands `git status`, `git diff` and `git log` (with any arguments);
  - ask before `git commit` (with any arguments);
  - deny reading `.env` and everything under `secrets/`, deny `git push` and deny `curl`.
  Use only the rule forms that are consulted: a path rule for the `Write` tool is never checked, and a bare `Bash` allow rule approves
  every command. Nothing else belongs in the file.
- `.claude/settings.local.json`: one key, `model`, set to `sonnet`, so that this developer's sessions use it and the team's do not.
  Add `.claude/settings.local.json` and `CLAUDE.local.md` to `.gitignore`.
- `.claude/skills/fix-issue/SKILL.md`: a custom command, `/fix-issue <number>`. It has a `name`, a `description` and an
  `argument-hint`, only a person can start it (it edits code and calls `gh`), its body uses `$ARGUMENTS` and tells Claude to run
  `make test`.
- `scripts/ci-review.sh`: reviews the diff headlessly with `claude -p`, in `--bare` mode, with `--output-format json`, at most 10
  turns, a dollar cap, a permission mode that never prompts and never auto-approves (`dontAsk` or `plan`), and `--allowedTools` listing
  `Read` and read-only git patterns, not whole tools that change things. It never skips permissions and never holds a key: the key
  comes from the environment.
- No file in the project holds a personal path, an email address or a key.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The memory file is short, names the commands, and its import resolves to the architecture notes |
| `e1` | Sixteen sample calls get the right answer from the merged rules: allow, ask or deny |
| `e2` | The shared file sets a mode it is allowed to set and uses only rules that Claude Code consults |
| `e3` | The personal file wins for its owner, is ignored by git and stays small |
| `e4` | The custom command is a skill with an argument hint that Claude cannot start by itself |
| `e5` | The headless script is capped, does not skip permissions and pre-approves no whole tool that changes things |
| `e6` | No file holds a personal path, an address or a key |

Run the tests with the command in the language folder's `run.sh`.
