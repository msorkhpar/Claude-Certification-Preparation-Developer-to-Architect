import java.util.List;
import java.util.Map;

/** Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md. */
final class ExtensionChoice {
    private ExtensionChoice() {}

    record Choice(String mechanism, String reason) {}

    private static final List<String> SURFACES = List.of("code", "api");
    private static final List<String> KNOWLEDGE = List.of("none", "convention", "reference", "procedure");
    private static final List<String> CARRIED = List.of("skill", "hook", "subagent", "mcp"); // what a plugin can bundle

    private static boolean flag(Map<String, Object> s, String key) {
        return s.get(key) instanceof Boolean b && b;
    }

    private static String text(Map<String, Object> s, String key, String fallback) {
        return s.get(key) == null ? fallback : String.valueOf(s.get(key));
    }

    static Choice choose(Map<String, Object> situation) {
        String surface = text(situation, "surface", "code");
        String knowledge = text(situation, "knowledge", "none");
        int repos = situation.get("repos") instanceof Number n ? n.intValue() : 1;
        if (!SURFACES.contains(surface)) throw new IllegalArgumentException("unknown surface: " + surface);
        if (!KNOWLEDGE.contains(knowledge)) throw new IllegalArgumentException("unknown knowledge kind: " + knowledge);
        if (repos < 1) throw new IllegalArgumentException("repos starts at 1");
        if (surface.equals("api")) {
            if (flag(situation, "builtin_covers")) return new Choice("builtin-tool", "provided-schema");
            if (flag(situation, "external_system") && flag(situation, "remote_server")) return new Choice("mcp", "remote-server");
            return new Choice("api-tool", "own-schema-and-code");
        }
        Choice choice;
        if (flag(situation, "guarantee")) choice = new Choice("hook", "must-hold-every-time");
        else if (flag(situation, "external_system")) choice = new Choice("mcp", "external-system");
        else if (flag(situation, "noisy")) choice = new Choice("subagent", "isolate-context");
        else if (knowledge.equals("convention")) choice = flag(situation, "path_scoped") ? new Choice("path-rule", "scoped-convention") : new Choice("claude-md", "always-known");
        else if (knowledge.equals("reference")) choice = new Choice("skill", "on-demand-reference");
        else if (knowledge.equals("procedure")) choice = new Choice("skill", "repeatable-procedure");
        else choice = new Choice("builtin-tool", "built-in-covers");
        if (repos >= 2 && CARRIED.contains(choice.mechanism())) return new Choice("plugin", "shared-setup");
        return choice;
    }
}
