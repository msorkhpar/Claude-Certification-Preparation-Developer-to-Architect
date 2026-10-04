# One setting in five places: precedence, locks and managed-only keys

**Level:** Architect Professional · **Module 92:** Enabling teams and operations · **Page 1 of 2**
**Exams:** P7, P5

**After this page you can** say which value Claude Code uses when the organisation, the team and a developer set the same key, tell a key that merges from a key that is replaced, choose which keys belong only in the organisation's managed file, lock the model choice with a list and not with a default, cap the effort level, and find out why a developer's setting is ignored.

Checked on 2026-10-04 against the Claude Code documentation pages "Settings files and precedence", "Managed settings", "Server-managed settings" and the settings reference, the Claude Certified Architect, Professional exam guide (version 1.0, domain 7), and by running the example offline in the course container. Nothing here started Claude Code or called a model; the layers in the example are invented and its key list is a subset of the documented keys. This page deepens module 57 (memory files and rules) and module 73 (the developer-productivity scenario) from one developer's configuration to the organisation's. The rollout, the spend and the adoption are the second page.

> **Exam guide and current product.** *What the guide states:* domain 7 asks the candidate to "Configure Claude tools and environments for teams (e.g., Claude Code)" and to "Support debugging and operational issue resolution". *What the current product's documentation says (pages read 2026-10-04):* settings are read from five places, in a fixed order of precedence, highest first: managed settings, the command line, project local, shared project and user; a key set at a higher level overrides the same key lower down, and lists such as permission rules combine across files. Managed settings apply "above every other level", with a few security exceptions in which a stricter lower value counts. Some keys are read only from managed settings, and a lower file that sets them has no effect. The documentation can change with each release, and the keys in the example are those of the pages read on the date above; the example's resolver is a teaching model of those rules and not Claude Code's own code.

## Why it matters

A platform team writes a careful policy and commits it to the repository's shared settings file, because every developer has the repository. A week later an audit finds three developers running with the bypass-permissions mode, one plugin from an unknown marketplace and a model nobody approved. Nothing was hacked. The policy was in a place that a developer's own file overrides, or in a place that the key does not read at all. The exam asks where each part of a policy belongs, which rule decides when two files disagree, and how to find out what a machine is really running.

## The idea

### Five places, one order

Claude Code reads settings from five places. In order of precedence, highest first:

1. **Managed settings.** The organisation's policy, delivered as a file on each machine (a managed-settings file in a system folder, or a device-management policy) or fetched from the admin console as server-managed settings.
2. **The command line.** Values for one session.
3. **Project local.** A developer's own file for one project, not committed.
4. **Shared project.** The file in the repository that the team commits.
5. **User.** A developer's settings for every project.

A key set at a higher level overrides the same key set lower down. That sentence is the whole rule for ordinary keys, and the example applies it: the managed file sets `cleanupPeriodDays` to 7, the command line sets 14, and the effective value is 7. The managed level is not a default that a developer may change: no user, project or local value overrides it, with one exception: a managed `model` is only a default, as the lock section below shows.

### Lists merge, except where a lock stops them

A key that holds a list, such as the permission rules, is combined across files and not replaced: the team's `allow` rules, the developer's own and the organisation's all apply, with duplicates removed. The example shows it: with no lock, a project file with one rule and a user file with two give a merged list of the distinct rules. Merging is right for a developer's convenience and wrong for a policy, because a developer can add an allow rule that the organisation did not intend. The lock is the key `allowManagedPermissionRulesOnly`. When the managed file sets it to `true`, only the managed permission rules apply, and the rules in the user, project and local files are ignored. The example records each ignored entry with its reason.

### Keys that only an organisation can set

Some keys are read only from managed settings, because their purpose is to be out of a developer's reach: `allowManagedHooksOnly` (only managed hooks run), `allowManagedMcpServersOnly` (only the managed list of MCP servers applies), `strictKnownMarketplaces` (the sources that plugins may come from; an empty list blocks every source, the official marketplace included) and `disableSideloadFlags` (the command-line flags that load a plugin, an agent or an MCP server for a single run are rejected). A lower file that sets one of these has **no effect**, which is the failure of the opening story: a lock written in the shared project file looks like policy and does nothing. The example ignores such a key from the local file with the note "a managed-only key".

### Locks you build from lists, and security exceptions

Two choices look alike and are not. A managed `model` is a **default**: a developer can still pick another model. The **lock** is `availableModels`, a list, and a managed list applies as it is: a project file's list cannot widen it. The example shows the two results: with a managed list of `sonnet` and `haiku`, a request for `haiku` is allowed and one for `opus` is refused (`not in availableModels`), whoever makes it.

