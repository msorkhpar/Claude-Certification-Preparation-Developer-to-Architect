import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Task decomposition: a per-item pass and a cross-item pass, an adaptive loop, and the choice between them. See ../../statement.md. Results are JSON-like maps. */
final class Decompose {
    private Decompose() {}

    /** The model's review of one file, or of one part of it: a map with "findings" (a list of text) and "summary" (text). */
    interface FilePass {
        Map<String, Object> apply(String path, String text, int part, int parts);
    }

    static Map<String, Object> reviewChanges(List<Map<String, String>> files, FilePass filePass, Function<List<Map<String, String>>, List<String>> crossPass) {
        return reviewChanges(files, filePass, crossPass, 40);
    }

    static Map<String, Object> reviewChanges(List<Map<String, String>> files, FilePass filePass, Function<List<Map<String, String>>, List<String>> crossPass, int maxLines) {
        // TODO: review each file alone (long files in parts), then run one cross pass over the summaries of the reviewed files.
        return null;
    }

    static Map<String, Object> runAdaptive(BiFunction<String, List<Map<String, String>>, Object> planner, Function<String, String> worker, String goal) {
        return runAdaptive(planner, worker, goal, 6);
    }

    static Map<String, Object> runAdaptive(BiFunction<String, List<Map<String, String>>, Object> planner, Function<String, String> worker, String goal, int maxSteps) {
        // TODO: ask the planner what to do next after every step, and stop when it is done, stuck or out of steps.
        return null;
    }

    static String chooseStrategy(Map<String, Object> task) {
        // TODO: fixed_chain, per_item_then_cross or adaptive.
        return null;
    }
}
