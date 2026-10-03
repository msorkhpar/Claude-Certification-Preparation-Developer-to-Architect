import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Distributing tools across agents: scoped tool sets, the tool choice of a turn, a check of the reply and the authorisation of a call. See ../../statement.md. Results are JSON-like maps. */
final class Distribute {
    private Distribute() {}

    /** Models whose API rejects tool_choice any and tool, as read on 2026-10-03. */
    private static final java.util.Set<String> NO_FORCING = java.util.Set.of("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1");

    static Map<String, List<String>> assignTools(Map<String, Map<String, Object>> roles, List<Map<String, Object>> catalog) {
        return assignTools(roles, catalog, 5);
    }

    static Map<String, List<String>> assignTools(Map<String, Map<String, Object>> roles, List<Map<String, Object>> catalog, int budget) {
        // TODO: give each role the tools of its specialisation, plus what it is explicitly granted; refuse a set that is too large.
        return null;
    }

    static Map<String, Object> planTurn(String model, String need, List<String> tools) {
        return planTurn(model, need, tools, null, false);
    }

    static Map<String, Object> planTurn(String model, String need, List<String> tools, String forced) {
        return planTurn(model, need, tools, forced, false);
    }

    static Map<String, Object> planTurn(String model, String need, List<String> tools, String forced, boolean manualThinking) {
        // TODO: the tool_choice and tool list of one request, with the portable fallback where forcing is not accepted.
        return null;
    }

    static String cacheImpact(Map<String, Object> previous, Map<String, Object> next) {
        // TODO: what does a change between two requests cost in prompt caching: none, messages or all?
        return null;
    }

    static String checkTurn(List<Map<String, Object>> blocks, String need) {
        return checkTurn(blocks, need, null);
    }

    static String checkTurn(List<Map<String, Object>> blocks, String need, String forced) {
        // TODO: compare a reply with the call that was required: ok, missed_call or wrong_tool.
        return null;
    }

    static Map<String, Object> authorize(Map<String, Object> call, Map<String, Object> policy, Collection<String> approvals) {
        // TODO: decide a call to a tool that cannot be undone, in the tool layer.
        return null;
    }
}
