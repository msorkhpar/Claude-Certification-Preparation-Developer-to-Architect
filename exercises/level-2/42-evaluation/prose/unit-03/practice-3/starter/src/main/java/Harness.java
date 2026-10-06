import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/** An eval harness. See ../../statement.md. */
final class Harness {
    private static final System.Logger LOG = System.getLogger(Harness.class.getName());

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static String norm(String text) {
        // TODO 1 of 9 (unlocks e1): the form two texts are compared in for an exact check.
        // Receives a text and returns it trimmed, every run of white space made one space, and lower-cased. Nothing else changes.
        // Example: norm("  Paris   FRANCE ") -> "paris france"
        return text;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) {
        return (List<Object>) o;
    }

    private static Map<String, Object> verdict(boolean ok, String failReason) {
        return map("passed", ok, "reason", ok ? "ok" : failReason);
    }

    static String judgePrompt(String criterion, String output) {
        return "Rate this response on a scale of 1-5 for " + criterion + ":\n<response>" + output + "</response>\n"
            + "1: Not at all " + criterion + "\n5: Perfectly " + criterion + "\nOutput only the number.";
    }

    private static Map<String, Object> fieldResult(Map<String, Object> obj, Map<String, Object> check) {
        // TODO 2 of 9 (unlocks e2): grade a parsed JSON object against a json_field check.
        // Receives the parsed object and the check {field, equals}. Returns `verdict(passed, reason)`: "missing field" when the field is
        // absent; "ok" when the value equals `equals` with the same JSON type (Objects.equals does that); otherwise "mismatch".
        // Example: {"n": "3"} against equals 3 -> {passed=false, reason=mismatch}
        return verdict(false, "mismatch");
    }

    private static Integer parseScore(String reply) {
        // TODO 3 of 9 (unlocks e3): read the judge's reply as a score.
        // Receives the reply (may be null). Returns the number when the trimmed reply is exactly one digit from 1 to 5, else null.
        // Example: parseScore(" 4 ") -> 4, parseScore("I give it a 4") -> null
        return null;
    }

    private static Map<String, Object> judged(int score, long threshold) {
        // TODO 4 of 9 (unlocks e3): the verdict for a judge score.
        // Receives the score and the threshold. Returns a map {passed, reason, score}: "ok" at or above the threshold, else "below threshold".
        // Example: judged(4, 4) -> {passed=true, reason=ok, score=4}
        return map("passed", false, "reason", "below threshold", "score", score);
    }

    /** Grade one output against the case's check: {"passed", "reason"} (and "score" for a judge check). */
    static Map<String, Object> grade(Map<String, Object> c, String output, Function<String, String> judge) {
        LOG.log(System.Logger.Level.DEBUG, "grade input: {0}", output);
        Map<String, Object> check = asMap(c.get("check"));
        String kind = (String) check.get("type");
        switch (kind) {
            case "exact":
                return verdict(norm(output).equals(norm((String) check.get("expected"))), "mismatch");
            case "regex":
                return verdict(Pattern.compile((String) check.get("pattern")).matcher(output).find(), "mismatch");
            case "json_field": {
                Object data;
                try {
                    data = Json.parse(output);
                } catch (RuntimeException e) {
                    return verdict(false, "not json");
                }
                if (!(data instanceof Map<?, ?>)) return verdict(false, "not json");
                Map<String, Object> obj = asMap(data);
                return fieldResult(obj, check);
            }
            case "judge": {
                if (judge == null) return map("passed", false, "reason", "ungradable", "score", null);
                String reply;
                try {
                    reply = judge.apply(judgePrompt((String) check.get("criterion"), output));
                } catch (RuntimeException e) {
                    return map("passed", false, "reason", "ungradable", "score", null);
                }
                Integer score = parseScore(reply);
                if (score == null) return map("passed", false, "reason", "ungradable", "score", null);
                long threshold = check.get("threshold") instanceof Number n ? n.longValue() : 4;
                return judged(score, threshold);
            }
            default:
                return verdict(false, "ungradable");
        }
    }

    private static Map<String, Object> runOnce(Function<String, String> model, Function<String, String> judge, Map<String, Object> c) {
        // TODO 5 of 9 (unlocks e4): one run of one case.
        // Receives the model, the judge and the case. Returns the verdict of `grade` for the model's output; when the model throws, the
        // verdict is {passed=false, reason=model error} and nothing is thrown. Example: a model that throws -> {passed=false, reason=model error}
        return map("passed", false, "reason", "model error");
    }

    private static Map<String, Object> outcome(List<Map<String, Object>> runs) {
        // TODO 6 of 9 (unlocks e7): combine the runs of one case.
        // Receives the verdicts of its runs. Returns a map {passed, mixed, reason}: passed only if every run passed; mixed (flaky) when
        // some passed and some failed; the reason is "ok", or the reason of the first failed run.
        // Example: [ok, mismatch] -> {passed=false, mixed=true, reason=mismatch}
        return map("passed", false, "mixed", false, "reason", "mismatch");
    }

