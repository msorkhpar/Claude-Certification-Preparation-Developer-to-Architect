import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Workflow patterns around a model: orchestrator and workers, evaluator and optimiser, routing and voting. See ../../statement.md. Results are JSON-like maps. */
final class Workflows {
    private Workflows() {}

    static Map<String, Object> orchestrate(Function<String, String> ask, String task) {
        return orchestrate(ask, task, 5);
    }

    static Map<String, Object> orchestrate(Function<String, String> ask, String task, int maxSubtasks) {
        // TODO: plan with one call, run a worker call for each subtask, combine the results with one more call.
        return null;
    }

    static Map<String, Object> refine(Function<String, String> write, Function<String, String> judge, String task) {
        return refine(write, judge, task, 3, 8);
    }

    static Map<String, Object> refine(Function<String, String> write, Function<String, String> judge, String task, int maxRounds, int threshold) {
        // TODO: write a draft, judge it, and revise with the feedback until the score reaches the threshold or the rounds run out.
        return null;
    }

    static Map<String, Object> route(Function<String, String> ask, String text, Map<String, Function<String, String>> routes, String defaultLabel) {
        // TODO: classify the text with one model call, then run the handler of the label (or of the default label).
        return null;
    }

    static Map<String, Object> vote(Function<String, String> ask, String prompt) {
        return vote(ask, prompt, 5);
    }

    static Map<String, Object> vote(Function<String, String> ask, String prompt, int n) {
        // TODO: ask n times and return the majority answer, the counts and the share of the winner.
        return null;
    }
}
