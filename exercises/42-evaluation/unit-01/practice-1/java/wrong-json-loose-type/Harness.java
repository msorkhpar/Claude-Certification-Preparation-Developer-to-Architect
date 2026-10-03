import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/** An eval harness. See ../../statement.md. */
final class Harness {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static String norm(String text) {
        return text.trim().replaceAll("\\s+", " ").toLowerCase();
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

    /** Grade one output against the case's check: {"passed", "reason"} (and "score" for a judge check). */
    static Map<String, Object> grade(Map<String, Object> c, String output, Function<String, String> judge) {
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
                if (!obj.containsKey((String) check.get("field"))) return verdict(false, "missing field");
                return verdict(String.valueOf(obj.get((String) check.get("field"))).equals(String.valueOf(check.get("equals"))), "mismatch");
            }
            case "judge": {
                if (judge == null) return map("passed", false, "reason", "ungradable", "score", null);
                String reply;
                try {
                    reply = judge.apply(judgePrompt((String) check.get("criterion"), output));
                } catch (RuntimeException e) {
                    return map("passed", false, "reason", "ungradable", "score", null);
                }
                String text = reply == null ? "" : reply.strip();
                if (!text.matches("[1-5]")) return map("passed", false, "reason", "ungradable", "score", null);
                int score = Integer.parseInt(text);
                long threshold = check.get("threshold") instanceof Number n ? n.longValue() : 4;
                return score >= threshold ? map("passed", true, "reason", "ok", "score", score) : map("passed", false, "reason", "below threshold", "score", score);
            }
            default:
                return verdict(false, "ungradable");
        }
    }

    /** Run every case `repeats` times through the model and grade it. A case passes only if every run passes. */
    static Map<String, Object> runEval(List<Map<String, Object>> cases, Function<String, String> model, Function<String, String> judge, int repeats) {
        List<Object> results = new ArrayList<>();
        Map<String, Object> byTag = new LinkedHashMap<>();
        List<Object> flaky = new ArrayList<>();
        int passedCount = 0;
        for (Map<String, Object> c : cases) {
            List<Map<String, Object>> runs = new ArrayList<>();
            for (int i = 0; i < repeats; i++) {
                String output;
                try {
                    output = model.apply((String) c.get("input"));
                } catch (RuntimeException e) {
                    runs.add(map("passed", false, "reason", "model error"));
                    continue;
                }
                runs.add(grade(c, output, judge));
            }
            boolean passed = runs.stream().allMatch(r -> (Boolean) r.get("passed"));
            boolean mixed = runs.stream().anyMatch(r -> (Boolean) r.get("passed")) && !passed;
            String reason = "ok";
            if (!passed) reason = (String) runs.stream().filter(r -> !(Boolean) r.get("passed")).findFirst().get().get("reason");
            results.add(map("id", c.get("id"), "passed", passed, "reason", reason, "flaky", mixed));
            if (mixed) flaky.add(c.get("id"));
            if (passed) passedCount++;
            if (c.get("tags") instanceof List<?> tags) {
                for (Object tag : tags) {
                    Map<String, Object> row = asMap(byTag.computeIfAbsent((String) tag, k -> map("passed", 0, "total", 0)));
                    row.put("total", (Integer) row.get("total") + 1);
                    row.put("passed", (Integer) row.get("passed") + (passed ? 1 : 0));
                }
            }
        }
        int total = results.size();
        return map("total", total, "passed", passedCount, "pass_rate", total == 0 ? 0.0 : (double) passedCount / total,
            "results", results, "by_tag", byTag, "flaky", flaky);
    }

    /** Compare a report with success criteria: min_pass_rate, tags {tag: minimum rate}, max_flaky. */
    static Map<String, Object> meets(Map<String, Object> report, Map<String, Object> criteria) {
        List<Object> failures = new ArrayList<>();
        if (criteria.get("min_pass_rate") instanceof Number min && ((Number) report.get("pass_rate")).doubleValue() < min.doubleValue()) failures.add("overall");
        if (criteria.get("tags") instanceof Map<?, ?> tags) {
            Map<String, Object> byTag = asMap(report.get("by_tag"));
            for (Map.Entry<String, Object> e : asMap(tags).entrySet()) {
                Object row = byTag.get(e.getKey());
                if (row == null) {
                    failures.add("tag:" + e.getKey());
                    continue;
                }
                double total = ((Number) asMap(row).get("total")).doubleValue();
                double passed = ((Number) asMap(row).get("passed")).doubleValue();
                if (total == 0 || passed / total < ((Number) e.getValue()).doubleValue()) failures.add("tag:" + e.getKey());
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

    /** What changed between two reports: regressions, fixes, added and removed cases, the pass-rate change. */
    static Map<String, Object> compare(Map<String, Object> baseline, Map<String, Object> current) {
        Map<String, Boolean> before = outcomes(baseline), now = outcomes(current);
        List<Object> regressions = new ArrayList<>(), fixed = new ArrayList<>(), added = new ArrayList<>(), removed = new ArrayList<>();
        for (String id : now.keySet()) {
            if (!before.containsKey(id)) added.add(id);
            else if (before.get(id) && !now.get(id)) regressions.add(id);
            else if (!before.get(id) && now.get(id)) fixed.add(id);
        }
        for (String id : before.keySet()) if (!now.containsKey(id)) removed.add(id);
        double delta = ((Number) current.get("pass_rate")).doubleValue() - ((Number) baseline.get("pass_rate")).doubleValue();
        return map("regressions", regressions, "fixed", fixed, "added", added, "removed", removed, "pass_rate_delta", delta,
            "ok", regressions.isEmpty() && removed.isEmpty());
    }
}
