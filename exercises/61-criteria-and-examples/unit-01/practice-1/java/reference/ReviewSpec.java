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
        String lowered = text.toLowerCase();
        for (String phrase : VAGUE) if (lowered.contains(phrase)) return phrase;
        return null;
    }

    static String buildReviewPrompt(Map<String, Object> spec, String diff) {
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
            for (String level : List.of("high", "low")) {
                if (!present(severity.get(level))) throw new IllegalArgumentException("criterion " + c.get("id") + ": severity " + level + " needs a concrete example");
            }
            ids.add((String) c.get("id"));
            out.add("<criterion id=\"" + c.get("id") + "\">\nReport: " + c.get("report") + "\nSkip: " + c.get("skip") + "\nSeverity high: " + severity.get("high") + "\nSeverity low: " + severity.get("low") + "\n</criterion>");
        }
        out.add("</criteria>");
        if (examples.size() < 2 || examples.size() > 4) throw new IllegalArgumentException("use two to four examples");
        Set<Object> verdicts = new HashSet<>();
        for (Map<String, Object> e : examples) verdicts.add(e.get("verdict"));
        if (!verdicts.equals(Set.of("report", "skip"))) throw new IllegalArgumentException("the examples need at least one report and one skip, and no other verdict");
        out.add("<examples>");
        for (Map<String, Object> e : examples) {
            if (!present(e.get("reason"))) throw new IllegalArgumentException("every example needs a reason");
            if ("report".equals(e.get("verdict")) && !ids.contains(e.get("category"))) throw new IllegalArgumentException("a report example names one of the criteria");
            String tag = "report".equals(e.get("verdict")) ? "verdict=\"report\" category=\"" + e.get("category") + "\"" : "verdict=\"skip\"";
            out.add("<example " + tag + ">\n<code>" + e.get("code") + "</code>\n<reason>" + e.get("reason") + "</reason>\n</example>");
        }
        out.add("</examples>");
        out.add("<diff>");
        out.add(diff);
        out.add("</diff>");
        return String.join("\n", out);
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
            top.sort((a, b) -> b.getValue().equals(a.getValue()) ? a.getKey().compareTo(b.getKey()) : b.getValue().compareTo(a.getValue()));
            List<Object> shown = new ArrayList<>();
            for (int i = 0; i < Math.min(3, top.size()); i++) shown.add(List.of(top.get(i).getKey(), top.get(i).getValue()));
            double precision = Math.round((double) accepted / items.size() * 100) / 100.0;
            boolean off = items.size() >= minReviewed && precision < minPrecision;
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
        if (!unresolved.isEmpty() && attended) return map("action", "ask", "ask", unresolved, "assumptions", assumptions);
        if (!unresolved.isEmpty()) return map("action", "stop", "ask", List.of(), "assumptions", assumptions);
        return map("action", "proceed", "ask", List.of(), "assumptions", assumptions);
    }
}
