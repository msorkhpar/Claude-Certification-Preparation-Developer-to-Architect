# Practice: choose the extension a situation calls for, and say why

A team adds an instruction file for a rule that must never be broken, a skill for a database that needs a connection, and a plugin for a single sentence. Each choice looks reasonable
and fails in its own way. In this practice you write the decision function of the page "Choosing in practice": a situation described by a few features goes in, and the
mechanism that fits and a reason code come out. The model is not called and nothing is installed. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open
`starter/` and edit the file there.

Python has `choose(situation)` in `extension_choice.py`; TypeScript has `choose` in `extensionChoice.ts`; Java has the static method `ExtensionChoice.choose` and the record `Choice`;
Kotlin has the top-level function `choose` and the data class `Choice`. Python and TypeScript take a plain object, and Java and Kotlin take a map from the key to its value. The keys
are the same strings in every language, and a missing key has the default shown below. The page lists the eighteen situations of the scenario bank in words; the tests hold the same eighteen as features.

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

The rules apply in this order:

1. An unknown `surface`, an unknown `knowledge` kind or `repos` below 1 is an error, on either surface. Say so with the language's usual argument error (`ValueError` in Python, `Error` in
   TypeScript, `IllegalArgumentException` in Java and Kotlin).
2. On `api`, nothing else about Claude Code applies, and the first match wins: `builtin_covers` gives `builtin-tool`; `external_system` together with `remote_server` gives `mcp`; anything else
   gives `api-tool`.
3. On `code`, the first match wins: `guarantee` gives `hook`; `external_system` gives `mcp`; `noisy` gives `subagent`; `knowledge` `convention` gives `path-rule` when `path_scoped` and `claude-md` when
   not; `reference` gives `skill` with `on-demand-reference`; `procedure` gives `skill` with `repeatable-procedure`; otherwise `builtin-tool`.
4. Then, when `repos` is 2 or more and the mechanism is a skill, a hook, a subagent or an MCP server, the answer becomes `plugin` with `shared-setup`. An instruction file and a path rule stay
   as they are, because a plugin cannot carry one, and a built-in tool has nothing to package.

## Why each part is there, and what you should see

1. **A guarantee outranks everything.** A sentence in an instruction file or a skill is a request. *You should see* `hook` even when the rule also comes with a convention, an outside system and noisy work.
2. **Access is not knowledge.** An outside system needs a connection, which a skill cannot hold. *You should see* `mcp` whatever else is true, and `subagent` only when nothing else claims the work.
3. **Load time decides the file.** A convention for everything is paid for on every request; one for some files is loaded when those files are used; reference and procedure wait until used. *You should see* the four answers, and a path scope ignored for anything that is not a convention.
4. **A plugin is for carrying.** *You should see* `plugin` from the second repository onward for the four kinds a plugin bundles, and never for an instruction file.
5. **An application is not Claude Code.** *You should see* the API branch ignore every Claude Code feature, and a plugin never appear there.
6. **Unknown means error.** *You should see* an error for a value that is not in the table, and the default for a key that is missing.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Each of the eighteen situations of the scenario bank gets its mechanism and its reason code |
| `e1` | A rule that must hold goes to a hook whatever else is true |
| `e2` | An outside system needs a server, and noisy work alone needs a subagent |
| `e3` | Knowledge goes to the file or skill that loads it at the right time |
| `e4` | A plugin carries a skill, hook, subagent or server to a second repository and nothing else |
| `e5` | In an application the platform may supply the schema, and only a remote server replaces your own tool |
| `e6` | An unknown value is an error and a missing key takes its default |

Run the tests with the command in the language folder's `run.sh`.
