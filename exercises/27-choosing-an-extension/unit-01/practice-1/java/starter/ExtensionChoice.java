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
        // TODO 1 of 8 (finish this to pass e6): refuse numbers and a combination that make no sense.
        // Receives repos, lastsDays, presence and localFiles (already defaulted). Throws IllegalArgumentException when repos or lastsDays is
        // below 1, or when presence is "away" and localFiles is true (a cloud run starts from a fresh clone). Returns nothing otherwise.
        // Example: checkCounts(0, 1, "session", false) throws; checkCounts(1, 1, "away", true) throws; checkCounts(2, 30, "away", false) returns
    }

    private static Choice apiChoice(Map<String, Object> s) {
        // TODO 2 of 8 (finish this to pass e5): the pick of an application on the Messages API, or null for the default own tool.
        // Receives the situation. Returns builtin-tool / provided-schema when builtin_covers is true, else mcp / remote-server when
        // external_system and remote_server are both true, else null (the caller then answers api-tool / own-schema-and-code).
        // Example: apiChoice(Map.of("builtin_covers", true, "remote_server", true)) -> builtin-tool; apiChoice(Map.of("external_system", true)) -> null
        return null;
    }

    private static Choice priorityChoice(Map<String, Object> s) {
        // TODO 3 of 8 (finish this to pass e1 and e2): the pick for a rule that must hold, an outside system or noisy work.
        // Receives the situation. The first match wins: guarantee gives hook / must-hold-every-time; external_system gives mcp / external-system;
        // noisy gives subagent / isolate-context; otherwise null. Whatever else is in the situation (timing, knowledge) does not matter here.
        // Example: priorityChoice(Map.of("guarantee", true, "noisy", true)) -> hook; priorityChoice(Map.of("timing", "event")) -> null
        return null;
    }

    private static Choice eventChoice(String presence) {
        // TODO 4 of 8 (finish this to pass e7): the pick for work that reacts to an event.
        // Receives presence. Returns routine / runs-unattended when presence is "away", else monitor / push-not-poll.
        // Example: eventChoice("session") -> monitor / push-not-poll
        return null;
    }

    private static Choice intervalChoice(Map<String, Object> s) {
        // TODO 5 of 8 (finish this to pass e8): the pick for work that repeats every so often.
        // Receives the situation. Returns routine / runs-unattended when presence is "away"; else, when lasts_days is above LOOP_EXPIRY_DAYS,
        // desktop-task / durable-and-local if local_files is true and routine / runs-unattended if not; else loop / session-rhythm.
        // Example: intervalChoice(Map.of("lasts_days", 8, "local_files", true)) -> desktop-task; intervalChoice(Map.of("lasts_days", 7)) -> loop
        return null;
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
        // TODO 6 of 8 (finish this to pass e9): the pick for a preference of one person, or null.
        // Receives the personal value. Returns output-style / response-voice for "voice", status-line / personal-display for "display",
        // keybinding / personal-keys for "keys", and null for "none".
        // Example: personalChoice("keys") -> keybinding / personal-keys; personalChoice("none") -> null
        return null;
    }

    private static Choice knowledgeChoice(String knowledge, boolean pathScoped) {
        // TODO 7 of 8 (finish this to pass e3): the pick for what Claude must be told, or null when nothing needs telling.
        // Receives knowledge and pathScoped. "convention" gives path-rule / scoped-convention when pathScoped, else claude-md / always-known;
        // "reference" gives skill / on-demand-reference; "procedure" gives skill / repeatable-procedure; anything else gives null. pathScoped
        // matters only for a convention.
        // Example: knowledgeChoice("reference", true) -> skill / on-demand-reference; knowledgeChoice("none", true) -> null
        return null;
    }

    private static Choice packageChoice(Choice choice, int repos) {
        // TODO 8 of 8 (finish this to pass e4): a plugin carries a skill, a hook, a subagent or a server to a second repository.
        // Receives the pick and repos. Returns plugin / shared-setup when repos is 2 or more and the pick's mechanism is in CARRIED; otherwise
        // returns the pick unchanged (an instruction file, a path rule and a built-in tool are never packaged).
        // Example: packageChoice(new Choice("hook", "must-hold-every-time"), 2) -> plugin; packageChoice(new Choice("claude-md", "always-known"), 2) -> unchanged
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
