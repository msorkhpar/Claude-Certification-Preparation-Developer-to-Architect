# Making Claude Code yours: output styles, the status line, keybindings and where each setting lives

**Level:** Developer · **Module 39:** Extending Claude Code · **Page 5 of 5**
**Exams:** DV7; A3

**After this page you can** change how Claude Code writes its replies with an output style, show what you need in the status line with a script, remap keys in the keybindings file, say which of the three is read from which file and which scope wins when settings disagree, and keep your personal choices out of a team's shared files.

Checked on 2026-10-04 against the Claude Code documentation pages "Output styles", "Customize your status line", "Customize keyboard shortcuts" and "Settings files and precedence", which document behaviour up to Claude Code v2.1.288. **What was read and what was run:** every statement about Claude Code on this page was read from those pages. No Claude Code session was started and no setting was changed. The practice's tests read the configuration files you write and run offline in the course container, in Python, TypeScript, Java and Kotlin; they check the files against the documented rules and never start Claude Code.

## Why it matters

Pages 1 to 4 extended what Claude Code can do: skills, subagents, hooks, plugins. A developer also wants it to fit them: shorter replies, the context percentage always in view, a different key for a stash. These are not extensions to share with a team, and a team that puts them in the shared file overrides every teammate. The questions an exam can ask are of three kinds: which feature this is (a style is an instruction, a hook is a guarantee), where it lives (user, project, local, managed) and why a setting did nothing (a name in the wrong case, a field misspelled, a reserved key).

## The idea

### Three personal settings and what each one changes

| Setting | What it changes | Where it is read from |
|---|---|---|
| Output style | the instructions Claude follows about voice, length and format of every reply | the `outputStyle` key of a settings file, and style files in an `output-styles` folder |
| Status line | a bar under the prompt that shows what a script of yours prints | the `statusLine` key of a settings file, and the script it names |
| Keybindings | which key does which action | one file in the home folder, `~/.claude/keybindings.json` |

None of them gives Claude new abilities and none of them is a guarantee. The documentation says it of styles in as many words: "An output style gives Claude instructions to follow. It doesn't guarantee that something always happens or never happens." For what Claude should know about the project, use CLAUDE.md; for something that has to happen every time, use a hook (page 2); for instructions for one kind of task, a skill (page 1). The documentation matches a style to "Every response in a certain voice, length, or format, or Claude in a different role". Claude Code sends the active style's instructions with every request, which is why a style costs input tokens and is not a one-time setting.

### Output styles

Claude Code starts in the Default style. Four built-in styles keep its standard instructions and add their own: **Proactive** (starts work and assumes on routine decisions instead of asking), **Concise** (leads with the result and drops the lead-in, narration and recap; Claude Code v2.1.237 or later), **Explanatory** (adds short `Insight` blocks that explain the choices behind the code) and **Learning** (the same insights, and a `TODO(human)` comment where Claude asks you to write a piece). A style does not change the permission mode: Proactive still prompts as before.

**Choosing one.** `/output-style <name>` switches (v2.1.269 or later; with no argument it lists the styles), `/config` has an Output style menu, and a settings file holds the choice:

```json
{
  "outputStyle": "Explanatory"
}
```

The command and the menu save the choice to `.claude/settings.local.json`, the local project level. Set `outputStyle` in `~/.claude/settings.json` to make a style your default across projects: "A project's own settings files take precedence over that value." Two traps live in the value. It is case-sensitive: "A value that doesn't match a style name exactly, such as `explanatory`, gives you the Default style", and there is no error. And a style takes effect at your next message: "Claude uses the new style starting with your next message."

**Writing one.** A custom style is a Markdown file: front matter, then the instructions. It lives in `~/.claude/output-styles` (yours), in `.claude/output-styles` (the project; the one closest to the working directory wins if the same name appears in several), or in the managed settings directory. The file name is the style name unless `name` is set. Plugins can ship styles too.

