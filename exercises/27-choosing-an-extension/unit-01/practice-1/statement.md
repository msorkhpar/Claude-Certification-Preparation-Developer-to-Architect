# Practice: choose the extension a situation calls for, and say why

A team adds an instruction file for a rule that must never be broken, a skill for a database that needs a connection, a loop for work that must run while the laptop is closed, and a
response style for a status bar. Each choice looks reasonable and fails in its own way. In this practice you write the decision function of the page "Choosing in practice": a situation described by a few features goes in, and the
mechanism that fits and a reason code come out. The model is not called and nothing is installed. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open
`starter/` and edit the file there.

Python has `choose(situation)` in `extension_choice.py`; TypeScript has `choose` in `extensionChoice.ts`; Java has the static method `ExtensionChoice.choose` and the record `Choice`;
Kotlin has the top-level function `choose` and the data class `Choice`. Python and TypeScript take a plain object, and Java and Kotlin take a map from the key to its value. The keys
are the same strings in every language, and a missing key has the default shown below. The page lists the twenty-eight situations of the scenario bank in words; the tests hold the same twenty-eight as features.

## What is already written, and what you write

The starter is a working chooser with eight gaps cut out of it. Everything that is plumbing is written and correct: the constants, the checks of the five
named values (`surface`, `knowledge`, `timing`, `presence`, `personal`), the reading of the defaults, the rhythm rules for a pipeline, a condition and a background command, and the order in which
the answers are tried. Each gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap
returns a neutral value (nothing, so the chooser falls back to `builtin-tool`), so the starter runs and fails the cases on an assertion. The names below are Python's; TypeScript, Java and Kotlin
have the camel-case forms (`checkCounts`, `apiChoice`, `priorityChoice`, `eventChoice`, `intervalChoice`, `personalChoice`, `knowledgeChoice`, `packageChoice`). Write them in this order:

1. `_check_counts` unlocks `e6`: `repos` or `lasts_days` below 1, and `presence` `away` with `local_files`, are errors.
2. `_api_choice` unlocks `e5`: the platform's tool, the remote server, or nothing (the caller answers `api-tool`).
3. `_priority_choice` unlocks `e1` and `e2`: a guarantee, an outside system and noisy work, in that order.
4. `_event_choice` unlocks `e7`: an event is a routine when nobody is there, a monitor otherwise.
5. `_interval_choice` unlocks `e8`: a loop, a routine or a desktop task for work that repeats.
6. `_personal_choice` unlocks `e9`: a voice, a display or a key binding.
7. `_knowledge_choice` unlocks `e3`: the instruction file, the path rule or the skill that loads the knowledge at the right time.
8. `_package` unlocks `e4`: a plugin from the second repository onward, for what a plugin can carry.

`m1` needs all eight. About twenty-five lines in all. To see what a gap receives, log its input with the `log` line at the top of the file; a run shows the lines under the failing case.

## What to write

`choose(situation)` returns `{mechanism, reason}`. A situation has these keys:

| Key | Meaning | Default |
|---|---|---|
| `surface` | `code` for a Claude Code session, `api` for an application that calls the Messages API | `code` |
| `guarantee` | the action must happen, or be blocked, every time, with no judgment | false |
| `external_system` | the work needs a system outside the machine, such as a database or a ticketing service | false |
| `builtin_covers` | on `api`, the platform already supplies a tool with its own schema for the job | false |
| `remote_server` | on `api`, a hosted server that speaks the protocol for the external system already exists | false |
| `knowledge` | what Claude must be told: `none`, `convention`, `reference` or `procedure` | `none` |
| `path_scoped` | the knowledge applies only to some files | false |
| `noisy` | the work reads or prints a lot that nobody needs afterwards, only its conclusion | false |
| `repos` | how many repositories need the same setup, at least 1 | 1 |
| `timing` | when the work runs: `none`, `interval` (every so often), `event` (when something happens), `condition` (until a check holds) or `background` (a long command Claude does not wait for) | `none` |
| `presence` | who is there: `session` (a Claude Code session stays open), `pipeline` (a CI job, nobody to answer a prompt) or `away` (the computer may be off) | `session` |
| `lasts_days` | how long a timed job must keep running, at least 1 | 1 |
| `local_files` | the job needs files on this machine, uncommitted changes included | false |
| `personal` | a preference of one person: `none`, `voice` (how every reply is written), `display` (what the bottom bar shows) or `keys` (a keyboard shortcut) | `none` |

The mechanisms, with their reason codes:

