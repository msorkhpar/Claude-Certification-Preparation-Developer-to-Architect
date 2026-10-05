# Building a plugin and publishing it through your own marketplace

**Level:** Developer · **Module 39:** Extending Claude Code · **Page 4 of 5**
**Exams:** DV1, DV7; A3.2

**After this page you can** build a plugin that bundles a skill, a hook and an MCP server, say where its version lives, write the `marketplace.json` that lists it, choose between the source kinds and pin each to a tag and a commit, declare dependencies inside and across marketplaces, give a team the marketplace through the repository's settings, rename or retire a plugin without breaking installs, and say how a marketplace that an administrator requires differs (module 92 teaches that side).

Checked on 2026-10-04 against the Claude Code documentation (create a marketplace, the marketplace reference, the plugin manifest reference, plugin dependencies, the plugin loading reference, host and maintain a marketplace, and manage plugins for your organization), which mention behaviour up to Claude Code v2.1.288. Nothing was installed and no `claude plugin` command was run for this page: every statement about Claude Code is read from those pages. The practice tests run offline in the course container and check the files you write against the rules quoted here.

## Why it matters

Page 3 packaged one plugin and shared it from a repository's settings. A team that ships several plugins needs the layer above: a catalog, with a name for each plugin, a place to fetch it from, a way to hold users on a tested version, and a way to rename a plugin later. The exam asks where a version lives, what a pinned source looks like, why a dependency from another marketplace was refused, and why the plugin a contributor enabled did not install. All of these are answered by the files.

## The idea

### A plugin that is ready to publish

The manifest is optional, since "Without it, Claude Code loads the components it finds in the standard layout", and a team writes one for the metadata, the version and the dependencies. The documentation fixes where each component lives:

| Component | Default location |
|---|---|
| Manifest | `.claude-plugin/plugin.json` |
| Skills | `skills/<name>/SKILL.md` |
| Commands | `commands/` (flat Markdown files; prefer skills for new plugins) |
| Agents | `agents/` |
| Hooks | `hooks/hooks.json` |
| MCP servers | `.mcp.json` |

Four rules keep the files loadable. "Put every other plugin file at the plugin root, not inside `.claude-plugin/`." A hooks file "wraps the event map in a top-level `"hooks"` key", and "A file that contains only the event map, without that wrapper, fails to load." Every component path in a manifest "must start with `./`", and a path outside the plugin does not load. And a script or a server is reached through `${CLAUDE_PLUGIN_ROOT}`, which is the "Absolute path of the plugin's installed version", because the cached copy lives in a folder named after the version and its path changes with every release. A `CLAUDE.md` at the plugin root is not loaded as context, so instructions that Claude must read go into a skill.

Every component is namespaced under the plugin's name: an agent `reviewer` in a plugin `deploy-tools` "appears as `deploy-tools:reviewer`". A manifest key can replace or add to a default folder. `commands`, `agents` and `outputStyles` replace the default scan, `skills` adds to it, and `hooks` and `mcpServers` merge with their default files.

Before publishing, load the plugin for one session with `--plugin-dir`, and run `claude plugin validate ./my-plugin`, which reports a missing path, a manifest field that Claude Code strips, and a name that is not kebab-case.

### Where the version lives

A version is how Claude Code detects an update, and the documentation gives the order: "The `version` field in the plugin's manifest comes first", then the entry's `version`, and then, "When neither is set", the commit SHA of the source for a git source. This gives two ways to release.

- **Bump the version on each release**: users stay on their cached copy until the string changes. If you set `"version": "1.0.0"` and push new commits without changing it, "users don't receive them".
- **Omit the version**: "users track your commits instead".

"Don't set `version` in both `plugin.json` and the marketplace entry": Claude Code then uses the manifest value without warning, and `claude plugin validate` reports the mismatch. The course's practice keeps the version in the manifest alone and requires three parts, such as `1.0.0`, although the documentation says a version is "not checked against semver", because a dependency range is a semantic-version range. A plugin that others depend on is tagged `<plugin-name>--v<version>`, for example by `claude plugin tag --push`, since "A constraint resolves against git tags".

### The marketplace file