```markdown
---
name: terse-review
description: Lead with the verdict, then the evidence, with no narration
keep-coding-instructions: true
---
Start every reply with the result in one sentence.
After it, give only the evidence the reader needs, one line per finding.
Write at full length whenever the reader asks for an explanation.
```

| Front matter field | What it does |
|---|---|
| `name` | the style's name in the picker; the file name if absent |
| `description` | the line shown in the picker |
| `keep-coding-instructions` | `true` keeps Claude Code's built-in software engineering instructions beside yours; default `false` |
| `force-for-plugin` | plugin styles only: applies the style whenever the plugin is enabled, over the user's `outputStyle` |

`keep-coding-instructions` is the one that costs people: "Custom output styles leave out Claude Code's built-in software engineering instructions, such as how to scope changes, write comments, and verify work", so a style that only changes how replies read, and leaves out the setting, also removes how Claude does engineering work. And a field is checked by nobody: "A misspelled field is ignored without an error." The terminal reads style files when it starts, so a style created in a running session needs a restart to be seen. Styles apply to the main conversation and to a fork, not to other subagents, which run their own system prompt.

### The status line

The status line is "a customizable bar at the bottom of Claude Code that runs any shell script you configure". Claude Code "receives JSON session data on stdin and displays whatever your script prints". It runs locally and "does not consume API tokens".

```json
{
  "statusLine": {
    "type": "command",
    "command": "~/.claude/statusline.sh",
    "padding": 1,
    "refreshInterval": 30
  }
}
```

`/statusline` writes the script and the setting for you from a description such as "show model name and context percentage"; `/statusline delete` removes it. By hand, the `statusLine` object takes `type` (`"command"`), `command` (a script path or an inline command), an optional `padding` in characters (default 0) and an optional `refreshInterval` in seconds (minimum 1). The data on standard input includes `model.display_name`, `workspace.current_dir`, `context_window.used_percentage`, `cost.total_cost_usd`, `output_style.name`, `vim.mode`, `session_id`, `rate_limits.five_hour.used_percentage` and `worktree.name`, among others; a script reads them with a tool such as `jq`.

```sh
#!/bin/sh
input=$(cat)
model=$(echo "$input" | jq -r '.model.display_name')
used=$(echo "$input" | jq -r '.context_window.used_percentage // 0' | cut -d. -f1)
echo "[$model] ${used}% context"
```

When it runs: once when a session starts, then after a new assistant message, after `/compact`, when the permission mode changes, when vim mode toggles, and on the `refreshInterval` timer if you set one; updates are debounced at 300 ms and a slow script that is still running is cancelled by the next update. Event-driven updates go quiet when the main session is idle, which is what `refreshInterval` is for. Two failure modes: a script that calls `git status` in a large repository makes the bar lag, so slow results are cached; and the status line is a shell command from a settings file, so it is under the workspace trust rule of hooks: "Until then, the status line stays blank." It also stays off when `disableAllHooks` is `true`.

### Keybindings

`/keybindings` creates or opens `~/.claude/keybindings.json`. "Changes to the keybindings file are automatically detected and applied without restarting Claude Code." The documentation describes a single bindings file in your home folder, not a project copy. The file is an object with a `bindings` array of blocks; each block names a context and maps keystrokes to actions.

```json
{
  "bindings": [
    {
      "context": "Chat",
      "bindings": {
        "ctrl+e": "chat:externalEditor",
        "ctrl+s": null
      }
    }
  ]
}
```

The rules that an error hides behind:

