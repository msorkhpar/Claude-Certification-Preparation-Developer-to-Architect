# Rolling out: plugins, servers, spend and adoption

**Level:** Architect Professional · **Module 92:** Enabling teams and operations · **Page 2 of 2**
**Exams:** P7, P6

**After this page you can** restrict where plugins and MCP servers may come from, give an organisation spend limits that add up at three levels, choose adoption measures that show a result and not activity, set a baseline before a rollout, and write the plan that a platform team hands to a hundred developers.

Checked on 2026-10-04 against the Claude Code documentation pages "Plugins for your organization", "Managed MCP configuration", "Manage costs effectively", "Analytics", "Server-managed settings" and "Claude Code admin setup", the Claude Certified Architect, Professional exam guide (version 1.0, domain 7), and by running the practice offline in the course container. Nothing here started Claude Code or called a model, and the names, limits and counts are invented. This page deepens module 58 (commands and skills) and module 60 (Claude Code in CI) from what one team installs to what an organisation allows. The precedence and the locks are the first page.

> **Exam guide and current product.** *What the guide states:* domain 7 asks the candidate to "Configure Claude tools and environments for teams (e.g., Claude Code)" and to "Improve developer workflows using AI-assisted tooling". *What the current product's documentation says (pages read 2026-10-04):* the administrator controls which marketplace sources plugins may come from, rejects the command-line flags that sideload plugins, agents and MCP servers, and can restrict MCP servers to a managed allowlist. On Teams and Enterprise plans, each member's use draws from a seat allowance; turning on usage credits lets members continue past it, with spend limits at the organisation, group or member level; the analytics dashboard shows adoption (daily active users and sessions) and, with the GitHub app installed, contribution metrics such as merged pull requests with Claude Code assistance. The dashboard counts lines accepted as a usage metric. What a team should use as a target is not stated there: that choice, the baseline of four weeks and the way the limits add up are this course's design values, taught as the exam's strategy.

## Why it matters

A platform team enables Claude Code for three hundred developers on a Monday. By Friday the chart shows "lines accepted" climbing, the team reports success, and finance asks why three groups are past their budget while the organisation total is under. The team had no baseline, so it cannot say whether pull requests merged faster. It had no check that the group limits added up to the organisation limit, so the limits were never consistent. And a plugin from a public marketplace has been installed by forty people. The exam asks what a rollout fixes before the first developer starts: where things may come from, what each level may spend, and what counts as a result.

## The idea

### Where plugins and servers may come from

A plugin bundles skills, agents, hooks and MCP servers, so a plugin source is a code source. The managed key `strictKnownMarketplaces` is an allowlist of the sources plugins may come from. The practice's policy lists one: the company's own marketplace repository. Three details matter. An empty list blocks every source, the official one included, so "allow nothing" and "allow ours" are different lists. The list takes only the sources the company owns, and a marketplace of another organisation does not belong on it. And the allowlist does not stop a developer from loading a plugin from a folder for one run with a command-line flag, which is why the policy also sets `disableSideloadFlags`: it rejects the flags that sideload plugins, agents and MCP servers.

MCP servers follow the same shape. With `allowManagedMcpServersOnly` on, only the managed `allowedMcpServers` list applies and a developer's own list cannot widen it. Each entry has exactly one of `serverName`, `serverCommand` or `serverUrl`: an entry with two keys is invalid, and a name with a space is not a name. A denied server wins over an allowed one, so the same server never appears in both lists; the practice tests that no server is allowed and denied at once. The project file then holds what the team agrees for itself: its allow rules and the plugin it enables from the company's marketplace.

### Spend limits that add up

On Teams and Enterprise plans, a member's use draws from a seat allowance. **Usage credits** let a member continue past it, and spend limits can be set at three levels: the organisation, a group and a member. The plan should state that credits are on, since otherwise a member who reaches the allowance stops. Two arithmetic rules keep three levels consistent. **The group limits add up to the organisation limit and no more**: three groups with 6000, 8000 and 6000 against an organisation limit of 20000 are exact, and a group limit one unit higher would let the groups together spend more than the organisation allows, so the organisation limit would decide before the group limits did and nobody would know which one they were under. And **a member's limit is no higher than the smallest group's**: a member limit above a group's own makes the member limit a number that can never be reached. Both are checked as a boundary: a total of exactly 20000 passes and 20001 does not.