    private static void countTags(Map<String, Object> byTag, List<?> tags, boolean passed) {
        // TODO 7 of 9 (unlocks e5): count one case under each of its tags.
        // Receives the map `byTag` (changed in place), the case's tags and whether it passed. For every tag, make a row
        // {passed=0, total=0} when there is none (computeIfAbsent), then `total` goes up by one and `passed` by one when the case passed.
        // Example: tags ["a"], passed -> byTag.get("a") is {passed=1, total=1}
    }

    /** Run every case `repeats` times through the model and grade it. A case passes only if every run passes. */
    static Map<String, Object> runEval(List<Map<String, Object>> cases, Function<String, String> model, Function<String, String> judge, int repeats) {
        List<Object> results = new ArrayList<>();
        Map<String, Object> byTag = new LinkedHashMap<>();
        List<Object> flaky = new ArrayList<>();
        int passedCount = 0;
        for (Map<String, Object> c : cases) {
            List<Map<String, Object>> runs = new ArrayList<>();
            for (int i = 0; i < repeats; i++) runs.add(runOnce(model, judge, c));
            Map<String, Object> outcome = outcome(runs);
            boolean passed = (Boolean) outcome.get("passed");
            boolean mixed = (Boolean) outcome.get("mixed");
            String reason = (String) outcome.get("reason");
            results.add(map("id", c.get("id"), "passed", passed, "reason", reason, "flaky", mixed));
            if (mixed) flaky.add(c.get("id"));
            if (passed) passedCount++;
            if (c.get("tags") instanceof List<?> tags) countTags(byTag, tags, passed);
        }
        int total = results.size();
        return map("total", total, "passed", passedCount, "pass_rate", total == 0 ? 0.0 : (double) passedCount / total,
            "results", results, "by_tag", byTag, "flaky", flaky);
    }

    private static boolean tagFailed(Object row, double minimum) {
        // TODO 8 of 9 (unlocks e5): does one tag fail its minimum rate?
        // Receives the tag's row {passed, total} or null (no case carries the tag) and the minimum rate. Returns true for null, an empty
        // row, or a rate below the minimum; a rate equal to the minimum is fine. Example: ({passed=1, total=2}, 0.5) -> false
        return false;
    }

    /** Compare a report with success criteria: min_pass_rate, tags {tag: minimum rate}, max_flaky. */
    static Map<String, Object> meets(Map<String, Object> report, Map<String, Object> criteria) {
        List<Object> failures = new ArrayList<>();
        if (criteria.get("min_pass_rate") instanceof Number min && ((Number) report.get("pass_rate")).doubleValue() < min.doubleValue()) failures.add("overall");
        if (criteria.get("tags") instanceof Map<?, ?> tags) {
            Map<String, Object> byTag = asMap(report.get("by_tag"));
            for (Map.Entry<String, Object> e : asMap(tags).entrySet()) {
                if (tagFailed(byTag.get(e.getKey()), ((Number) e.getValue()).doubleValue())) failures.add("tag:" + e.getKey());
            }
        }
        if (criteria.get("max_flaky") instanceof Number max && asList(report.get("flaky")).size() > max.longValue()) failures.add("flaky");
        return map("met", failures.isEmpty(), "failures", failures);
    }

    private static Map<String, Boolean> outcomes(Map<String, Object> report) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        for (Object r : asList(report.get("results"))) out.put((String) asMap(r).get("id"), (Boolean) asMap(r).get("passed"));
        return out;
    }

    private static List<List<Object>> changes(Map<String, Boolean> before, Map<String, Boolean> now) {
        // TODO 9 of 9 (unlocks e6): what changed between two runs.
        // Receives two maps {case id -> passed}, the baseline and the current run. Returns a list of four id lists: regressions (passed
        // before, fails now), fixed (failed before, passes now), added (only in the current run), removed (only in the baseline). The
        // first three follow the current order, removed the baseline's. Example: before {a=true}, now {a=false, b=true} -> [[a], [], [b], []]
        return List.of(new ArrayList<Object>(), new ArrayList<Object>(), new ArrayList<Object>(), new ArrayList<Object>());
    }

    /** What changed between two reports: regressions, fixes, added and removed cases, the pass-rate change. */
    static Map<String, Object> compare(Map<String, Object> baseline, Map<String, Object> current) {
        Map<String, Boolean> before = outcomes(baseline), now = outcomes(current);
        List<List<Object>> diff = changes(before, now);
        List<Object> regressions = diff.get(0), fixed = diff.get(1), added = diff.get(2), removed = diff.get(3);
        double delta = ((Number) current.get("pass_rate")).doubleValue() - ((Number) baseline.get("pass_rate")).doubleValue();
        return map("regressions", regressions, "fixed", fixed, "added", added, "removed", removed, "pass_rate_delta", delta,
            "ok", regressions.isEmpty() && removed.isEmpty());
    }
}