- **Contexts** are case-sensitive names such as `Global`, `Chat`, `Autocomplete`, `Confirmation`, `Transcript`, `HistorySearch` and `Task`, and an action belongs to a context; actions read `namespace:action` (`chat:submit`, `app:toggleTodos`).
- **Keystrokes** join modifiers with `+` (`ctrl`, `shift`, `alt` or `meta`, `cmd`) and a key; a chord is keystrokes separated by spaces (`ctrl+k ctrl+s`), each pressed within 3 seconds of the one before.
- **`null`** frees the one key it is written against, so a default such as Ctrl+S (stash) stops doing anything. Unbinding every chord on a prefix frees the prefix for a single-key binding.
- **Reserved keys** cannot be rebound: "These shortcuts cannot be rebound." They are Ctrl+C (interrupt), Ctrl+D (exit), Ctrl+M, Ctrl+I, Ctrl+H, Ctrl+[ and Caps Lock; Ctrl+B, Ctrl+A and Ctrl+Z collide with a terminal multiplexer or the shell.
- **Mistakes are quiet.** Claude Code "writes a warning to the debug log" for a parse error, an invalid context, a reserved key, a duplicate. A misspelled modifier is dropped and the binding lands on the key that remains (`ctl+k` becomes `k`). For an unknown action, "Claude Code skips the binding and keeps any default binding for that key in effect." Run `claude --debug` to see them.

### Where each setting lives: the scopes

Settings files have scopes, and the documentation gives their order, highest precedence first. When the same key appears in more than one place, "Claude Code uses the value from the highest level that sets it". The order: managed settings, command line arguments, project local settings (`.claude/settings.local.json`), shared project settings (`.claude/settings.json`), user settings (`~/.claude/settings.json`). "A key at a higher level overrides the same key anywhere below it", while list keys such as `permissions.allow` are combined, not replaced.

| Scope | File | Who it affects | Use it for |
|---|---|---|---|
| User | `~/.claude/settings.json` | you, in every project | personal preferences: theme, editor mode, default model, a style, a status line |
| Shared project | `.claude/settings.json` | everyone who clones the repository | team permissions, hooks, plugins |
| Project local | `.claude/settings.local.json` | you, in this one project | personal overrides, and testing before you share |
| Managed | `managed-settings.json`, MDM or the admin console | everyone in the organization | policy; "Nothing you set overrides them" |

Three consequences for personalisation. First, "Commit `.claude/settings.json` so everyone who clones the repository gets the same permissions, hooks, and plugins", so a style or a status line committed there reaches every teammate and, sitting above the user level, replaces their own choice; a status line in a project file is also a shell command that runs for everyone who trusts the folder. Second, a personal override for one project goes in `.claude/settings.local.json`, which Claude Code keeps out of git when it creates the file; if you create it by hand, add it to `.gitignore` yourself. Third, keybindings have no project scope at all, and an output style file may be shared, but choosing it is each person's.

### Worked examples

**One: terse replies for me, everywhere.** *Features:* a voice, for one person, in every project. *Choice:* a style file in `~/.claude/output-styles` with `keep-coding-instructions: true`, selected with `"outputStyle": "terse-review"` in `~/.claude/settings.json`. *The tempting alternative:* a sentence in CLAUDE.md. It tells Claude something about the project and is shared with the team, and it is not how a voice is switched.

**Two: a team voice for a documentation repository.** *Features:* a voice wanted by several people in one repository. *Choice:* the style file in the repository's `.claude/output-styles`, committed; each person selects it for themselves, in user or local settings. *The tempting alternative:* the style named in the committed `.claude/settings.json`, which overrides what teammates chose.

**Three: context and branch always visible.** *Features:* a display, personal, with data from the session. *Choice:* a script in `~/.claude/` and a `statusLine` in the user settings, created with `/statusline`. *The tempting alternative:* an output style asking Claude to print the percentage; a style is only instructions to Claude, while the script receives the session's own JSON.

**Four: Ctrl+S must stop stashing.** *Features:* a key, personal. *Choice:* `"ctrl+s": null` in the `Chat` block of `~/.claude/keybindings.json`; it applies at once. *The tempting alternative:* a line in CLAUDE.md, or a hook; neither reads keys.

## Traps

1. **A style where a guarantee was needed.** "Always run the formatter" in an output style, a skill or CLAUDE.md is an instruction Claude may not follow; a hook runs itself. A style answers "how should replies read".
2. **A silent no-op.** A name in the wrong case selects the Default style, a misspelled front matter field is ignored, a reserved key cannot be rebound and a misspelled modifier lands on the wrong key; none shows an error on screen. The exam scenario is "the setting did nothing"; look for case, spelling and scope.
3. **A personal setting in a shared file.** The committed project file sits above the user file, so a style or a status line put there overrides every teammate's own choice. Personal choices go in the user file, or in `.claude/settings.local.json` for one project, which is kept out of git.

## The practice

The practice is in [`exercises/39-extending-claude-code`](../../exercises/39-extending-claude-code/unit-03/practice-1/statement.md), third unit. You write one person's setup: a user settings file that selects an output style and a status line, the style file, the status line script, a keybindings file that rebinds one key and frees another, a team's shared project file that holds only permissions, a local override and the ignore file that keeps it out of git. It is graded in Python, TypeScript, Java and Kotlin by test suites that read your files, offline; the statement lists seven cases, each saying what you should see when it works.

## Quiz

1. A developer selects a built-in style in a settings file with `"outputStyle": "explanatory"`. Replies keep Claude Code's standard wording and no error appears. What is the cause?
   - **a**: The value must match a name exactly, or the default applies without a message
   - **b**: The session must be restarted first, because styles are read only at launch
   - **c**: Project files cannot set a style at all, so the value belongs in the user folder
   - **d**: A hook must apply the choice on each reply, because settings only suggest it to Claude

2. A keybindings file sets `ctrl+s` to `null` and `ctrl+c` to a custom action. What results?
   - **a**: Both keys change as written, and a warning appears on the screen at once
   - **b**: Ctrl+C is rebound as written, and Ctrl+S keeps stashing the prompt as before
   - **c**: Ctrl+S is freed, and Ctrl+C keeps its default because it is reserved
   - **d**: Both keys are freed together, and Ctrl+C then does nothing at all in the chat

3. One developer's `~/.claude/settings.json` names an output style, while the repository's `.claude/settings.json` names a different one. Which does the session use?
   - **a**: The personal choice, because a developer's own file always wins for that person
   - **b**: The project's choice, because that level outranks the user level
   - **c**: Whichever file was saved last, because Claude Code compares modification times
   - **d**: Neither choice, because a key set at two levels is rejected as a conflict

<details>
<summary>Answer key</summary>

1. **a**. The page says "A value that doesn't match a style name exactly, such as `explanatory`, gives you the Default style", with no error. *b* is ruled out because "Claude uses the new style starting with your next message", and a built-in style needs no restart; only a style file created in a running session does. *c* is ruled out because "A project's own settings files take precedence over that value", so a project file can set a style. *d* is ruled out because a style is applied by Claude Code itself: "Claude Code sends the active style's instructions with every request", and no hook is involved.
2. **c**. The page says `null` "frees the one key it is written against", and that reserved keys cannot be rebound: "These shortcuts cannot be rebound." *a* is ruled out because Claude Code "writes a warning to the debug log", not to the screen, and the reserved key does not change. *b* is ruled out because `null` "frees the one key it is written against", so Ctrl+S stops stashing. *d* is ruled out because "These shortcuts cannot be rebound", and Ctrl+C is reserved, so it keeps interrupting.
3. **b**. The page says "Claude Code uses the value from the highest level that sets it", and the shared project settings sit above the user settings. *a* is ruled out because "A key at a higher level overrides the same key anywhere below it", and the user level is the lowest of the files. *c* is ruled out because the documentation gives "their order, highest precedence first", and no file date is involved. *d* is ruled out because "Claude Code uses the value from the highest level that sets it", so two levels are resolved by order and not rejected.

</details>
