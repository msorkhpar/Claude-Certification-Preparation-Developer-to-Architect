import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
        Map<String, Object> reviewed = new LinkedHashMap<>();
        Map<String, Object> failed = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();
        for (Map<String, String> item : files) {
            String path = item.get("path");
            String text = item.get("text");
            if (text.isBlank()) {
                skipped.add(path); // nothing to review is not worth a model call
                continue;
            }
            List<String> lines = text.lines().toList();
            int parts = (lines.size() + maxLines - 1) / maxLines;
            List<String> findings = new ArrayList<>();
            List<String> summaries = new ArrayList<>();
            try {
                for (int part = 0; part < parts; part++) {
                    String chunk = String.join("\n", lines.subList(part * maxLines, Math.min(lines.size(), (part + 1) * maxLines)));
                    Map<String, Object> result = filePass.apply(path, chunk, part + 1, parts);
                    findings.addAll((List<String>) result.get("findings"));
                    summaries.add((String) result.get("summary"));
                }
            } catch (RuntimeException error) { // one file failing must not stop the others
                failed.put(path, String.valueOf(error.getMessage()));
                continue;
            }
            reviewed.put(path, map("findings", findings, "summary", String.join(" ", summaries), "parts", parts));
        }
        List<String> cross = new ArrayList<>();
        String crossError = null;
        if (reviewed.size() >= 2) { // a relation between files needs at least two of them
            List<Map<String, String>> summaries = new ArrayList<>();
            for (Map.Entry<String, Object> e : reviewed.entrySet()) {
                Map<String, String> one = new LinkedHashMap<>();
                one.put("path", e.getKey());
                one.put("summary", (String) ((Map<String, Object>) e.getValue()).get("summary"));
                summaries.add(one);
            }
            try {
                cross = new ArrayList<>(crossPass.apply(summaries));
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
            for (Map<String, String> step : steps) history.add(new LinkedHashMap<>(step));
            Object reply = planner.apply(goal, new ArrayList<>());
            if (!(reply instanceof Map<?, ?> plan) || !(plan.get("done") instanceof Boolean done)) {
                return map("status", "bad_plan", "summary", "", "steps", steps, "reason", "the planner reply could not be read");
            }
            if (done) return map("status", "done", "summary", plan.get("summary") == null ? "" : String.valueOf(plan.get("summary")), "steps", steps, "reason", "");
            String subtask = plan.get("next") == null ? "" : String.valueOf(plan.get("next")).strip();
            if (subtask.isEmpty()) return map("status", "stuck", "summary", "", "steps", steps, "reason", "no next step");
            for (Map<String, String> step : steps) {
                if (step.get("subtask").toLowerCase(Locale.ROOT).equals(subtask.toLowerCase(Locale.ROOT))) {
                    return map("status", "stuck", "summary", "", "steps", steps, "reason", "repeated subtask: " + subtask);
                }
            }
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
        if (!known) return "adaptive";
        if (count >= 2 && Boolean.TRUE.equals(task.get("items_interact"))) return "per_item_then_cross";
        return "fixed_chain";
    }
}