"A plugin marketplace is a directory or repository with a `.claude-plugin/marketplace.json` file that lists your plugins and where to fetch each one." The file "requires a `name`, an `owner`, and a `plugins` array", and each entry "needs a `name` and a `source`". The marketplace name is what users type after the `@` in `plugin@marketplace`, so it may use only letters, digits, `.`, `_` and `-`, starting with a letter or digit. A name cannot be one of the reserved official names (`claude-plugins-official` is one), and "Names that impersonate an official marketplace" are refused as well.

Two names appear for one plugin, and they must agree. The entry name is the install id and "the key Claude Code writes under `enabledPlugins`"; the manifest name is the prefix of its skills. "When the two names differ and someone installs by the manifest name, Claude Code reports `Plugin "<manifest-name>" not found in marketplace "<marketplace>"`."

```json
{
  "name": "example-org-tools",
  "description": "Plugins for the platform team",
  "owner": { "name": "Example Org Platform Team" },
  "plugins": [
    { "name": "standards-kit", "source": "./plugins/standards-kit", "description": "Changelog skill and edit guard" }
  ]
}
```

### Sources, and pinning

An entry's `source` says where to fetch that one plugin. The three kinds most teams use:

| Source | Use it when | Example value |
|---|---|---|
| Relative path | The plugin is inside the marketplace repository | `"./plugins/standards-kit"` |
| `github` | The plugin is a repository of its own | `{ "source": "github", "repo": "example-org/lint-helper" }` |
| `git-subdir` | The plugin is a folder of another repository, such as a monorepo | `{ "source": "git-subdir", "url": "example-org/monorepo", "path": "tools/db-tools" }` |

Others exist: `url` (a git repository by URL on any host, which "doesn't take `owner/repo` shorthand"), `npm`, `archive` (a zip over HTTPS, pinned with `sha256`) and `command`. A relative path "resolves from the marketplace root", must start with `./`, and cannot contain `..`. It works only when Claude Code has the marketplace's files: a marketplace added as a bare `marketplace.json` URL downloads only that file, so each entry there needs a source that can be fetched on its own.

`github`, `url` and `git-subdir` sources share two pins. "`ref`: a branch or tag." "`sha`: a full 40-character lowercase commit SHA. When you set both `ref` and `sha`, Claude Code checks out `sha`." A tag can be moved and a commit cannot, so the practice pins both: the tag says what the commit is, and the commit is what is installed.

### Dependencies, inside and across marketplaces

A dependency goes in the `dependencies` array of the manifest, or of the marketplace entry. A string tracks whatever version the marketplace offers, and an object adds a range: `{ "name": "lint-helper", "version": "~1.2.0" }`. "Without a version constraint, a dependency moves to each new release its marketplace publishes the next time users update." The dependency "installs at the highest git tag that satisfies this range", so the maintainer of the dependency must tag releases.

A dependency from another marketplace is refused by default: "By default, Claude Code doesn't install a dependency from a different marketplace than the declaring plugin's own". The root marketplace, the one that hosts the plugin being installed, names the other one in `allowCrossMarketplaceDependenciesOn`, and "Only the root marketplace's allowlist applies." When the dependency is declared in the entry the install is refused with a message; when it is declared in `plugin.json` the install completes without it and the plugin then fails to load.

### Renaming and retiring

A plugin's `name` is its identifier, and "changing it breaks every existing install". The label users see changes through `displayName`. When a name must change, a top-level `renames` map migrates users: it maps each former name to the current one, or to `null` for a plugin that is gone, and "Claude Code rewrites the old key to the new one in `enabledPlugins`". `claude plugin validate` rejects a chain that cycles or that ends anywhere other than `null` or a name in `plugins`. With `forceRemoveDeletedPlugins: true` a removed plugin is also uninstalled from users' machines.

### Giving a team the marketplace

Host the marketplace in a git repository. Teammates run `claude plugin marketplace add example-org/team-marketplace`, or the repository carries the registration. Running the add command once with `--scope project` and committing the `.claude/settings.json` it writes registers the marketplace for everyone who works in the repository, and `enabledPlugins` there turns plugins on. Two details decide whether it works:

