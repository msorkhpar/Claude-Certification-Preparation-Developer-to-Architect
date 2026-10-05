import java.util.List;
import java.util.Map;

/** Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md. */
final class ExtensionChoice {
    private static final System.Logger LOG = System.getLogger(ExtensionChoice.class.getName());
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

    private static int number(Map<String, Object> s, String key) {
        return s.get(key) instanceof Number n ? n.intValue() : 1;
    }

    private static void checkCounts(int repos, int lastsDays, String presence, boolean localFiles) {
        if (repos < 1 || lastsDays < 1 || (presence.equals("away") && localFiles)) throw new IllegalArgumentException("repos and lasts_days start at 1, and a cloud run starts from a fresh clone and sees no local files");
    }

    private static Choice apiChoice(Map<String, Object> s) {
        if (flag(s, "builtin_covers")) return new Choice("builtin-tool", "provided-schema");
        if (flag(s, "external_system") && flag(s, "remote_server")) return new Choice("mcp", "remote-server");
        return null;
    }

    private static Choice priorityChoice(Map<String, Object> s) {
        if (flag(s, "guarantee")) return new Choice("hook", "must-hold-every-time");
        if (flag(s, "external_system")) return new Choice("mcp", "external-system");
        if (flag(s, "noisy")) return new Choice("subagent", "isolate-context");
        return null;
    }

    private static Choice eventChoice(String presence) {
        return presence.equals("away") ? new Choice("routine", "runs-unattended") : new Choice("monitor", "push-not-poll");
    }

    private static Choice intervalChoice(Map<String, Object> s) {
        if (text(s, "presence", "session").equals("away")) return new Choice("routine", "runs-unattended");
        if (number(s, "lasts_days") > LOOP_EXPIRY_DAYS) return flag(s, "local_files") ? new Choice("desktop-task", "durable-and-local") : new Choice("routine", "runs-unattended");
        return new Choice("loop", "session-rhythm");
    }

    /** The pick for work that runs without a person, on a rhythm or on an event, or null when the situation has none of these. */
    private static Choice rhythm(Map<String, Object> s) {
        String timing = text(s, "timing", "none");
        String presence = text(s, "presence", "session");
        if (presence.equals("pipeline")) return new Choice("headless-ci", "no-person-present");
        if (timing.equals("condition")) return new Choice("goal", "until-condition-holds");
        if (timing.equals("background")) return new Choice("background-task", "work-while-it-runs");
        if (timing.equals("event")) return eventChoice(presence);
        if (timing.equals("interval")) return intervalChoice(s);
        return null;
    }

    private static Choice personalChoice(String personal) {
        if (personal.equals("voice")) return new Choice("output-style", "response-voice");
        if (personal.equals("display")) return new Choice("status-line", "personal-display");
        if (personal.equals("keys")) return new Choice("keybinding", "personal-keys");
        return null;
    }

    private static Choice knowledgeChoice(String knowledge, boolean pathScoped) {
        if (knowledge.equals("convention")) return pathScoped ? new Choice("path-rule", "scoped-convention") : new Choice("claude-md", "always-known");
        if (knowledge.equals("reference")) return new Choice("skill", "on-demand-reference");
        if (knowledge.equals("procedure")) return new Choice("skill", "repeatable-procedure");
        return null;
    }

    private static Choice packageChoice(Choice choice, int repos) {
        if (repos >= 2 && CARRIED.contains(choice.mechanism())) return new Choice("plugin", "shared-setup");
        return choice;
    }

    static Choice choose(Map<String, Object> situation) {
        LOG.log(System.Logger.Level.DEBUG, "choose input: {0}", situation);
        String surface = text(situation, "surface", "code");
        String knowledge = text(situation, "knowledge", "none");
        int repos = number(situation, "repos");
        if (!SURFACES.contains(surface)) throw new IllegalArgumentException("unknown surface: " + surface);
        if (!KNOWLEDGE.contains(knowledge)) throw new IllegalArgumentException("unknown knowledge kind: " + knowledge);
        if (!TIMINGS.contains(text(situation, "timing", "none"))) throw new IllegalArgumentException("unknown timing: " + situation.get("timing"));
        if (!PRESENCE.contains(text(situation, "presence", "session"))) throw new IllegalArgumentException("unknown presence: " + situation.get("presence"));
        if (!PERSONAL.contains(text(situation, "personal", "none"))) throw new IllegalArgumentException("unknown personal preference: " + situation.get("personal"));
        checkCounts(repos, number(situation, "lasts_days"), text(situation, "presence", "session"), flag(situation, "local_files"));
        if (surface.equals("api")) {
            Choice api = apiChoice(situation);
            return api != null ? api : new Choice("api-tool", "own-schema-and-code");
        }
        Choice choice = priorityChoice(situation);
        if (choice == null) choice = rhythm(situation);
        if (choice == null) choice = personalChoice(text(situation, "personal", "none"));
        if (choice == null) choice = knowledgeChoice(knowledge, flag(situation, "path_scoped"));
        if (choice == null) choice = new Choice("builtin-tool", "built-in-covers");
        return packageChoice(choice, repos);
    }
}
