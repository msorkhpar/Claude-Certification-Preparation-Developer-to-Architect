# Practice: one person's Claude Code setup, in the right files

A developer wants shorter replies, the context percentage always in view and one key changed. A team lead wants the same repository to keep its permission rules in one shared file. Each of
these goes wrong quietly: a style name in the wrong case selects the default style, a misspelled field is ignored, a reserved key cannot be rebound, a personal style committed to the shared
file overrides every teammate. In this practice you write the files of the page "Making Claude Code yours". Nothing is installed and Claude Code is not started: the tests read your files and
check them against the documented rules. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the files there.

The folder has two parts. `home/` stands for the user's `~` and `project/` for a repository.

| File | What it holds |
|---|---|
| `home/.claude/settings.json` | the user's settings: `outputStyle` and `statusLine` |
| `home/.claude/output-styles/terse-review.md` | a custom output style |
| `home/.claude/statusline.sh` | the status line script, which reads the session JSON on standard input |
| `home/.claude/keybindings.json` | the user's keybindings |
| `project/.claude/settings.json` | the team's shared settings |
| `project/.claude/settings.local.json` | the developer's personal override for this project |
| `project/.gitignore` | the repository's ignore file |

The starter holds the files unfinished. The settings files are strict JSON: a comment or a trailing comma is an error.

## What is already written, and what you write

The starter is the setup with seven gaps cut out. The shape of every file is written: the keybindings file with its schema and its `Chat` block, the
user settings with the `statusLine` object, the style file with its name and description, the status line script with its shebang and its first
field, and the two project settings files. JSON cannot carry a comment, so the JSON gaps are listed here; each is an empty value (`""`, `0`, `{}` or
`[]`) in the file named, and a gap in a script, a style file or the ignore file is a comment that says what to write. The starter loads and fails the
cases on an assertion. Write them in this order:

1. `home/.claude/settings.json` unlocks `m1` and `e2`: the style name, the script path in `~/.claude/` and the `padding` and `refreshInterval` numbers.
2. `home/.claude/output-styles/terse-review.md` unlocks `e1`: `keep-coding-instructions: true` and the instructions, at least three lines.
3. `home/.claude/statusline.sh` unlocks `e2` and `e6`: the context percentage and the branch, read from documented fields only.
4. `home/.claude/keybindings.json` unlocks `e3`: one key rebound to an action and one freed with `null`, in the `Chat` block.
5. `project/.claude/settings.json` unlocks `e4`: the team's `permissions.allow` rules, and nothing personal.
6. `project/.claude/settings.local.json` unlocks `e4`: the exact style name for this project.
7. `project/.gitignore` unlocks `e5`: the line that keeps the local settings file out of git.

About nine lines in all. These files have no code to log from; a failing case shows the assertion message, which names the file and the rule.

## What to write

1. **The user's settings.** `outputStyle` names the style, exactly as its file or its built-in name spells it. `statusLine` is an object with `type` `"command"`, a `command` that points at
   the script in `~/.claude/`, an optional `padding` (0 or more) and an optional `refreshInterval` (seconds, 1 or more).
2. **The style file.** Front matter with `name`, `description` and `keep-coding-instructions` set to `true` (the style only changes how replies read), and no other field. Then the
   instructions: at least three lines that say how a reply is written (the result first, evidence only, full length when an explanation is asked for).
3. **The status line script.** A script with a shebang line that reads the session JSON with `jq -r '...'` and prints one line. It may read only fields the session sends, such as
   `model.display_name`, `context_window.used_percentage` and `workspace.current_dir`.
4. **The keybindings.** An object with a `bindings` array of blocks. Each block names a `context` exactly as the documentation spells it (`Chat`, not `chat`), and maps keystrokes to
   actions: at least one key rebound to a `namespace:action` name and at least one key freed with `null`. Keystrokes use the modifiers `ctrl`, `shift`, `alt`, `meta` or `cmd`. The keys
   Ctrl+C, Ctrl+D, Ctrl+M, Ctrl+I, Ctrl+H and Ctrl+[ cannot be rebound.
5. **The shared project file.** It carries the team's `permissions.allow` rules and no `outputStyle` or `statusLine`: those are personal and a committed file would override every teammate.
6. **The local override.** `project/.claude/settings.local.json` sets `outputStyle` to an exact style name for this project only, and `project/.gitignore` lists the file so it stays out of git.
7. **Nothing personal, nothing unfinished.** No file holds a home folder path, an e-mail address, a key that starts like an API key, or a TODO marker.

## Why each part is there, and what you should see

1. **Names are exact.** *You should see* the user's `outputStyle` find a style, and a style name written in another case rejected.
2. **A style keeps the engineering.** *You should see* `keep-coding-instructions: true` and only the four documented fields, because a misspelled field is ignored without an error.
3. **The script reads what is sent.** *You should see* only documented JSON fields, and a refresh interval of at least one second.
4. **Keys have rules.** *You should see* real contexts, real modifiers, a `namespace:action` name for each rebinding, a `null` that frees a key, and no reserved key rebound.
5. **Scopes keep choices apart.** *You should see* the shared file with permissions only, the personal style in the local file, and the local file listed in the ignore file.
6. **Nothing leaks.** *You should see* no personal path, address or key in any file.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The user level holds a style, a status line and a keybindings file that fit together |
| `e1` | The style file keeps the coding instructions and carries only documented fields |
| `e2` | The status line reads only fields the session sends and refreshes no faster than every second |
| `e3` | The keybindings use real contexts and actions, free keys and a null to unbind |
| `e4` | Personal settings stay in the user files and the project file stays shared |
| `e5` | The local settings file is kept out of git |
| `e6` | No file holds a personal path, an address, a key or an unfinished marker |

Run the tests with the command in the language folder's `run.sh`.
