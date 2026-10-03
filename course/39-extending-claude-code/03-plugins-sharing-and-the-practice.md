# Plugins, dependencies, sharing and the practice

**Level:** Developer · **Module 39:** Extending Claude Code · **Page 3 of 3**
**Exams:** DV1, DV7; A3.2

**After this page you can** package skills, agents and hooks as a plugin, declare a dependency with a version range, share a plugin with a team through the repository's settings, say what a plugin may and may not configure, and finish the module's practice.

Checked on 2026-10-03 against the Claude Code documentation (plugins: create, manifest, dependencies, install, security and components; the settings reference), which mention behaviour up to Claude Code v2.1.286. No plugin was installed in the course environment. The practice tests read the files you write and run your hook script offline.

## Why it matters

A skill in one repository helps one team. A plugin makes the same set installable everywhere, versioned, with its dependencies. The exam asks what goes into a plugin, how a version range behaves, and how a team gets a plugin without each person installing it by hand. It also asks the careful question: a plugin runs code with your privileges, so who decides to trust it?

## The idea

### What a plugin is

"A plugin is a directory of skills, agents, hooks, and MCP servers, plus a `plugin.json` file, called the manifest, that names the plugin. Claude Code loads the directory as one unit, so you can share it with teammates, install it in several projects, or publish it to a marketplace."

The layout is fixed. The manifest sits in `.claude-plugin/plugin.json`, and "Only `plugin.json` goes inside `.claude-plugin/`." Everything else goes at the plugin root, next to that folder: `skills/<name>/SKILL.md`, `agents/<name>.md`, `hooks/hooks.json` and `.mcp.json`. A plugin may hold any mix of these, and none is required. Only `name` is required in the manifest. Add `version` and `description` so that updates and listings make sense.

To test one without installing, start Claude Code with `--plugin-dir <path>`, which "loads a plugin for one session without installing it". Run `claude plugin validate` on the folder to check the manifest and the skill frontmatter before you run anything.

Two details matter for correctness. A hook script in a plugin cannot assume where the plugin was installed, so the command in `hooks/hooks.json` reaches it through `${CLAUDE_PLUGIN_ROOT}`. And a plugin is more restricted than a project: the agents it ships do not support `permissionMode`, `hooks` or `mcpServers`, and "These fields are ignored when loading agents from a plugin." A plugin author who needs hooks or an MCP server ships them in the plugin's own `hooks/hooks.json` and `.mcp.json`, where they "apply whenever the plugin is enabled rather than only inside the subagent."

### Dependencies

A plugin can depend on another one, such as one whose skill or MCP server it calls. List dependencies in the `dependencies` array of `plugin.json`. An entry is a plugin name as a string, which tracks whatever version the marketplace offers, or an object with a `name` and a `version`, which is a semantic-version range such as `~2.1.0`, `^2.0`, `>=1.4` or `=2.1.0`.

The range exists for a reason the documentation states: "Without a version constraint, a dependency moves to each new release its marketplace publishes the next time users update. If that release renames an MCP tool your plugin calls, your plugin breaks for everyone who updates." With `~2.1.0`, "users who have your plugin installed keep receiving `2.1.x` patches of the dependency and never move to `2.2`." You then upgrade on your own schedule: test against the newer release, change the range and publish a new version of your plugin.

For a dependency from a git-backed source, resolution is by git tag: "The dependency installs at the highest git tag that satisfies this range, so the dependency's maintainer must tag releases." A range does not match pre-release versions such as `2.0.0-beta.1` unless you opt in. By default Claude Code does not install a dependency from a different marketplace than the declaring plugin's own. The root marketplace must list the other one in `allowCrossMarketplaceDependenciesOn`, so one marketplace cannot silently install from a source the user never chose.

A plugin with only a `name` and a `dependencies` array is a valid plugin, and installing it installs everything in the array. Platform teams use that to publish a bundle such as a standard set for backend engineers.

### Sharing across a team

A marketplace is the catalog Claude Code fetches plugins from. To give a repository's contributors a plugin without each of them installing it, set two keys in the repository's `.claude/settings.json`: "Add the marketplace under `extraKnownMarketplaces`, keyed by the marketplace's own `name`" from its `marketplace.json`, and then "add each plugin under `enabledPlugins` as `plugin-name@marketplace-name`." Each marketplace entry carries a `source` object, such as a GitHub repository written as `owner/name`. For a whole organisation, the same two keys go in managed settings.

The safeguard is trust: "The `extraKnownMarketplaces` entries apply only in a folder the contributor has trusted, and in an untrusted folder Claude Code ignores them without a message." A repository cannot install code on a machine that has not trusted it.

