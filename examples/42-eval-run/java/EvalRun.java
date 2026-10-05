import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An eval run, a success gate and a regression comparison, on a scripted classifier.
 *
 * <p>The Claude documentation on success criteria and evaluations (read on 2026-10-03) says to design evals that mirror the real task, edge
 * cases included, to automate the grading, and to judge several dimensions at once ("an F1 score of at least 0.85", "99.5% of outputs are
 * non-toxic"). This file runs six sentiment cases through two scripted versions of a prompt, grades them by exact match, and shows that a
 * better average can still hide a regression. The two models are lookup tables standing in for the application: no model is called.
 */
public final class EvalRun {
    private static final System.Logger LOG = System.getLogger(EvalRun.class.getName());
    record Case(String id, String input, String expect, List<String> tags) {}

    record Result(String id, boolean passed, List<String> tags) {}

    /** pass rate of a run, and per tag {passed, total}. */
    record Report(List<Result> results, double passRate, Map<String, int[]> byTag) {}

    record Criteria(double minPassRate, Map<String, Double> tags) {}

    record Diff(List<String> regressions, List<String> fixed) {}

    static final List<Case> CASES = List.of(
        new Case("pos-1", "Love it, works great", "positive", List.of("core")),
        new Case("neg-1", "Broke after two days", "negative", List.of("core")),
        new Case("neu-1", "It arrived on Tuesday", "neutral", List.of("core")),
        new Case("sarcasm-1", "Oh great, another crash", "negative", List.of("edge")),
        new Case("mixed-1", "Fast shipping but the screen is dim", "neutral", List.of("edge")),
        new Case("empty-1", "", "neutral", List.of("edge")));

    static final Map<String, String> PROMPT_V1 = Map.of(
        "Love it, works great", "positive", "Broke after two days", "negative", "It arrived on Tuesday", "neutral",
        "Oh great, another crash", "positive", "Fast shipping but the screen is dim", "positive", "", "Neutral");
    static final Map<String, String> PROMPT_V2 = Map.of(
        "Love it, works great", "positive", "Broke after two days", "negative", "It arrived on Tuesday", "neutral",
        "Oh great, another crash", "negative", "Fast shipping but the screen is dim", "neutral", "", "positive");

    static final Criteria CRITERIA = new Criteria(0.8, Map.of("edge", 0.75));

    /** Code-graded: the answer must equal the expected label, ignoring case and surrounding white space. */
    static boolean grade(Case c, String output) {
        return output.strip().toLowerCase(Locale.ROOT).equals(c.expect());
    }

    static Report run(List<Case> cases, Map<String, String> model) {
        List<Result> results = new ArrayList<>();
        Map<String, int[]> byTag = new LinkedHashMap<>();
        int passed = 0;
        for (Case c : cases) {
            boolean ok = grade(c, model.get(c.input()));
            results.add(new Result(c.id(), ok, c.tags()));
            if (ok) passed++;
            for (String tag : c.tags()) {
                int[] row = byTag.computeIfAbsent(tag, t -> new int[2]);
                if (ok) row[0]++;
                row[1]++;
            }
        }
        return new Report(results, passed / (double) results.size(), byTag);
    }

    /** Every dimension of the success criteria must hold, not only the average. */
    static List<String> gate(Report report, Criteria criteria) {
        List<String> failures = new ArrayList<>();
        if (report.passRate() < criteria.minPassRate()) failures.add("overall");
        for (Map.Entry<String, Double> tag : criteria.tags().entrySet()) {
            int[] row = report.byTag().get(tag.getKey());
            if (row[0] / (double) row[1] < tag.getValue()) failures.add("tag:" + tag.getKey());
        }
        return failures;
    }

    static Diff compare(Report baseline, Report current) {
        Map<String, Boolean> before = new LinkedHashMap<>();
        for (Result r : baseline.results()) before.put(r.id(), r.passed());
        List<String> regressions = new ArrayList<>();
        List<String> fixed = new ArrayList<>();
        for (Result r : current.results()) {
            if (before.get(r.id()) && !r.passed()) regressions.add(r.id());
            if (!before.get(r.id()) && r.passed()) fixed.add(r.id());
        }
        return new Diff(regressions, fixed);
    }

    private static String py(List<String> names) {
        return names.stream().map(n -> "'" + n + "'").collect(Collectors.joining(", ", "[", "]"));
    }

    private static String py(boolean value) {
        return value ? "True" : "False";
    }

    public static void main(String[] args) {
        Report v1 = run(CASES, PROMPT_V1);
        Report v2 = run(CASES, PROMPT_V2);
        for (Map.Entry<String, Report> e : List.of(Map.entry("prompt v1", v1), Map.entry("prompt v2", v2))) {
            Report report = e.getValue();
            int[] edge = report.byTag().get("edge");
            System.out.println(String.format(Locale.ROOT, "%s: pass rate %.3f, edge %d/%d, gate failures %s", e.getKey(), report.passRate(), edge[0], edge[1], py(gate(report, CRITERIA))));
        }
        Diff diff = compare(v1, v2);
        System.out.println("v2 against v1: fixed " + py(diff.fixed()) + " regressions " + py(diff.regressions()));
        System.out.println("average improved: " + py(v2.passRate() > v1.passRate()) + " - safe to ship: " + py(diff.regressions().isEmpty() && gate(v2, CRITERIA).isEmpty()));
    }
}
