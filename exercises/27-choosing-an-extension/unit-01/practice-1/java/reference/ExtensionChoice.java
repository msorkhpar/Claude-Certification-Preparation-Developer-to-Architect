import java.util.List;
import java.util.Map;

/** Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md. */
final class ExtensionChoice {
    private ExtensionChoice() {}

    record Choice(String mechanism, String reason) {}

    private static final List<String> SURFACES = List.of("code", "api");
    private static final List<String> KNOWLEDGE = List.of("none", "convention", "reference", "procedure");
    private static final List<String> CARRIED = List.of("skill", "hook", "subagent", "mcp"); // what a plugin can bundle
    private static final List<String> TIMINGS = List.of("none", "interval", "event", "condition", "background");
    private static final List<String> PRESENCE = List.of("session", "pipeline", "away");
    private static final List<String> PERSONAL = List.of("none", "voice", "display", "keys");
    private static final int LOOP_EXPIRY_DAYS = 7; // a recurring task of a session expires after seven days

    private static boolean flag(Map<String, Object> s, String key) {
        return s.get(key) instanceof Boolean b && b;
    }

    private static String text(Map<String, Object> s, String key, String fallback) {
        return s.get(key) == null ? fallback : String.valueOf(s.get(key));
    }

    /** The mechanism for work that runs without a person, on a rhythm or on an event, or null when the situation has none of these. */
    private static Choice rhythm(Map<String, Object> s) {
        String timing = text(s, "timing", "none");
        String presence = text(s, "presence", "session");
        int days = s.get("lasts_days") instanceof Number n ? n.intValue() : 1;
        if (presence.equals("pipeline")) return new Choice("headless-ci", "no-person-present");
        if (timing.equals("condition")) return new Choice("goal", "until-condition-holds");
        if (timing.equals("background")) return new Choice("background-task", "work-while-it-runs");
        if (timing.equals("event")) return presence.equals("away") ? new Choice("routine", "runs-unattended") : new Choice("monitor", "push-not-poll");
        if (timing.equals("interval")) {
            if (presence.equals("away")) return new Choice("routine", "runs-unattended");
            if (days > LOOP_EXPIRY_DAYS) return flag(s, "local_files") ? new Choice("desktop-task", "durable-and-local") : new Choice("routine", "runs-unattended");
            return new Choice("loop", "session-rhythm");
        }
        return null;
    }

    static Choice choose(Map<String, Object> situation) {
        String surface = text(situation, "surface", "code");
        String knowledge = text(situation, "knowledge", "none");
        int repos = situation.get("repos") instanceof Number n ? n.intValue() : 1;
        if (!SURFACES.contains(surface)) throw new IllegalArgumentException("unknown surface: " + surface);
        if (!KNOWLEDGE.contains(knowledge)) throw new IllegalArgumentException("unknown knowledge kind: " + knowledge);
        if (repos < 1) throw new IllegalArgumentException("repos starts at 1");
        if (!TIMINGS.contains(text(situation, "timing", "none"))) throw new IllegalArgumentException("unknown timing: " + situation.get("timing"));
        if (!PRESENCE.contains(text(situation, "presence", "session"))) throw new IllegalArgumentException("unknown presence: " + situation.get("presence"));
        if (!PERSONAL.contains(text(situation, "personal", "none"))) throw new IllegalArgumentException("unknown personal preference: " + situation.get("personal"));
        if (situation.get("lasts_days") instanceof Number days && days.intValue() < 1) throw new IllegalArgumentException("lasts_days starts at 1");
        if (text(situation, "presence", "session").equals("away") && flag(situation, "local_files")) throw new IllegalArgumentException("a cloud run starts from a fresh clone and sees no local files");
        if (surface.equals("api")) {
            if (flag(situation, "builtin_covers")) return new Choice("builtin-tool", "provided-schema");
            if (flag(situation, "external_system") && flag(situation, "remote_server")) return new Choice("mcp", "remote-server");
            return new Choice("api-tool", "own-schema-and-code");
        }
        Choice choice;
        if (flag(situation, "guarantee")) choice = new Choice("hook", "must-hold-every-time");
        else if (flag(situation, "external_system")) choice = new Choice("mcp", "external-system");
        else if (flag(situation, "noisy")) choice = new Choice("subagent", "isolate-context");
        else if (rhythm(situation) != null) choice = rhythm(situation);
        else if (text(situation, "personal", "none").equals("voice")) choice = new Choice("output-style", "response-voice");
        else if (text(situation, "personal", "none").equals("display")) choice = new Choice("status-line", "personal-display");
        else if (text(situation, "personal", "none").equals("keys")) choice = new Choice("keybinding", "personal-keys");
        else if (knowledge.equals("convention")) choice = flag(situation, "path_scoped") ? new Choice("path-rule", "scoped-convention") : new Choice("claude-md", "always-known");
        else if (knowledge.equals("reference")) choice = new Choice("skill", "on-demand-reference");
        else if (knowledge.equals("procedure")) choice = new Choice("skill", "repeatable-procedure");
        else choice = new Choice("builtin-tool", "built-in-covers");
        if (repos >= 2 && CARRIED.contains(choice.mechanism())) return new Choice("plugin", "shared-setup");
        return choice;
    }
}
