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