- Registration waits for trust. "The `extraKnownMarketplaces` entries apply only in a folder the contributor has trusted, and in an untrusted folder Claude Code ignores them without a message."
- Only a relative-path plugin installs from the file alone. "A plugin whose marketplace entry points at an external source instead, such as the plugin's own GitHub repository, doesn't install from the repository's settings alone." Each contributor sees `Plugin "<name>" is enabled in project settings but isn't installed here` in the `/plugin` Errors tab until they run `claude plugin install <name>@<marketplace> --scope project`. Nothing installs, and nothing prompts, until each contributor runs the install. (A seed directory that already holds the plugin is the other exception.) This is why the practice enables only the plugin whose source is a relative path.

A private marketplace needs no field for credentials: "Claude Code has no git token of its own, and `marketplace.json` has no field for one." It clones with the git credentials the user's machine already holds. Background auto-update is "off for your marketplace until a user or admin turns it on", so without it users receive changes when they run `/plugin marketplace update` or `claude plugin update`.

### A managed marketplace is a different channel

Module 92 teaches the administrator's side. The difference in one paragraph: an administrator puts the same two keys, `extraKnownMarketplaces` and `enabledPlugins`, in managed settings, and "Users can't override them". There `true` force-enables a plugin and `false` blocks it at every scope; a separate allowlist, `strictKnownMarketplaces`, decides which marketplace sources may be added at all. A repository's own settings reach only the contributors who trust the folder, and a person can switch a plugin off locally. A marketplace you host for a team and a marketplace an administrator requires are the same file, reached by different channels with different power.

## Traps

1. **Setting the version twice.** The manifest wins without a warning, and the entry's value is dead text. Keep it in one place.
2. **A moved tag.** A pin by `ref` alone follows the tag if someone moves it. Pin the commit with `sha`.
3. **Enabling an external plugin from the repository's settings and expecting an install.** Only a relative-path plugin installs from the file; the others wait for each contributor's own install.

## The practice

The practice is in [`exercises/39-extending-claude-code`](../../exercises/39-extending-claude-code/unit-02/practice-1/statement.md), second unit. You write the marketplace repository of a platform team: the marketplace file with its three entries (a relative path, a `github` source and a `git-subdir` source, the last two pinned to a tag and a commit), the manifest of the plugin inside it with a version and a ranged dependency, its skill, hook file and server file, a cross-marketplace dependency with its allowlist, a rename map, and the team's settings. It is graded in Python, TypeScript, Java and Kotlin by test suites that read your files, offline; the statement lists nine cases, each saying what you should see when it works.

## Quiz

1. A team pushes fresh commits to a plugin whose manifest still says `"version": "1.0.0"`. What reaches installed users?
   - **a**: The copy they already hold, until the number is raised
   - **b**: The fresh commits at the next launch, since a push always counts
   - **c**: The fresh commits once the marketplace entry repeats that number
   - **d**: The fresh commits, since the commit hash is the version

2. A catalog entry calls a bundle `reviewer-kit`, while the bundle's own manifest says `review-kit`. What happens when someone installs it under the manifest's name?
   - **a**: An ambiguity message asks which of the two names was meant
   - **b**: A not-found message naming what was typed
   - **c**: A success message, with the entry name as the skill prefix
   - **d**: A success message, once the catalog has been added again

3. A repository's settings turn on a bundle whose catalog entry points at a repository of its own. A contributor trusts the folder and starts a session. What do they see?
   - **a**: The bundle loaded, since trust is the only condition that applies
   - **b**: A prompt that lists the hooks of the bundle before it installs
   - **c**: A notice that it is enabled but not installed, until they install it
   - **d**: The bundle loaded once the marketplace is added to managed settings

<details>
<summary>Answer key</summary>

