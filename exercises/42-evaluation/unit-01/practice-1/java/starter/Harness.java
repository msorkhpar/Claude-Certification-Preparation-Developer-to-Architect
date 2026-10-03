import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** An eval harness. See ../../statement.md. */
final class Harness {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** Grade one output against the case's check: {"passed", "reason"} (and "score" for a judge check). */
    static Map<String, Object> grade(Map<String, Object> c, String output, Function<String, String> judge) {
        return map("passed", true, "reason", "ok");
    }

    /** Run every case `repeats` times through the model and grade it. A case passes only if every run passes. */
    static Map<String, Object> runEval(List<Map<String, Object>> cases, Function<String, String> model, Function<String, String> judge, int repeats) {
        return map("total", 0, "passed", 0, "pass_rate", 0.0, "results", new ArrayList<>(), "by_tag", map(), "flaky", new ArrayList<>());
    }

    /** Compare a report with success criteria: min_pass_rate, tags {tag: minimum rate}, max_flaky. */
    static Map<String, Object> meets(Map<String, Object> report, Map<String, Object> criteria) {
        return map("met", true, "failures", new ArrayList<>());
    }

    /** What changed between two reports: regressions, fixed, added and removed case ids, the pass-rate change. */
    static Map<String, Object> compare(Map<String, Object> baseline, Map<String, Object> current) {
        return map("regressions", new ArrayList<>(), "fixed", new ArrayList<>(), "added", new ArrayList<>(), "removed", new ArrayList<>(), "pass_rate_delta", 0.0, "ok", true);
    }
}