### Adoption is a result

The dashboard shows two kinds of numbers. Usage metrics (lines accepted, suggestions accepted, daily active users, sessions) measure **activity**. Contribution metrics (merged pull requests with assistance) and the measures a team already has (time to merge, review turnaround, escaped defects) measure **outcomes**. A target on an activity invites the activity: lines accepted can be raised by accepting more lines. A target on an outcome needs a baseline, so the plan takes **four weeks of data before the rollout**, enough to include a release cycle and a quiet week, and states two outcome targets: the share of merged pull requests with assistance, and the time to merge. Lines and suggestions accepted are reported and not targeted. Contribution metrics need the GitHub app and are not available for organisations with zero data retention enabled, so check that before promising them.

### The plan a platform team hands over

The practice's `rollout.md` has three parts, each testable: the precedence table in the documented order, with the note that server-managed settings cannot target a group yet; the spend limits at three levels, adding up; and the adoption plan with its baseline and its two outcome targets. A plan does not carry a name, an address or a home path: it is read by many people and committed to a repository.

### The example

The example is the resolver of the first page, and the part that belongs here is the model lock and the list merge: the managed list refuses a model outside it, and without the lock a project's rule and a user's rules merge into one list. It ran offline in every language.

<!-- example: m92-policy-resolver tabs: python,typescript,java,kotlin -->
```python
"""Which value does a developer's Claude Code actually use when a team, a person and an organisation all set the same key? A small resolver that applies the documented precedence, the keys only an organisation can set, the lists that merge and the locks that stop them merging.

The layers are invented and the key list is a subset of the settings documentation read on 2026-10-04 (Claude Code settings and managed-settings pages). Nothing here starts Claude Code or calls a model.
"""
LEVELS = ["managed", "command line", "local", "project", "user"]
MANAGED_ONLY = {"allowManagedPermissionRulesOnly", "allowManagedHooksOnly", "allowManagedMcpServersOnly", "strictKnownMarketplaces", "disableSideloadFlags"}
EFFORT = ["low", "medium", "high", "xhigh", "max"]


def effective(layers):
    """The settings Claude Code applies, and a note for every entry that was ignored and why."""
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
    record Result(Map<String, Object> settings, List<String> notes) {}

    record Entry(String level, Object value) {}

    static final List<String> LEVELS = List.of("managed", "command line", "local", "project", "user");
    static final Set<String> MANAGED_ONLY = Set.of("allowManagedPermissionRulesOnly", "allowManagedHooksOnly", "allowManagedMcpServersOnly", "strictKnownMarketplaces", "disableSideloadFlags");
    static final List<String> EFFORT = List.of("low", "medium", "high", "xhigh", "max");

    /** The settings Claude Code applies, and a note for every entry that was ignored and why. */
    static Result effective(Map<String, Map<String, Object>> layers) {
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
data class Result(val settings: Map<String, Any>, val notes: List<String>)

val LEVELS = listOf("managed", "command line", "local", "project", "user")
val MANAGED_ONLY = setOf("allowManagedPermissionRulesOnly", "allowManagedHooksOnly", "allowManagedMcpServersOnly", "strictKnownMarketplaces", "disableSideloadFlags")
val EFFORT = listOf("low", "medium", "high", "xhigh", "max")

/** The settings Claude Code applies, and a note for every entry that was ignored and why. */
fun effective(layers: Map<String, Map<String, Any>>): Result {
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

The practice is in [`exercises/92-enabling-teams-and-operations`](../../exercises/92-enabling-teams-and-operations/unit-01/practice-1/statement.md), the same one as the first page. The parts that belong here are the plugin and MCP allowlists, the three-level spend limits, the adoption targets with their baseline and the plan's precedence table.

## Traps

1. **"Allow all plugins from the official marketplace and block the rest; the official one is safe."** It is tempting because the official source feels trustworthy. The exam rejects it because the allowlist names the sources the company has decided to trust, and an empty list blocks the official marketplace too; list the company's own, and add the flag that stops sideloading.
2. **"Give each group a limit that suits it; the organisation limit will catch any overspend."** It is tempting because the organisation limit is a safety net. The exam rejects it because group limits that sum to more than the organisation limit make the group numbers meaningless, since the organisation limit decides first; make the groups add up exactly.
3. **"Track lines accepted as the success measure; it moves every week."** It is tempting because it is on the dashboard and it grows. The exam rejects it because it measures activity, which can be raised without any gain; take a baseline of four weeks and target an outcome such as the time to merge.

## Quiz

1. Scenario: Yusuf's organisation limit is 20000 a month and he has three groups. He proposes limits of 7000, 8000 and 6000 for them. What does the page say about that proposal?
   - **a**: It is sound, because the organisation limit stops any overspend by the groups
   - **b**: It breaks the arithmetic, since the parts could together exceed the whole
   - **c**: It is sound, because unused budget of one group is lent to the others
   - **d**: It is inconsistent, since each group must have the same limit as the others

2. Scenario: A platform team wants to show leadership that Claude Code is working. It sets a target of more accepted lines per developer each month and starts the rollout on Monday. What is the main weakness of the plan?
   - **a**: Accepted lines are not shown on the dashboard and cannot be tracked
   - **b**: Developers accept fewer lines as they learn, so the figure falls over time
   - **c**: A monthly target is too coarse to reflect how developers actually work
   - **d**: Activity rises without any gain, so the figure proves nothing

<details>
<summary>Answer key</summary>

1. **b**. Seven thousand, eight thousand and six thousand add up to 21000, which is more than 20000. *a* is ruled out because "so the organisation limit would decide before the group limits did and nobody would know which one they were under". *c* is ruled out because the page requires that "The group limits add up to the organisation limit and no more", and says nothing about lending. *d* is ruled out because the page's own example has "three groups with 6000, 8000 and 6000 against an organisation limit of 20000", so equal limits are not required.
2. **d**. Lines accepted measure activity. *a* is ruled out because "The dashboard counts lines accepted as a usage metric". *c* is ruled out because the page's objection is "A target on an activity invites the activity", not the length of the period. *b* is ruled out because the page gives no such trend and rests on the point that "lines accepted can be raised by accepting more lines".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: A developer reports that the managed policy "does nothing" for the allowlist of MCP servers on her laptop. The organisation's allowlist, and the key that makes it the only list, are both in the shared project file of the repository. Which fact explains it?
   - **a**: Lists in settings files merge, so her own list has been added to the policy
   - **b**: The command line always outranks the managed file, so the list is overridden
   - **c**: Settings of that kind are honoured at the top tier alone and inert below it
   - **d**: The shared project file is read first, so the policy file replaced it

2. Scenario: A security lead asks for a way to refuse the options that bring in a plugin, an agent or an MCP server for one run only. Which key does that, and where is it set?
   - **a**: `strictKnownMarketplaces`, in the shared project file
   - **b**: `disableSideloadFlags`, in managed settings
   - **c**: `allowManagedHooksOnly`, in the user file
   - **d**: `availableModels`, in managed settings

3. Scenario: Before starting a rollout, a team records four weeks of the time to merge for pull requests. Why four weeks, and what does the record allow later?
   - **a**: Because usage credits reset every four weeks and the data must match
   - **b**: Because the dashboard keeps only four weeks, so older data is lost
   - **c**: To show leadership how many lines developers write without the tool
   - **d**: Because a release cycle fits inside it, so any shift is judged against it

<details>
<summary>Answer key</summary>

1. **c**. A key that only managed settings read has no effect elsewhere. *b* is ruled out because "no user, project or local value overrides it", and the managed level is not a default. *a* is ruled out because the page says managed-only keys are not merged: "A lower file that sets one of these has **no effect**". *d* is ruled out because "Some keys are read only from managed settings, because their purpose is to be out of a developer's reach".
2. **b**. That key rejects the flags that sideload plugins, agents and MCP servers. *a* is ruled out because "An empty list blocks every source, the official one included", and the allowlist does not stop sideloading. *c* is ruled out because "only managed hooks run" is what that key does. *d* is ruled out because "The **lock** is `availableModels`, a list" and concerns the model choice.
3. **d**. A baseline with a release cycle in it lets a later change be measured. *b* is ruled out because the page says "enough to include a release cycle and a quiet week", not a limit of the dashboard. *c* is ruled out because "Lines and suggestions accepted are reported and not targeted". *a* is ruled out because "A target on an outcome needs a baseline", and spend credits play no part in it.

</details>
