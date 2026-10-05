# Practice: rolling Claude Code out to an organisation

A company is about to give three hundred developers Claude Code. The platform team has drafted the policy and the plan, and the draft is wrong in several places: the lock that makes managed permissions the only ones sits in a project file where it does nothing, plugins can come from anywhere, a managed default model is mistaken for a lock, the spend limits do not add up, and the adoption targets count lines and prompts. In this practice you correct the files. There is no program to write and no model is called: the tests read your files. The names of the settings are the documented ones of Claude Code (settings and managed-settings pages, read 2026-10-04); the numbers (an effort cap of at most `high`, a baseline of at least 4 weeks, group limits that add up to the organisation limit) are this course's design values. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the files there; each language folder holds its own copy of the files.

## What to write

The project folder holds three files.

- `managed/managed-settings.json`: the organisation's policy, as it would be delivered from the admin console or placed on each machine.
- `.claude/settings.json`: the shared project file that the team commits.
- `docs/rollout.md`: the plan, with the sections Precedence, Spend limits and Adoption.

Rules the files must follow:

- The managed file makes managed settings the only source of permission rules (`allowManagedPermissionRulesOnly`), turns off bypass mode (`permissions.disableBypassPermissionsMode` set to `disable`) and denies `Read(./.env)`.
- The keys only an organisation can set (`allowManagedPermissionRulesOnly`, `allowManagedHooksOnly`, `allowManagedMcpServersOnly`, `strictKnownMarketplaces`, `disableSideloadFlags`) are never in the project file, because they have no effect there. Only managed hooks run: `allowManagedHooksOnly` is `true` in the managed file.
- `strictKnownMarketplaces` is a non-empty list of sources the company owns (a `github` repository under `example-org/`, or a `url` under `https://plugins.example.com/`), and `disableSideloadFlags` is `true`.
- `allowManagedMcpServersOnly` is `true`; `allowedMcpServers` has at least one entry; each entry has exactly one of `serverName`, `serverCommand` or `serverUrl`, and a name holds letters, numbers, hyphens and underscores only; no server is both allowed and denied.
- `availableModels` is a non-empty list, a `model` default is one of them, and `maxEffortLevel` is `low`, `medium` or `high` (exactly `high` is allowed; `max` sets no cap).
- The Spend limits section says that usage credits are on and has a table with the columns Level, Name and Monthly limit: one `organization` row, `group` rows whose limits add up to the organisation limit or less (exactly equal is allowed), and a `member` row no larger than the smallest group.
- The Adoption section has a line `Baseline: N weeks` with N at least 4, and an `Outcome targets:` list of at least two lines, each an outcome (pull requests, time to merge, review, defects) and none a measure of activity (lines accepted, prompts, suggestions accepted).
- The Precedence section has a table of the five levels, in order: Managed settings, Command line, Project local, Shared project, User. The plan says that server-managed settings cannot target a group yet (`per-group ... not yet supported`).
- No file holds a home path or an address other than `example.com`.

## Why each part is there, and what you should see

1. **A lock that sits in the wrong file is no lock.** *You should see* `allowManagedPermissionRulesOnly` and the bypass switch in the managed file, and the environment file denied.
2. **A key that a project file cannot set must not be written there.** *You should see* the five managed-only keys in the managed file alone.
3. **A plugin is code that runs on a developer's machine.** *You should see* the company's own marketplace allowed, nothing else, and no flag that loads a plugin from a folder.
4. **An allowlist that a user can widen is a suggestion.** *You should see* the managed MCP list as the only one that applies, with valid entries and no server in both lists.
5. **A managed default model is not a lock.** *You should see* the choice locked by `availableModels` and an effort cap that is a cap.
6. **Budgets are checked by adding.** *You should see* group limits that add up to the organisation limit, and a member limit inside every group's.
7. **Enablement is measured by what the work produces.** *You should see* outcome targets against a baseline, with activity reported and never set as a target.
8. **People need to know which setting wins.** *You should see* the five levels in the documented order and the note about groups.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The managed file locks permission rules, turns off bypass mode and denies `Read(./.env)` |
| `e1` | The managed-only keys are absent from the project file and `allowManagedHooksOnly` is `true` in the managed file |
| `e2` | `strictKnownMarketplaces` lists only sources the company owns and `disableSideloadFlags` is `true` |
| `e3` | The managed MCP allowlist is exclusive, well formed and disjoint from the denylist |
| `e4` | `availableModels` is set, the default model is in it and the effort cap is at most `high` (exactly `high` is allowed) |
| `e5` | The group limits add up to the organisation limit or less (exactly equal is allowed) and a member's limit is within the smallest group's |
| `e6` | The baseline is at least 4 weeks (exactly 4 is allowed) and the outcome targets are outcomes |
| `e7` | The precedence table has the documented order, the group note is present and no file holds personal data |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