Two keys go the other way, because a stricter value is always welcome. `disableClaudeAiConnectors` set to `true` from any level stands. `maxEffortLevel` caps the effort level, and when several levels set a cap, the lowest applies, so a developer may lower the organisation's cap and nobody can raise it. The example's managed cap is `high`, the project file asks for `xhigh` and the result is `high`; a cap of `xhigh` or `max` in the managed file would be no cap at all for the levels it was meant to hold back.

### Server-managed settings and their limits

When the company has no device management, the same policy can be set in the admin console and fetched by Claude Code at startup and refreshed hourly during a session. Four facts bound it. It is for Teams and Enterprise organisations and is edited by an Owner or Primary Owner, not by any administrator. It applies to everyone in the organisation, and per-group policy is not yet supported there, so a different policy for one group means a different file or profile deployed to that group. If the fetch fails, Claude Code continues without the remote policy and warns, unless `forceRemoteSettingsRefresh` is set, which makes startup fail closed. And a policy fetched hourly is not an instant switch: plan for the interval.

### Finding out what a machine is running

A developer says "my setting is ignored". The answer is a lookup and not a guess. `/status` shows the `Setting sources` line, which names the managed source Claude Code selected, and `claude doctor` lists what it dropped. The example's notes play the same part: every ignored entry names its key, its level and its reason. When a policy "does nothing", the reasons are almost always the same three: it is in a file the key does not read, a higher level sets the same key, or a lock removed the lower entry on purpose.

### The example

The example is the resolver: it takes five layers (managed, command line, local, project and user), applies the order, the managed-only keys, the lists that merge, the locks, the lowest effort cap and the connector rule, and prints the effective settings, a note for every ignored entry, the two model checks and the merged list of a project with no lock. It ran offline in every language.

<!-- example: m92-policy-resolver tabs: python,typescript,java,kotlin -->
<!-- /example -->

### The practice: the policy files

The practice is in [`exercises/92-enabling-teams-and-operations`](../../exercises/92-enabling-teams-and-operations/unit-01/practice-1/statement.md). A draft of the managed file, the shared project file and the rollout plan is wrong in several places: the lock that makes managed permissions the only ones sits in a project file, plugins can come from anywhere, a managed default model is mistaken for a lock, the spend limits do not add up and the adoption targets count lines. You correct the files, and the tests read them. It is graded in Python, TypeScript, Java and Kotlin; the statement lists eight cases, each saying what you should see.

## Traps

1. **"Put the policy in the shared project file; everyone has the repository."** It is tempting because the file is committed and reviewed with the code. The exam rejects it because a developer's own files override it and the managed-only keys are read only from managed settings; a policy belongs in managed settings, and the shared file holds what the team agrees for itself.
2. **"Set the managed `model` so developers cannot choose another."** It is tempting because the setting is named model and sits at the top level. The exam rejects it because a managed model is a default and the developer can still pick another; the lock is the `availableModels` list.
3. **"Add the organisation's permission rules to the managed file and trust the merge."** It is tempting because lists merge and the rules are all there. The exam rejects it because a merge lets the developer's own allow rules join the policy; set `allowManagedPermissionRulesOnly` in the managed file when the rules must be the only ones.

## Quiz

1. Scenario: Dmitri's company wants developers limited to two approved models. The platform team sets `model` to the first of them in the managed file and tells the developers to use only those two. What happens when a developer picks another model?
   - **a**: It is allowed, since a default gets overridden and only a list restricts
   - **b**: The choice is refused, because the managed file outranks the developer's own picks
   - **c**: The pick is refused for the session and restored after the next restart
   - **d**: The pick stands only if the project file also names that model

2. Scenario: Amara adds `allowManagedHooksOnly: true` to the shared project file that her team commits, expecting every developer to be limited to the hooks of the organisation. What is the effect?
   - **a**: Hooks from the project run, since the team's file is read after the user file
   - **b**: Every hook is switched off in the project until the managed file confirms it
   - **c**: Nothing happens, since only the top tier reads that key
   - **d**: Only the hooks of the project run, since the team's choice is the closest

<details>
<summary>Answer key</summary>

1. **a**. A managed `model` is a default, and the lock is the list. *b* is ruled out because "A managed `model` is a **default**: a developer can still pick another model". *c* is ruled out because the rule is "The **lock** is `availableModels`, a list", with no session restore described. *d* is ruled out because "a managed list applies as it is: a project file's list cannot widen it", and nothing here makes a project file's model decide.
2. **c**. A lower file that sets a managed-only key has no effect. *a* is ruled out because "A lower file that sets one of these has **no effect**". *b* is ruled out because the page describes "a lock written in the shared project file looks like policy and does nothing", not a switch that waits for confirmation. *d* is ruled out because "Some keys are read only from managed settings, because their purpose is to be out of a developer's reach".

</details>
