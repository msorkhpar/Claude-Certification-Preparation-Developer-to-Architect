import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/** A coordinator that delegates to isolated subagents, reviews what comes back and synthesizes. See ../../statement.md. Results are JSON-like maps. */
final class Coordinator {
    private Coordinator() {}

    static Map<String, Object> coordinate(Function<String, Map<String, Object>> planner, Function<String, String> subagent,
            BiFunction<String, List<Map<String, Object>>, List<String>> reviewer, BiFunction<String, List<Map<String, Object>>, String> synthesizer, String question) {
        return coordinate(planner, subagent, reviewer, synthesizer, question, 4, 2);
    }

    static Map<String, Object> coordinate(Function<String, Map<String, Object>> planner, Function<String, String> subagent,
            BiFunction<String, List<Map<String, Object>>, List<String>> reviewer, BiFunction<String, List<Map<String, Object>>, String> synthesizer,
            String question, int maxAgents, int maxRounds) {
        // TODO: plan, delegate one brief per subagent, review the findings for gaps, delegate the gaps, then synthesize once.
        return null;
    }
}
