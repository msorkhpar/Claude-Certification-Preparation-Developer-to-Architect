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
```python
"""Which value does a developer's Claude Code actually use when a team, a person and an organisation all set the same key? A small resolver that applies the documented precedence, the keys only an organisation can set, the lists that merge and the locks that stop them merging.

The layers are invented and the key list is a subset of the settings documentation read on 2026-10-04 (Claude Code settings and managed-settings pages). Nothing here starts Claude Code or calls a model.
"""
import logging

log = logging.getLogger(__name__)
LEVELS = ["managed", "command line", "local", "project", "user"]
MANAGED_ONLY = {"allowManagedPermissionRulesOnly", "allowManagedHooksOnly", "allowManagedMcpServersOnly", "strictKnownMarketplaces", "disableSideloadFlags"}
EFFORT = ["low", "medium", "high", "xhigh", "max"]


def effective(layers):
    """The settings Claude Code applies, and a note for every entry that was ignored and why."""
    log.debug("effective input: %r", layers)
    managed = layers.get("managed", {})
    lock_rules = managed.get("allowManagedPermissionRulesOnly") is True
    lock_mcp = managed.get("allowManagedMcpServersOnly") is True
    out, notes = {}, []
    for key in sorted({k for level in layers.values() for k in level}):
        values = [(level, layers[level][key]) for level in LEVELS if level in layers and key in layers[level]]
        reason = None
        if key in MANAGED_ONLY:
            reason = "a managed-only key"
        elif key in ("permissions.allow", "permissions.deny") and lock_rules:
            reason = "managed settings are the only source of permission rules"
        elif key == "allowedMcpServers" and lock_mcp:
            reason = "managed settings are the only source of the MCP allowlist"
        elif key == "availableModels" and any(level == "managed" for level, _ in values):
            reason = "the managed list applies as it is"
        if reason:
            notes += [f"ignored {key} from {level}: {reason}" for level, _ in values if level != "managed"]
            values = [(level, v) for level, v in values if level == "managed"]
        if not values:
            continue
        out[key] = combine(key, [v for _, v in values])
    return out, notes


def combine(key, values):
    """Lists merge without duplicates, the lowest effort cap wins, a connector ban from any level stands, and any other key takes the highest level's value."""
    if isinstance(values[0], list):
        merged = []
        for value in values:
            merged += [item for item in value if item not in merged]
        return merged
    if key == "maxEffortLevel":
        return min(values, key=EFFORT.index)
    if key == "disableClaudeAiConnectors":
        return any(v is True for v in values)
    return values[0]


def allowed_model(requested, settings):
    """A managed list of available models refuses any other choice, whoever makes it."""
    listed = settings.get("availableModels")
    return "allowed" if listed is None or requested in listed else "refused (not in availableModels)"


def show(value):
    if isinstance(value, list):
        return ", ".join(value)
    return "True" if value is True else "False" if value is False else str(value)


def main():
    layers = {
        "managed": {"allowManagedPermissionRulesOnly": True, "permissions.deny": ["Read(./.env)"], "permissions.allow": ["Read(./docs/**)"],
                    "maxEffortLevel": "high", "availableModels": ["sonnet", "haiku"], "cleanupPeriodDays": 7, "spinnerTipsEnabled": True},
        "command line": {"cleanupPeriodDays": 14},
        "local": {"permissions.allow": ["Bash(npm test)"], "allowManagedHooksOnly": False},
        "project": {"permissions.allow": ["Bash(git status)"], "permissions.deny": ["Read(./secrets/**)"], "maxEffortLevel": "xhigh",
                    "availableModels": ["opus"], "disableClaudeAiConnectors": False, "spinnerTipsEnabled": False},
        "user": {"permissions.allow": ["Edit(./notes/**)"], "disableClaudeAiConnectors": True, "spinnerTipsEnabled": False},
    }
    settings, notes = effective(layers)
    print("effective settings:")
    for key, value in settings.items():
        print(f"  {key} = {show(value)}")
    print("notes:")
    for note in notes:
        print(f"  {note}")
    for model in ("opus", "haiku"):
        print(f"model {model}: {allowed_model(model, settings)}")
    open_layers = {"project": {"permissions.allow": ["Bash(git status)"]}, "user": {"permissions.allow": ["Edit(./notes/**)", "Bash(git status)"]}}
    merged, _ = effective(open_layers)
    print(f"without a lock the lists merge: {show(merged['permissions.allow'])}")


if __name__ == "__main__":
    main()
```
```text
effective settings:
  allowManagedPermissionRulesOnly = True
  availableModels = sonnet, haiku
  cleanupPeriodDays = 7
  disableClaudeAiConnectors = True
  maxEffortLevel = high
  permissions.allow = Read(./docs/**)
  permissions.deny = Read(./.env)
  spinnerTipsEnabled = True
notes:
  ignored allowManagedHooksOnly from local: a managed-only key
  ignored availableModels from project: the managed list applies as it is
  ignored permissions.allow from local: managed settings are the only source of permission rules
  ignored permissions.allow from project: managed settings are the only source of permission rules
  ignored permissions.allow from user: managed settings are the only source of permission rules
  ignored permissions.deny from project: managed settings are the only source of permission rules
model opus: refused (not in availableModels)
model haiku: allowed
without a lock the lists merge: Bash(git status), Edit(./notes/**)
```
```typescript
import { logger } from "./logger.ts";
const log = logger("policy_resolver");
/**
 * Which value does a developer's Claude Code actually use when a team, a person and an organisation all set the same key? A small resolver that applies the documented precedence, the keys only an organisation can set, the lists that merge and the locks that stop them merging.
 *
 * The layers are invented and the key list is a subset of the settings documentation read on 2026-10-04 (Claude Code settings and managed-settings pages). Nothing here starts Claude Code or calls a model.
 */
export type Value = boolean | number | string | string[];
export type Layers = Record<string, Record<string, Value>>;

export const LEVELS = ["managed", "command line", "local", "project", "user"];
const MANAGED_ONLY = new Set(["allowManagedPermissionRulesOnly", "allowManagedHooksOnly", "allowManagedMcpServersOnly", "strictKnownMarketplaces", "disableSideloadFlags"]);
const EFFORT = ["low", "medium", "high", "xhigh", "max"];

/** The settings Claude Code applies, and a note for every entry that was ignored and why. */
export function effective(layers: Layers): [Record<string, Value>, string[]] {
  log.debug("effective input", layers);
  const managed = layers["managed"] ?? {};
  const lockRules = managed["allowManagedPermissionRulesOnly"] === true;
  const lockMcp = managed["allowManagedMcpServersOnly"] === true;
  const out: Record<string, Value> = {};
  const notes: string[] = [];
  const keys = [...new Set(Object.values(layers).flatMap((level) => Object.keys(level)))].sort();
  for (const key of keys) {
    let values: [string, Value][] = LEVELS.filter((level) => level in layers && key in layers[level]).map((level) => [level, layers[level][key]]);
    let reason: string | null = null;
    if (MANAGED_ONLY.has(key)) reason = "a managed-only key";
    else if ((key === "permissions.allow" || key === "permissions.deny") && lockRules) reason = "managed settings are the only source of permission rules";
    else if (key === "allowedMcpServers" && lockMcp) reason = "managed settings are the only source of the MCP allowlist";
    else if (key === "availableModels" && values.some(([level]) => level === "managed")) reason = "the managed list applies as it is";
    if (reason) {
      for (const [level] of values) if (level !== "managed") notes.push(`ignored ${key} from ${level}: ${reason}`);
      values = values.filter(([level]) => level === "managed");
    }
    if (values.length === 0) continue;
    out[key] = combine(key, values.map(([, v]) => v));
  }
  return [out, notes];
}

/** Lists merge without duplicates, the lowest effort cap wins, a connector ban from any level stands, and any other key takes the highest level's value. */
export function combine(key: string, values: Value[]): Value {
  if (Array.isArray(values[0])) {
    const merged: string[] = [];
    for (const value of values as string[][]) for (const item of value) if (!merged.includes(item)) merged.push(item);
    return merged;
  }
  if (key === "maxEffortLevel") return (values as string[]).reduce((a, b) => (EFFORT.indexOf(b) < EFFORT.indexOf(a) ? b : a));
  if (key === "disableClaudeAiConnectors") return values.some((v) => v === true);
  return values[0];
}

/** A managed list of available models refuses any other choice, whoever makes it. */
export function allowedModel(requested: string, settings: Record<string, Value>): string {
  const listed = settings["availableModels"] as string[] | undefined;
  return listed === undefined || listed.includes(requested) ? "allowed" : "refused (not in availableModels)";
}

function show(value: Value): string {
  if (Array.isArray(value)) return value.join(", ");
  return value === true ? "True" : value === false ? "False" : String(value);
}

function main(): void {
  const layers: Layers = {
    managed: { allowManagedPermissionRulesOnly: true, "permissions.deny": ["Read(./.env)"], "permissions.allow": ["Read(./docs/**)"], maxEffortLevel: "high", availableModels: ["sonnet", "haiku"], cleanupPeriodDays: 7, spinnerTipsEnabled: true },
    "command line": { cleanupPeriodDays: 14 },
    local: { "permissions.allow": ["Bash(npm test)"], allowManagedHooksOnly: false },
    project: { "permissions.allow": ["Bash(git status)"], "permissions.deny": ["Read(./secrets/**)"], maxEffortLevel: "xhigh", availableModels: ["opus"], disableClaudeAiConnectors: false, spinnerTipsEnabled: false },
    user: { "permissions.allow": ["Edit(./notes/**)"], disableClaudeAiConnectors: true, spinnerTipsEnabled: false },
  };
  const [settings, notes] = effective(layers);
  console.log("effective settings:");
  for (const [key, value] of Object.entries(settings)) console.log(`  ${key} = ${show(value)}`);
  console.log("notes:");
  for (const note of notes) console.log(`  ${note}`);
  for (const model of ["opus", "haiku"]) console.log(`model ${model}: ${allowedModel(model, settings)}`);
  const open: Layers = { project: { "permissions.allow": ["Bash(git status)"] }, user: { "permissions.allow": ["Edit(./notes/**)", "Bash(git status)"] } };
  const [merged] = effective(open);
  console.log(`without a lock the lists merge: ${show(merged["permissions.allow"])}`);
}

if (import.meta.main) main();
```
```text
effective settings:
  allowManagedPermissionRulesOnly = True
  availableModels = sonnet, haiku
  cleanupPeriodDays = 7
  disableClaudeAiConnectors = True
  maxEffortLevel = high
  permissions.allow = Read(./docs/**)
  permissions.deny = Read(./.env)
  spinnerTipsEnabled = True
notes:
  ignored allowManagedHooksOnly from local: a managed-only key
  ignored availableModels from project: the managed list applies as it is
  ignored permissions.allow from local: managed settings are the only source of permission rules
  ignored permissions.allow from project: managed settings are the only source of permission rules
  ignored permissions.allow from user: managed settings are the only source of permission rules
  ignored permissions.deny from project: managed settings are the only source of permission rules
model opus: refused (not in availableModels)
model haiku: allowed
without a lock the lists merge: Bash(git status), Edit(./notes/**)
```
```java
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Which value does a developer's Claude Code actually use when a team, a person and an organisation all set the same key? A small resolver that applies the documented precedence, the keys only an organisation can set, the lists that merge and the locks that stop them merging.
 *
 * The layers are invented and the key list is a subset of the settings documentation read on 2026-10-04 (Claude Code settings and managed-settings pages). Nothing here starts Claude Code or calls a model.
 */
public class PolicyResolver {
    private static final System.Logger LOG = System.getLogger(PolicyResolver.class.getName());
    record Result(Map<String, Object> settings, List<String> notes) {}

    record Entry(String level, Object value) {}

    static final List<String> LEVELS = List.of("managed", "command line", "local", "project", "user");
    static final Set<String> MANAGED_ONLY = Set.of("allowManagedPermissionRulesOnly", "allowManagedHooksOnly", "allowManagedMcpServersOnly", "strictKnownMarketplaces", "disableSideloadFlags");
    static final List<String> EFFORT = List.of("low", "medium", "high", "xhigh", "max");

    /** The settings Claude Code applies, and a note for every entry that was ignored and why. */
    static Result effective(Map<String, Map<String, Object>> layers) {
        LOG.log(System.Logger.Level.DEBUG, "effective input: {0}", layers);
        Map<String, Object> managed = layers.getOrDefault("managed", Map.of());
        boolean lockRules = Boolean.TRUE.equals(managed.get("allowManagedPermissionRulesOnly"));
        boolean lockMcp = Boolean.TRUE.equals(managed.get("allowManagedMcpServersOnly"));
        Map<String, Object> out = new LinkedHashMap<>();
        List<String> notes = new ArrayList<>();
        TreeSet<String> keys = new TreeSet<>();
        for (Map<String, Object> level : layers.values()) keys.addAll(level.keySet());
        for (String key : keys) {
            List<Entry> values = new ArrayList<>();
            for (String level : LEVELS) if (layers.containsKey(level) && layers.get(level).containsKey(key)) values.add(new Entry(level, layers.get(level).get(key)));
            String reason = null;
            if (MANAGED_ONLY.contains(key)) reason = "a managed-only key";
            else if ((key.equals("permissions.allow") || key.equals("permissions.deny")) && lockRules) reason = "managed settings are the only source of permission rules";
            else if (key.equals("allowedMcpServers") && lockMcp) reason = "managed settings are the only source of the MCP allowlist";
            else if (key.equals("availableModels") && values.stream().anyMatch(e -> e.level().equals("managed"))) reason = "the managed list applies as it is";
            if (reason != null) {
                for (Entry e : values) if (!e.level().equals("managed")) notes.add("ignored " + key + " from " + e.level() + ": " + reason);
                values = new ArrayList<>(values.stream().filter(e -> e.level().equals("managed")).toList());
            }
            if (values.isEmpty()) continue;
            out.put(key, combine(key, values.stream().map(Entry::value).toList()));
        }
        return new Result(out, notes);
    }

    /** Lists merge without duplicates, the lowest effort cap wins, a connector ban from any level stands, and any other key takes the highest level's value. */
    @SuppressWarnings("unchecked")
    static Object combine(String key, List<Object> values) {
        if (values.get(0) instanceof List) {
            List<String> merged = new ArrayList<>();
            for (Object value : values) for (String item : (List<String>) value) if (!merged.contains(item)) merged.add(item);
            return merged;
        }
        if (key.equals("maxEffortLevel")) {
            String lowest = (String) values.get(0);
            for (Object v : values) if (EFFORT.indexOf((String) v) < EFFORT.indexOf(lowest)) lowest = (String) v;
            return lowest;
        }
        if (key.equals("disableClaudeAiConnectors")) return values.stream().anyMatch(v -> Boolean.TRUE.equals(v));
        return values.get(0);
    }

    /** A managed list of available models refuses any other choice, whoever makes it. */
    @SuppressWarnings("unchecked")
    static String allowedModel(String requested, Map<String, Object> settings) {
        List<String> listed = (List<String>) settings.get("availableModels");
        return listed == null || listed.contains(requested) ? "allowed" : "refused (not in availableModels)";
    }

    @SuppressWarnings("unchecked")
    static String show(Object value) {
        if (value instanceof List) return String.join(", ", (List<String>) value);
        if (value instanceof Boolean b) return b ? "True" : "False";
        return String.valueOf(value);
    }

    public static void main(String[] args) {
        Map<String, Map<String, Object>> layers = new LinkedHashMap<>();
        layers.put("managed", new LinkedHashMap<>(Map.of("allowManagedPermissionRulesOnly", true, "permissions.deny", List.of("Read(./.env)"), "permissions.allow", List.of("Read(./docs/**)"),
            "maxEffortLevel", "high", "availableModels", List.of("sonnet", "haiku"), "cleanupPeriodDays", 7, "spinnerTipsEnabled", true)));
        layers.put("command line", Map.of("cleanupPeriodDays", 14));
        layers.put("local", Map.of("permissions.allow", List.of("Bash(npm test)"), "allowManagedHooksOnly", false));
        layers.put("project", Map.of("permissions.allow", List.of("Bash(git status)"), "permissions.deny", List.of("Read(./secrets/**)"), "maxEffortLevel", "xhigh",
            "availableModels", List.of("opus"), "disableClaudeAiConnectors", false, "spinnerTipsEnabled", false));
        layers.put("user", Map.of("permissions.allow", List.of("Edit(./notes/**)"), "disableClaudeAiConnectors", true, "spinnerTipsEnabled", false));
        Result result = effective(layers);
        System.out.println("effective settings:");
        for (Map.Entry<String, Object> e : result.settings().entrySet()) System.out.println("  " + e.getKey() + " = " + show(e.getValue()));
        System.out.println("notes:");
        for (String note : result.notes()) System.out.println("  " + note);
        for (String model : List.of("opus", "haiku")) System.out.println("model " + model + ": " + allowedModel(model, result.settings()));
        Map<String, Map<String, Object>> open = new LinkedHashMap<>();
        open.put("project", Map.of("permissions.allow", List.of("Bash(git status)")));
        open.put("user", Map.of("permissions.allow", List.of("Edit(./notes/**)", "Bash(git status)")));
        System.out.println("without a lock the lists merge: " + show(effective(open).settings().get("permissions.allow")));
    }
}
```
```text
effective settings:
  allowManagedPermissionRulesOnly = True
  availableModels = sonnet, haiku
  cleanupPeriodDays = 7
  disableClaudeAiConnectors = True
  maxEffortLevel = high
  permissions.allow = Read(./docs/**)
  permissions.deny = Read(./.env)
  spinnerTipsEnabled = True
notes:
  ignored allowManagedHooksOnly from local: a managed-only key
  ignored availableModels from project: the managed list applies as it is
  ignored permissions.allow from local: managed settings are the only source of permission rules
  ignored permissions.allow from project: managed settings are the only source of permission rules
  ignored permissions.allow from user: managed settings are the only source of permission rules
  ignored permissions.deny from project: managed settings are the only source of permission rules
model opus: refused (not in availableModels)
model haiku: allowed
without a lock the lists merge: Bash(git status), Edit(./notes/**)
```
```kotlin
/**
 * Which value does a developer's Claude Code actually use when a team, a person and an organisation all set the same key? A small resolver that applies the documented precedence, the keys only an organisation can set, the lists that merge and the locks that stop them merging.
 *
 * The layers are invented and the key list is a subset of the settings documentation read on 2026-10-04 (Claude Code settings and managed-settings pages). Nothing here starts Claude Code or calls a model.
 */

private val log = System.getLogger("policy_resolver")

data class Result(val settings: Map<String, Any>, val notes: List<String>)

val LEVELS = listOf("managed", "command line", "local", "project", "user")
val MANAGED_ONLY = setOf("allowManagedPermissionRulesOnly", "allowManagedHooksOnly", "allowManagedMcpServersOnly", "strictKnownMarketplaces", "disableSideloadFlags")
val EFFORT = listOf("low", "medium", "high", "xhigh", "max")

/** The settings Claude Code applies, and a note for every entry that was ignored and why. */
fun effective(layers: Map<String, Map<String, Any>>): Result {
    log.log(System.Logger.Level.DEBUG, "effective input: {0}", layers)
    val managed = layers["managed"] ?: mapOf()
    val lockRules = managed["allowManagedPermissionRulesOnly"] == true
    val lockMcp = managed["allowManagedMcpServersOnly"] == true
    val out = linkedMapOf<String, Any>()
    val notes = mutableListOf<String>()
    for (key in layers.values.flatMap { it.keys }.toSortedSet()) {
        var values = LEVELS.filter { layers[it]?.containsKey(key) == true }.map { it to layers.getValue(it).getValue(key) }
        val reason = when {
            key in MANAGED_ONLY -> "a managed-only key"
            (key == "permissions.allow" || key == "permissions.deny") && lockRules -> "managed settings are the only source of permission rules"
            key == "allowedMcpServers" && lockMcp -> "managed settings are the only source of the MCP allowlist"
            key == "availableModels" && values.any { it.first == "managed" } -> "the managed list applies as it is"
            else -> null
        }
        if (reason != null) {
            for ((level, _) in values) if (level != "managed") notes.add("ignored $key from $level: $reason")
            values = values.filter { it.first == "managed" }
        }
        if (values.isEmpty()) continue
        out[key] = combine(key, values.map { it.second })
    }
    return Result(out, notes)
}

/** Lists merge without duplicates, the lowest effort cap wins, a connector ban from any level stands, and any other key takes the highest level's value. */
@Suppress("UNCHECKED_CAST")
fun combine(key: String, values: List<Any>): Any {
    if (values[0] is List<*>) {
        val merged = mutableListOf<String>()
        for (value in values) for (item in value as List<String>) if (item !in merged) merged.add(item)
        return merged
    }
    if (key == "maxEffortLevel") return (values as List<String>).minByOrNull { EFFORT.indexOf(it) }!!
    if (key == "disableClaudeAiConnectors") return values.any { it == true }
    return values[0]
}

/** A managed list of available models refuses any other choice, whoever makes it. */
@Suppress("UNCHECKED_CAST")
fun allowedModel(requested: String, settings: Map<String, Any>): String {
    val listed = settings["availableModels"] as List<String>?
    return if (listed == null || requested in listed) "allowed" else "refused (not in availableModels)"
}

@Suppress("UNCHECKED_CAST")
fun show(value: Any?): String = when (value) {
    is List<*> -> (value as List<String>).joinToString(", ")
    true -> "True"
    false -> "False"
    else -> value.toString()
}

fun main() {
    val layers = linkedMapOf<String, Map<String, Any>>(
        "managed" to linkedMapOf("allowManagedPermissionRulesOnly" to true, "permissions.deny" to listOf("Read(./.env)"), "permissions.allow" to listOf("Read(./docs/**)"),
            "maxEffortLevel" to "high", "availableModels" to listOf("sonnet", "haiku"), "cleanupPeriodDays" to 7, "spinnerTipsEnabled" to true),
        "command line" to mapOf("cleanupPeriodDays" to 14),
        "local" to mapOf("permissions.allow" to listOf("Bash(npm test)"), "allowManagedHooksOnly" to false),
        "project" to mapOf("permissions.allow" to listOf("Bash(git status)"), "permissions.deny" to listOf("Read(./secrets/**)"), "maxEffortLevel" to "xhigh",
            "availableModels" to listOf("opus"), "disableClaudeAiConnectors" to false, "spinnerTipsEnabled" to false),
        "user" to mapOf("permissions.allow" to listOf("Edit(./notes/**)"), "disableClaudeAiConnectors" to true, "spinnerTipsEnabled" to false),
    )
    val result = effective(layers)
    println("effective settings:")
    for ((key, value) in result.settings) println("  $key = ${show(value)}")
    println("notes:")
    for (note in result.notes) println("  $note")
    for (model in listOf("opus", "haiku")) println("model $model: ${allowedModel(model, result.settings)}")
    val open = linkedMapOf<String, Map<String, Any>>("project" to mapOf("permissions.allow" to listOf("Bash(git status)")), "user" to mapOf("permissions.allow" to listOf("Edit(./notes/**)", "Bash(git status)")))
    println("without a lock the lists merge: ${show(effective(open).settings["permissions.allow"])}")
}
```
```text
effective settings:
  allowManagedPermissionRulesOnly = True
  availableModels = sonnet, haiku
  cleanupPeriodDays = 7
  disableClaudeAiConnectors = True
  maxEffortLevel = high
  permissions.allow = Read(./docs/**)
  permissions.deny = Read(./.env)
  spinnerTipsEnabled = True
notes:
  ignored allowManagedHooksOnly from local: a managed-only key
  ignored availableModels from project: the managed list applies as it is
  ignored permissions.allow from local: managed settings are the only source of permission rules
  ignored permissions.allow from project: managed settings are the only source of permission rules
  ignored permissions.allow from user: managed settings are the only source of permission rules
  ignored permissions.deny from project: managed settings are the only source of permission rules
model opus: refused (not in availableModels)
model haiku: allowed
without a lock the lists merge: Bash(git status), Edit(./notes/**)
```
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