| Mechanism | Reason code | When |
|---|---|---|
| `hook` | `must-hold-every-time` | `guarantee` |
| `mcp` | `external-system` | an outside system, in a Claude Code session |
| `subagent` | `isolate-context` | noisy work |
| `path-rule` | `scoped-convention` | a convention for some files |
| `claude-md` | `always-known` | a convention for the whole project |
| `skill` | `on-demand-reference` | reference material needed sometimes |
| `skill` | `repeatable-procedure` | a procedure someone starts by name |
| `builtin-tool` | `built-in-covers` | nothing is needed beyond what Claude Code has |
| `plugin` | `shared-setup` | a skill, hook, subagent or server wanted in a second repository |
| `api-tool` | `own-schema-and-code` | an application that defines the tool and runs its code |
| `builtin-tool` | `provided-schema` | an application whose platform supplies the schema |
| `mcp` | `remote-server` | an application that reaches an existing remote server |
| `loop` | `session-rhythm` | an interval job in an open session that lasts up to seven days |
| `routine` | `runs-unattended` | an interval or event job that must run with the computer off, or an interval job of more than seven days that needs no local files |
| `desktop-task` | `durable-and-local` | an interval job of more than seven days that needs local files |
| `monitor` | `push-not-poll` | react to each line of an event stream in the session |
| `background-task` | `work-while-it-runs` | a long command Claude starts and does not wait for |
| `headless-ci` | `no-person-present` | a job in a pipeline |
| `goal` | `until-condition-holds` | keep working until a check holds |
| `output-style` | `response-voice` | a person wants every reply in a voice, length or format |
| `status-line` | `personal-display` | a person wants something shown in the bottom bar |
| `keybinding` | `personal-keys` | a person wants a different keyboard shortcut |

The rules apply in this order:

1. An unknown `surface`, `knowledge`, `timing`, `presence` or `personal` value, `repos` or `lasts_days` below 1, or `presence` `away` together with `local_files` (a cloud run starts from a fresh clone) is an error, on either surface. Say so with the language's usual argument error (`ValueError` in Python, `Error` in
   TypeScript, `IllegalArgumentException` in Java and Kotlin).
2. On `api`, nothing else about Claude Code applies, and the first match wins: `builtin_covers` gives `builtin-tool`; `external_system` together with `remote_server` gives `mcp`; anything else
   gives `api-tool`.
3. On `code`, the first match wins: `guarantee` gives `hook`; `external_system` gives `mcp`; `noisy` gives `subagent`; then work that runs without a person or on a rhythm:
   `presence` `pipeline` gives `headless-ci`; `timing` `condition` gives `goal`; `background` gives `background-task`; `event` gives `monitor`, or `routine` when `presence` is `away`; `interval`
   gives `routine` when `presence` is `away`, then `desktop-task` when `lasts_days` is above 7 and `local_files`, `routine` when `lasts_days` is above 7, and `loop` otherwise; then `personal`
   `voice` gives `output-style`, `display` gives `status-line` and `keys` gives `keybinding`; then `knowledge` `convention` gives `path-rule` when `path_scoped` and `claude-md` when
   not; `reference` gives `skill` with `on-demand-reference`; `procedure` gives `skill` with `repeatable-procedure`; otherwise `builtin-tool`.
4. Then, when `repos` is 2 or more and the mechanism is a skill, a hook, a subagent or an MCP server, the answer becomes `plugin` with `shared-setup`. An instruction file and a path rule stay
   as they are, because a plugin cannot carry one, and a built-in tool has nothing to package. The new mechanisms of rule 3 (loops, routines, tasks, goals, styles, the status line and key
   bindings) are not packaged by this function.

## Why each part is there, and what you should see

1. **A guarantee outranks everything.** A sentence in an instruction file or a skill is a request. *You should see* `hook` even when the rule also comes with a convention, an outside system and noisy work.
2. **Access is not knowledge.** An outside system needs a connection, which a skill cannot hold. *You should see* `mcp` whatever else is true, and `subagent` only when nothing else claims the work.
3. **Load time decides the file.** A convention for everything is paid for on every request; one for some files is loaded when those files are used; reference and procedure wait until used. *You should see* the four answers, and a path scope ignored for anything that is not a convention.
4. **A plugin is for carrying.** *You should see* `plugin` from the second repository onward for the four kinds a plugin bundles, and never for an instruction file.
5. **An application is not Claude Code.** *You should see* the API branch ignore every Claude Code feature, and a plugin never appear there.
6. **Time is a feature of its own.** A pipeline job, a condition, a long command and an event are not intervals, and an interval is a loop only while a session stays open and for seven days. *You
   should see* `headless-ci`, `goal`, `background-task`, `monitor`, then `loop`, `routine` or `desktop-task` for an interval, and the hook, server and subagent rules still ahead of all of them.
7. **A preference is personal.** How replies are written, what the bar shows and which key does what belong to one person's setup, not to knowledge. *You should see* `output-style`,
   `status-line` and `keybinding`, and a convention still going to the instruction file when there is no preference.
8. **Unknown means error.** *You should see* an error for a value that is not in the table, and the default for a key that is missing.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Each of the twenty-eight situations of the scenario bank gets its mechanism and its reason code |
| `e1` | A rule that must hold goes to a hook whatever else is true |
| `e2` | An outside system needs a server, and noisy work alone needs a subagent |
| `e3` | Knowledge goes to the file or skill that loads it at the right time |
| `e4` | A plugin carries a skill, hook, subagent or server to a second repository and nothing else |
| `e5` | In an application the platform may supply the schema, and only a remote server replaces your own tool |
| `e6` | An unknown value is an error and a missing key takes its default |
| `e7` | A pipeline, a condition, a long command and an event are not intervals |
| `e8` | An interval is a loop in the session, and a routine or desktop task when it must outlive it |
| `e9` | A personal preference goes to a style, a status line or a key binding, and knowledge keeps its own rules |

Run the tests with the command in the language folder's `run.sh`.
