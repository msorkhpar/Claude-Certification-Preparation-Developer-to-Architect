import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a coordinator is told when one of five sources fails, under four ways of reporting it.
 *
 * <p>The exam guide (task 5.3) calls structured error context (failure type, the query attempted, partial results, alternatives) what lets a coordinator recover intelligently. It names two anti-patterns: a generic status
 * such as "search unavailable", which hides the context, and silent suppression, which reports an empty result as a success; terminating the whole workflow on one failure is the third. The five sources below and
 * their outcomes are invented for the illustration; nothing here calls a model or a search tool.
 */
public final class ErrorContext {
    /** kind is ok, timeout or permission */
    record Outcome(String kind, List<String> items) {}

    static final Map<String, Outcome> OUTCOMES = new LinkedHashMap<>();
    static final Map<String, String> TRY = Map.of("timeout", "retry later", "permission", "request access");

    static {
        OUTCOMES.put("news", new Outcome("ok", List.of("n1", "n2")));
        OUTCOMES.put("papers", new Outcome("timeout", List.of("p1")));
        OUTCOMES.put("patents", new Outcome("ok", List.of()));
        OUTCOMES.put("filings", new Outcome("permission", List.of()));
        OUTCOMES.put("blogs", new Outcome("ok", List.of("b1")));
    }

    private static boolean ok(Outcome o) {
        return o.kind().equals("ok");
    }

    private static List<String> found(Map<String, Outcome> outcomes) {
        List<String> out = new ArrayList<>();
        for (Outcome o : outcomes.values()) if (ok(o)) out.addAll(o.items());
        return out;
    }

    static String generic(Map<String, Outcome> outcomes) {
        List<String> down = new ArrayList<>();
        for (Map.Entry<String, Outcome> e : outcomes.entrySet()) if (!ok(e.getValue())) down.add(e.getKey());
        return "found " + String.join(", ", found(outcomes)) + "; sources unavailable: " + String.join(", ", down);
    }

    static String suppress(Map<String, Outcome> outcomes) {
        List<String> nothing = new ArrayList<>();
        for (Map.Entry<String, Outcome> e : outcomes.entrySet()) if (!ok(e.getValue()) || e.getValue().items().isEmpty()) nothing.add(e.getKey());
        return "found " + String.join(", ", found(outcomes)) + "; nothing found in: " + String.join(", ", nothing);
    }

    static String terminate(Map<String, Outcome> outcomes) {
        List<String> kept = new ArrayList<>();
        for (Map.Entry<String, Outcome> e : outcomes.entrySet()) {
            if (!ok(e.getValue())) return "aborted at " + e.getKey() + "; found " + String.join(", ", kept);
            kept.addAll(e.getValue().items());
        }
        return "found " + String.join(", ", kept);
    }

    static String structured(Map<String, Outcome> outcomes) {
        List<String> good = new ArrayList<>();
        List<String> partial = new ArrayList<>();
        List<String> empty = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        for (Map.Entry<String, Outcome> e : outcomes.entrySet()) {
            Outcome o = e.getValue();
            if (ok(o) && !o.items().isEmpty()) good.add(e.getKey());
            else if (!ok(o) && !o.items().isEmpty()) partial.add(e.getKey() + " (" + o.kind() + ", kept " + String.join(", ", o.items()) + ")");
            else if (ok(o)) empty.add(e.getKey());
            else gaps.add(e.getKey() + " (" + o.kind() + ", try: " + TRY.get(o.kind()) + ")");
        }
        Map<String, List<String>> parts = new LinkedHashMap<>();
        parts.put("well supported", good);
        parts.put("partial", partial);
        parts.put("no findings", empty);
        parts.put("gaps", gaps);
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, List<String>> p : parts.entrySet()) if (!p.getValue().isEmpty()) lines.add(p.getKey() + ": " + String.join(", ", p.getValue()));
        return String.join("; ", lines);
    }

    public static void main(String[] args) {
        System.out.println("generic status: " + generic(OUTCOMES));
        System.out.println("silent empty: " + suppress(OUTCOMES));
        System.out.println("abort on failure: " + terminate(OUTCOMES));
        System.out.println("structured context: " + structured(OUTCOMES));
    }
}
