import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Task decomposition: a per-item pass and a cross-item pass, an adaptive loop, and the choice between them. See ../../statement.md. Results are JSON-like maps. */
final class Decompose {
    private static final System.Logger LOG = System.getLogger(Decompose.class.getName());
    private Decompose() {}

    /** The model's review of one file, or of one part of it: a map with "findings" (a list of text) and "summary" (text). */
    interface FilePass {
        Map<String, Object> apply(String path, String text, int part, int parts);
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    static Map<String, Object> reviewChanges(List<Map<String, String>> files, FilePass filePass, Function<List<Map<String, String>>, List<String>> crossPass) {
        return reviewChanges(files, filePass, crossPass, 40);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> reviewChanges(List<Map<String, String>> files, FilePass filePass, Function<List<Map<String, String>>, List<String>> crossPass, int maxLines) {
        LOG.log(System.Logger.Level.DEBUG, "reviewChanges input: {0}", files);
        Map<String, Object> reviewed = new LinkedHashMap<>();
        Map<String, Object> failed = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();
        for (Map<String, String> item : files) {
            String path = item.get("path");
            String text = item.get("text");
            // TODO 2 of 9 (finish this to pass e2): the blank file rule. When the file's text is blank, add its path to
            //   `skipped` and go on to the next file. Example: "\n  \n" -> skipped.
            List<String> lines = text.lines().toList();
            // TODO 3 of 9 (finish this to pass e2): the number of parts. Receives the lines of the file and max_lines.
            //   Return how many parts of at most max_lines lines the file needs, rounding up. Example: 41 lines, max_lines
            //   40 -> 2.
            int parts = 1;
            List<String> findings = new ArrayList<>();
            List<String> summaries = new ArrayList<>();
            try {
                for (int part = 0; part < parts; part++) {
                    // TODO 1 of 9 (finish this to pass m1, e1): the review of one part of a file. Slice the part's lines
                    //   (part number 1..parts, max_lines per part), call the file pass with (path, text of the part, part
                    //   number from 1, parts), add its findings to `findings` and its summary to `summaries`. Example: a 5
                    //   line file, max_lines 3 -> two calls, parts 1 and 2 of 2.
                }
            } catch (RuntimeException error) {
                // TODO 4 of 9 (finish this to pass e3): the failing file. When the file pass throws, record the path with
                //   the error's message in `failed` and go on to the next file. Example: the pass raises "boom" for a.py ->
                //   failed {a.py: "boom"}, and b.py is still reviewed.
                continue;
            }
            reviewed.put(path, map("findings", findings, "summary", String.join(" ", summaries), "parts", parts));
        }
        List<String> cross = new ArrayList<>();
        String crossError = null;
        // TODO 5 of 9 (finish this to pass e3): the guard of the cross pass. Run it only when at least two files were
        //   reviewed (a relation between files needs two). Example: one reviewed file and one failed -> no cross pass.
        if (reviewed.size() >= 0) {
            List<Map<String, String>> summaries = new ArrayList<>();
            for (Map.Entry<String, Object> e : reviewed.entrySet()) {
                Map<String, String> one = new LinkedHashMap<>();
                one.put("path", e.getKey());
                one.put("summary", (String) ((Map<String, Object>) e.getValue()).get("summary"));
                summaries.add(one);
            }
            try {
                // TODO 6 of 9 (finish this to pass m1, e1): the cross pass call. Call the cross pass with one {path,
                //   summary} entry per reviewed file (the joined summary, never the text) and keep what it returns in
                //   `cross`. Example: two files -> one call with two entries.
                cross = new ArrayList<>();
            } catch (RuntimeException error) {
                crossError = String.valueOf(error.getMessage());
            }
        }
        return map("files", reviewed, "cross", cross, "failed", failed, "skipped", skipped, "cross_error", crossError);
    }

    static Map<String, Object> runAdaptive(BiFunction<String, List<Map<String, String>>, Object> planner, Function<String, String> worker, String goal) {
        return runAdaptive(planner, worker, goal, 6);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> runAdaptive(BiFunction<String, List<Map<String, String>>, Object> planner, Function<String, String> worker, String goal, int maxSteps) {
        List<Map<String, String>> steps = new ArrayList<>();
        while (true) {
            List<Map<String, String>> history = new ArrayList<>();
            // TODO 7 of 9 (finish this to pass e4): the question to the planner. Call the planner with the goal and a
            //   copy of the steps done so far (each {subtask, result}). Example: after two steps the planner receives a
            //   list of two.
            Object reply = planner.apply(goal, history);
            if (!(reply instanceof Map<?, ?> plan) || !(plan.get("done") instanceof Boolean done)) {
                return map("status", "bad_plan", "summary", "", "steps", steps, "reason", "the planner reply could not be read");
            }
            if (done) return map("status", "done", "summary", plan.get("summary") == null ? "" : String.valueOf(plan.get("summary")), "steps", steps, "reason", "");
            String subtask = plan.get("next") == null ? "" : String.valueOf(plan.get("next")).strip();
            // TODO 8 of 9 (finish this to pass e5): the stuck rules. When the planner gives no next step, finish with
            //   status stuck and the reason "no next step"; when the next step was already done (compare ignoring case),
            //   finish with status stuck and the reason "repeated subtask: NAME". Example: next "Fix A" after "fix a" ->
            //   stuck.
            if (steps.size() >= maxSteps) return map("status", "step_limit", "summary", "", "steps", steps, "reason", "step limit reached");
            String result;
            try {
                result = worker.apply(subtask);
            } catch (RuntimeException error) { // the planner decides what a failed step means
                result = "ERROR: " + error.getMessage();
            }
            Map<String, String> step = new LinkedHashMap<>();
            step.put("subtask", subtask);
            step.put("result", result);
            steps.add(step);
        }
    }

    static String chooseStrategy(Map<String, Object> task) {
        Object stepsKnown = task.get("steps_known");
        Object items = task.get("items");
        if (!(stepsKnown instanceof Boolean known) || !(items instanceof Integer count) || count < 0) {
            throw new IllegalArgumentException("steps_known must be true or false and items a whole number of at least 0");
        }
        // TODO 9 of 9 (finish this to pass e6): the choice. Receives the validated fields. Return adaptive when the
        //   steps are not known; otherwise per_item_then_cross when there are at least 2 items and items_interact is true;
        //   otherwise fixed_chain. Example: steps known, 3 items that interact -> per_item_then_cross.
        return "fixed_chain";
    }
}
