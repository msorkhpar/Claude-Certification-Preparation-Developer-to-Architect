import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A review specification that cuts false positives: the prompt, the trust in each category and the next step when a request is incomplete. See ../../statement.md. Results are JSON-like maps. */
final class ReviewSpec {
    private static final System.Logger LOG = System.getLogger(ReviewSpec.class.getName());
    private ReviewSpec() {}

    static final List<String> VAGUE = List.of("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment");

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        return value == null ? List.of() : (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value == null ? Map.of() : (Map<String, Object>) value;
    }

    private static boolean present(Object value) {
        return value instanceof String s && !s.isBlank();
    }

    private static boolean blank(Object value) {
        return value == null || (value instanceof String s && s.isBlank());
    }

    private static String vague(String text) {
        // TODO 2 of 7 (finish this to pass e1): the vague check. Receives a criterion's report or skip text. Return the
        //   first phrase of VAGUE that the lower-cased text contains, or nothing when it contains none. Example: "Be
        //   conservative here" -> "be conservative".
        return null;
    }

    static String buildReviewPrompt(Map<String, Object> spec, String diff) {
        LOG.log(System.Logger.Level.DEBUG, "buildReviewPrompt input: {0}", spec);
        List<Map<String, Object>> criteria = maps(spec.get("criteria"));
        List<Map<String, Object>> examples = maps(spec.get("examples"));
        if (criteria.isEmpty()) throw new IllegalArgumentException("at least one criterion is required");
        List<String> ids = new ArrayList<>();
        List<String> out = new ArrayList<>();
        out.add("<criteria>");
        for (Map<String, Object> c : criteria) {
            for (String key : List.of("report", "skip")) {
                if (!present(c.get(key))) throw new IllegalArgumentException("criterion " + c.get("id") + ": " + key + " is required");
                String phrase = vague((String) c.get(key));
                if (phrase != null) throw new IllegalArgumentException("criterion " + c.get("id") + ": " + key + " is vague ('" + phrase + "'): name the pattern instead");
            }
            Map<String, Object> severity = asMap(c.get("severity"));
            // TODO 3 of 7 (finish this to pass e2): the severity check. For each of the levels high and low, refuse the
            //   criterion when its severity has no concrete example text for that level. Example: severity {high: "..."}
            //   with no low -> refused.
            ids.add((String) c.get("id"));
            out.add("<criterion id=\"" + c.get("id") + "\">\nReport: " + c.get("report") + "\nSkip: " + c.get("skip") + "\nSeverity high: " + severity.get("high") + "\nSeverity low: " + severity.get("low") + "\n</criterion>");
        }
        out.add("</criteria>");
        // TODO 4 of 7 (finish this to pass e3): the examples check. Refuse the specification unless it has two to four
        //   examples and their verdicts are exactly report and skip (at least one of each, nothing else). Example: three
        //   report examples and no skip -> refused.
        out.add("<examples>");
        for (Map<String, Object> e : examples) {
            if (!present(e.get("reason"))) throw new IllegalArgumentException("every example needs a reason");
            if ("report".equals(e.get("verdict")) && !ids.contains(e.get("category"))) throw new IllegalArgumentException("a report example names one of the criteria");
            String tag = "report".equals(e.get("verdict")) ? "verdict=\"report\" category=\"" + e.get("category") + "\"" : "verdict=\"skip\"";
            out.add("<example " + tag + ">\n<code>" + e.get("code") + "</code>\n<reason>" + e.get("reason") + "</reason>\n</example>");
        }
        out.add("</examples>");
        // TODO 1 of 7 (finish this to pass m1): the end of the prompt. After the criteria and examples blocks, add the
        //   diff in a <diff> block and return the whole prompt joined with newlines, so that the diff comes last. Example:
        //   criteria, examples, then <diff>, the diff, </diff>.
        return diff;
    }

    static Map<String, Object> categoryReport(List<Map<String, Object>> findings) {
        return categoryReport(findings, 5, 0.5);
    }

    static Map<String, Object> categoryReport(List<Map<String, Object>> findings, int minReviewed, double minPrecision) {
        Map<String, List<Map<String, Object>>> byCategory = new LinkedHashMap<>();
        for (Map<String, Object> f : findings) byCategory.computeIfAbsent((String) f.get("category"), k -> new ArrayList<>()).add(f);
        Map<String, Object> categories = new LinkedHashMap<>();
        List<String> disable = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : byCategory.entrySet()) {
            List<Map<String, Object>> items = entry.getValue();
            int accepted = 0;
            Map<String, Integer> counts = new HashMap<>();
            for (Map<String, Object> f : items) {
                if ("accepted".equals(f.get("verdict"))) accepted++;
                else if ("dismissed".equals(f.get("verdict"))) counts.merge((String) f.get("detected_pattern"), 1, Integer::sum);
            }
            List<Map.Entry<String, Integer>> top = new ArrayList<>(counts.entrySet());
            // TODO 6 of 7 (finish this to pass e5): the top dismissed patterns. Receives the count of dismissals per
            //   detected pattern. Return up to three [pattern, count] pairs, ordered by count (highest first) and then by
            //   pattern name. Example: {a: 2, b: 3, c: 2, d: 1} -> [b 3], [a 2], [c 2].
            List<Object> shown = new ArrayList<>();
            for (int i = 0; i < Math.min(3, top.size()); i++) shown.add(List.of(top.get(i).getKey(), top.get(i).getValue()));
            double precision = Math.round((double) accepted / items.size() * 100) / 100.0;
            // TODO 5 of 7 (finish this to pass e4): the disable flag of a category. Receives the number reviewed, the
            //   precision, min_reviewed and min_precision. A category is disabled when it has at least min_reviewed
            //   reviews and a precision below min_precision. Example: 5 reviews, precision 0.4, defaults -> disabled; 4
            //   reviews -> not.
            boolean off = false;
            categories.put(entry.getKey(), map("reviewed", items.size(), "precision", precision, "disable", off, "top_dismissed", shown));
            if (off) disable.add(entry.getKey());
        }
        Collections.sort(disable);
        return map("categories", categories, "disable", disable);
    }

    static Map<String, Object> nextStep(Map<String, Object> request, List<String> required, Map<String, String> defaults, boolean attended) {
        List<String> missing = new ArrayList<>();
        for (String f : required) if (blank(request.get(f))) missing.add(f);
        Map<String, Object> assumptions = new LinkedHashMap<>();
        List<String> unresolved = new ArrayList<>();
        for (String f : missing) {
            if (defaults.containsKey(f)) assumptions.put(f, defaults.get(f));
            else unresolved.add(f);
        }
        // TODO 7 of 7 (finish this to pass e6, e7): the next step. `unresolved` holds the missing fields that have no
        //   default. When some are unresolved and the run is attended, return action ask with those fields in `ask`; when
        //   unattended, return action stop with an empty `ask`. Both carry the assumptions. Example: attended, unresolved
        //   [repo] -> ask [repo].
        return map("action", "proceed", "ask", List.of(), "assumptions", assumptions);
    }
}