### Trust

"A Claude Code plugin you install can execute arbitrary code on your machine with your user privileges." A marketplace's name tells you who publishes the catalog and not what each plugin does. Review a plugin's hooks and scripts before you enable it, prefer a pinned dependency range, and use managed settings to restrict which marketplaces an organisation allows.

### Choosing between a skill folder and a plugin

Keep a skill in `.claude/skills/` while one repository uses it. Make a plugin when more than one project needs it, when it bundles a hook with the skill that depends on it, or when it needs a version and a dependency. The skill then travels with its hook script and its subagent, and one `enabledPlugins` line turns the set on.

### The practice

The practice builds a plugin called `release-kit` and the team settings that enable it. It holds a guard hook script that refuses pushes, forced recursive deletes and piped downloads in every spelling, and blocks edits of protected paths. It also holds two skills, one that Claude may start and one that only a person may start, a read-only subagent with project memory, a manifest with a dependency pinned to patch updates, and the repository settings. Tests run your script as a process on events, and read the other files. The files are language-neutral, so the module has Python and TypeScript test suites and no Java or Kotlin edition: no YAML or JSON library is available offline for those two here, and a second edition would test the same files. The hook script is a Python file in both editions, because a hook is a separate program.

## Traps

1. **Leaving a dependency unpinned.** The next release of the dependency reaches every user of your plugin, and a rename breaks them.
2. **Hard-coding the plugin's install path in a hook.** Use `${CLAUDE_PLUGIN_ROOT}`. The path differs on every machine.
3. **Putting `permissionMode` in a plugin's agent.** The loader ignores it. Ship the guard in `hooks/hooks.json`.
4. **Assuming a repository's `extraKnownMarketplaces` applies before trust.** In an untrusted folder it is ignored without a message.

## Quiz

1. A plugin declares a dependency on `~2.1.0`, and the dependency's maintainer publishes 2.1.4 and then 2.2.0. Which version do the plugin's users receive?
   - **a**: 2.1.4, because the range takes patch updates and not the 2.2 line
   - **b**: 2.2.0, because a range always follows the newest release that exists
   - **c**: 2.1.0, because a range freezes the first version that it matched once
   - **d**: Neither, because ranges are resolved only for plugins that carry no version

2. A team wants every contributor to receive a tool bundle without a manual install. Where does the configuration go?
   - **a**: The repository's own `.claude/settings.json`, naming a marketplace and an enabled entry
   - **b**: Each person's home settings file, copied by hand from a wiki page of the team
   - **c**: The manifest, which lists the people who should install the bundle on their machines
   - **d**: The memory file, which names the bundle in a sentence for Claude to read each time

3. An agent file shipped inside a plugin sets `permissionMode: bypassPermissions` to run quietly. What happens?
   - **a**: The plugin fails to load, because the loader rejects the whole file
   - **b**: The agent skips every prompt, exactly as the value says it should
   - **c**: The value is dropped at load time, because the loader skips such fields
   - **d**: The agent asks once, then remembers the answer for the whole session

<details>
<summary>Answer key</summary>

1. **a**. The page says with `~2.1.0` "users who have your plugin installed keep receiving `2.1.x` patches of the dependency and never move to `2.2`." *b* is ruled out because a pinned range does not follow the newest release: "Without a version constraint, a dependency moves to each new release its marketplace publishes". *c* is ruled out because the dependency "installs at the highest git tag that satisfies this range", which is 2.1.4 and not the first match. *d* is ruled out because "resolution is by git tag" for a git-backed dependency, and the object form with a `version` range is how a dependency is constrained.
2. **a**. The page says to set two keys in the repository's `.claude/settings.json`, so that the marketplace and the enabled plugin reach every contributor. *b* is ruled out because hand copying is what the shared file replaces, and the page gives the repository's file as the way "to give a repository's contributors a plugin without each of them installing it". *c* is ruled out because the manifest is "a `plugin.json` file, called the manifest, that names the plugin", and it lists dependencies and not people. *d* is ruled out because the page says to "set two keys in the repository's `.claude/settings.json`", which is configuration and not a sentence in a memory file.
3. **c**. The page says "These fields are ignored when loading agents from a plugin." *b* is ruled out because the fields are "ignored when loading agents from a plugin", so the agent keeps its normal prompts. *a* is ruled out because "These fields are ignored when loading agents from a plugin", so the file still loads. *d* is ruled out because no remembered approval is described, and the page names the fix: "Ship the guard in `hooks/hooks.json`."

</details>

## Module quiz

This quiz covers all three pages of the module.

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