1. **a**. The page says users stay on their cached copy until the string changes. *b* is ruled out because if the version is not changed, "users don't receive them". *c* is ruled out because "Don't set `version` in both `plugin.json` and the marketplace entry", and Claude Code "then uses the manifest value without warning". *d* is ruled out because the commit hash counts only when neither place sets a version: "When neither is set", the commit SHA of the source.
2. **b**. The page says "When the two names differ and someone installs by the manifest name, Claude Code reports `Plugin "<manifest-name>" not found in marketplace "<marketplace>"`." *a* is ruled out because the entry name is "the key Claude Code writes under `enabledPlugins`", and nothing offers a choice between the two names. *c* is ruled out because "the manifest name is the prefix of its skills", and the entry name never replaces it. *d* is ruled out because "When the two names differ and someone installs by the manifest name, Claude Code reports" a failure, and re-adding the catalog changes neither name.
3. **c**. The page says "Each contributor sees `Plugin "<name>" is enabled in project settings but isn't installed here` in the `/plugin` Errors tab until they run `claude plugin install <name>@<marketplace> --scope project`." *a* is ruled out because "Only a relative-path plugin installs from the file alone". *b* is ruled out because "Nothing installs, and nothing prompts, until each contributor runs the install." *d* is ruled out because "A repository's own settings reach only the contributors who trust the folder", which is a different channel from managed settings.

</details>

## Module quiz

This quiz covers all five pages of the module.

1. A team needs one rule enforced for every command, with an explanation given to Claude, and the rule must hold in every project that installs their tooling. Which design fits?
   - **a**: A skill in each repository that tells Claude to avoid the action
   - **b**: A hook in a plugin that blocks with exit code 2 and a reason
   - **c**: A subagent in the plugin that reviews each command before it runs
   - **d**: A line in each memory file that forbids the action by its name

2. One skill drafts release summaries and another tags and ships a release. Which pair of settings fits?
   - **a**: The first for a person alone, the second on automatic loading
   - **b**: Both on automatic loading, so that Claude can finish unaided
   - **c**: The first on automatic loading, the second for a person alone
   - **d**: Both for a person alone, so that neither is ever loaded at all

3. A repository's `.claude/settings.json` registers a marketplace and enables a plugin, but a new contributor has not yet approved the folder. What happens?
   - **a**: The entries apply only to managed settings, never to a repository
   - **b**: The plugin installs at once, because the file is in the repository
   - **c**: A prompt appears for each plugin, listing its hooks and its scripts
   - **d**: Claude Code ignores the entries without a message until trust is given

4. A subagent should review code and remember wording the team prefers, with that knowledge committed with the code. Which fields fit?
   - **a**: Every tool and the user memory scope
   - **b**: Read-only tools and the project memory scope
   - **c**: Read-only tools and the local memory scope
   - **d**: Every tool and no memory field at all

<details>
<summary>Answer key</summary>

1. **b**. The hooks section gives "deterministic control: certain actions always happen rather than relying on the LLM to choose to run them", exit 2 blocks and "Write a reason to stderr", and a plugin carries the hook to every project that enables it. *a* is ruled out because "a skill is instructions Claude may follow", and not a rule that holds. *c* is ruled out because a subagent is "An isolated context that returns summarised results" and is not a gate for every command. *d* is ruled out because a memory file holds "Persistent context loaded every conversation", which is guidance and not enforcement.
2. **c**. The page says "A skill that writes release notes is harmless and Claude can start it when asked", while a skill with side effects needs `disable-model-invocation: true`. *b* is ruled out because for a skill with side effects "a person should start it". *a* is ruled out because it reverses the safety, and the first sentence of the page's reasoning is "A skill that writes release notes is harmless and Claude can start it when asked". *d* is ruled out because "Claude loads it by itself when your request matches its description", and the harmless skill gains from that.
3. **d**. The page says the entries "apply only in a folder the contributor has trusted, and in an untrusted folder Claude Code ignores them without a message." *b* is ruled out because trust comes first: "A repository cannot install code on a machine that has not trusted it." *c* is ruled out because the page says "Claude Code ignores them without a message", which describes no prompt. *a* is ruled out because the page says "the same two keys go in managed settings" for a whole organisation, and the repository's file covers its own contributors.
4. **b**. The page says a reviewer needs `Read`, `Grep` and `Glob`, and that the `project` scope is for knowledge that is "shareable via version control". *a* is ruled out because "Omit it and the subagent inherits every tool", and the `user` scope lives in the home directory and is not committed. *c* is ruled out because the `local` scope is for knowledge that "should not be checked in". *d* is ruled out because "Omit it and the subagent inherits every tool", and without `memory` the subagent keeps no knowledge across conversations.

</details>
