# Practice: set up a repository for Claude Code

A repository is ready for Claude Code when the right instructions load every session, the permission rules say what runs freely,
what asks and what never runs, the personal settings stay on one machine, and the unattended runs are bounded. All of that is plain
files. In this practice you write them for a small service, the invoice API, and tests read the project the way Claude Code would.
Pick your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the files there. The tests are the same in all four
languages and read only the project: the files are not code, and the checks run on the course's own models of the documented rules (`examples/38-settings-layers`
and `examples/39-hook-gate`, in your language). The Java and Kotlin tests read JSON with Jackson and YAML front matter with its YAML module. Nothing here starts
Claude Code or touches the network.

## The project

`starter/` is the finished project with eight gaps cut out of it: `docs/architecture.md`, the Conventions and Gotchas of `CLAUDE.md`, the
skill's body and the script's plumbing are written. The tests merge your files with two fixed layers: an organisation policy that denies
`Bash(sudo *)`, and a user file that sets the model to `haiku` and allows `Bash(ls *)`. Everything is plain files, so a gap is a
`TODO` comment where a comment is possible (Markdown, the skill, the script) and is named below where it is not (JSON). A run reports
what each failing case read; to debug a gap, put the file's content or the merged settings in a `print` of the test you are working on.
These files are not code, so there is no logger line to add (the `log` line of the code practices has no file to live in here).

## What is already written, and what you write

About a dozen lines in all. In this order:

1. `CLAUDE.md`, Commands: the two bullets with `make test` and `make lint` in backticks. Unlocks `m1`.
2. `CLAUDE.md`, Architecture: the `@docs/architecture.md` import line. Unlocks `m1`.
3. `.claude/settings.json`: `model` is `opus` and `permissions.defaultMode` is `acceptEdits`. Unlocks `e2` and `e3`.
4. `.claude/settings.json`, allow: add `make lint` and the three read-only git commands to the one rule given. Unlocks `e1`.
5. `.claude/settings.json`, deny: add `Read(./.env)`, `Read(./secrets/**)` and `Bash(git push *)` to the `curl` rule given. Unlocks `e1` and `e2`.
6. `.claude/settings.local.json` and `.gitignore`: replace the personal path by the one `model` key, and ignore the two personal files. Unlocks `e3` and `e6`.
7. `.claude/skills/fix-issue/SKILL.md`: the `argument-hint`, the line that only lets a person start it, and the `make test` step. Unlocks `e4`.
8. `scripts/ci-review.sh`: the permission mode, the turn limit, the dollar cap and the read-only `--allowedTools`. Unlocks `e5`.

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
