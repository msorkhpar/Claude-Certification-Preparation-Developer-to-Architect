# Practice: the marketplace of a platform team

A platform team ships one plugin from its own repository and lists two more that live elsewhere. Each of these goes wrong quietly: an entry named differently from its manifest cannot be installed by the manifest's name, a version set in two places is read from one without a warning, a hooks file without its wrapper fails to load, a dependency from another marketplace is refused unless the root marketplace allows it, and a plugin from an external source is not installed by the repository's settings alone. In this practice you write the files of the page "Building a plugin and publishing it through your own marketplace". Nothing is installed and Claude Code is not started: the tests read your files and check them against the documented rules. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the files there. The folder is the root of the marketplace repository.

| File | What it holds |
|---|---|
| `.claude-plugin/marketplace.json` | the catalog: name, owner, entries, the dependency allowlist and the renames |
| `.claude/settings.json` | the team's settings that register the marketplace and enable a plugin |
| `plugins/standards-kit/.claude-plugin/plugin.json` | the manifest of the plugin that lives inside the marketplace |
| `plugins/standards-kit/skills/changelog/SKILL.md` | a skill that drafts a changelog entry |
| `plugins/standards-kit/hooks/hooks.json` | the hook registration |
| `plugins/standards-kit/scripts/protect.sh` | the guard script the hook runs (given; it only has to stay in place) |
| `plugins/standards-kit/.mcp.json` | the plugin's MCP server |
| `plugins/standards-kit/servers/tickets.js` | the server's code (given; a stand-in) |

The starter holds the files unfinished. All JSON files are strict JSON: a comment or a trailing comma is an error.

## What to write

1. **The marketplace file.** A `name` of `example-org-tools`, a `description`, an `owner` with a `name` (an e-mail, if you give one, ends in `@example.com`) and a `plugins` array of three entries,
   `standards-kit`, `lint-helper` and `db-tools`. Entry names are unique and use letters, digits, dots, underscores and hyphens only. No plugin name, and not the marketplace name,
   may be reserved or pass as an Anthropic or official one.
2. **The three sources.** `standards-kit` is a relative path, `"./plugins/standards-kit"`: it starts with `./`, stays inside the marketplace and names a folder that exists. `lint-helper` is a
   `github` source with `repo` written `owner/name`. `db-tools` is a `git-subdir` source with a `url` and a `path` of `tools/db-tools`. The two external sources are pinned twice: `ref` is a release
   tag such as `v1.2.0` and `sha` is a full 40-character lowercase commit.
3. **The manifest.** `name` `standards-kit`, a three-part `version` such as `1.0.0` and a `description`. The version lives here and not in the marketplace entry. Only `plugin.json` goes inside
   `.claude-plugin/`. The manifest depends on `lint-helper` through the object form `{"name", "version"}` with the range `~1.2.0`.
4. **The components.** The skill `changelog` has front matter with `name` and a `description` that says when to use it (`Use when ...`), and a body with no `TODO`. `hooks.json` wraps the event map in a
   top-level `hooks` key and holds one `PreToolUse` group with the matcher `Edit|Write` and one `command` handler that reaches `scripts/protect.sh` through `${CLAUDE_PLUGIN_ROOT}`. `.mcp.json` declares
   a server whose arguments reach its code through `${CLAUDE_PLUGIN_ROOT}`. There is no `CLAUDE.md` at the plugin root: it is not loaded as context.
5. **The dependency across marketplaces.** The `db-tools` entry depends on `audit-logger` from the marketplace `shared-tools`, and the root marketplace names `shared-tools` in
   `allowCrossMarketplaceDependenciesOn`.
6. **The team settings.** `extraKnownMarketplaces` registers the marketplace under its own name from a `github` source (`owner/name`), and `enabledPlugins` turns on `standards-kit@example-org-tools`. Only
   plugins of that marketplace are enabled, and only ones whose entry is a relative path.
7. **The renames.** `renames` maps the former name `std-kit` to `standards-kit` and `old-linter` to `null` (retired). A former name is not also a current entry, and every chain of renames ends
   at a current plugin or at `null`.
8. **Nothing personal, nothing unfinished.** No file holds a home folder path, an e-mail address that is not `@example.com`, a key that starts like an API key, or a `TODO` marker.

## Why each part is there, and what you should see

1. **The catalog is complete.** *You should see* an owner, a description and an entry for each plugin, each with a source that resolves.
2. **Names are installable.** *You should see* the marketplace and each plugin name valid in a plugin id, and none that an install could mistake for an official one.
3. **Files are found wherever the plugin is cached.** *You should see* only the manifest in `.claude-plugin/`, the wrapper key in `hooks.json` and `${CLAUDE_PLUGIN_ROOT}` in every path the plugin runs.
4. **One version.** *You should see* the version in the manifest alone, in three parts.
5. **A tag says what, a commit says which.** *You should see* each source kind written in its own form and pinned to a tag and a full commit.
6. **Dependencies are bounded.** *You should see* a range on the local dependency and the other marketplace allowed by name.
7. **Settings install what they can.** *You should see* the marketplace registered under its own name and only a relative-path plugin enabled.
8. **Renames do not dangle.** *You should see* every former name end at a current plugin or at `null`.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The marketplace file names an owner and lists each plugin by a source that resolves |
| `e1` | Names are valid in a plugin id and are not reserved or mistaken for official ones |
| `e2` | The plugin keeps only its manifest in the manifest folder and finds its files through the plugin root |
| `e3` | The version lives in the manifest alone and is semantic |
| `e4` | Each source kind is written in its own form and pinned to a tag and a commit |
| `e5` | A dependency carries a range, and one from another marketplace is allowed by name |
| `e6` | The team settings register the marketplace and enable only what installs from it |
| `e7` | Renames lead every former name to a current plugin or to `null` |
| `e8` | No file is left unfinished or holds a personal path, an address or a key |

Run the tests with the command in the language folder's `run.sh`.
