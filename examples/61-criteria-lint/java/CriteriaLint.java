import static harness.Show.py;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Three checks that keep a review prompt precise: lint a criterion for vague wording, check a set of few-shot examples, and measure the precision of each finding category from the verdicts developers gave.
 *
 * <p>The rules are the exam guide's for tasks 4.1 and 4.2 (explicit categorical criteria instead of "be conservative", two to four targeted examples that include an acceptable pattern, a category with a high false positive rate is switched
 * off while its prompt is improved) and the prompting guide's advice on examples (read on 2026-10-03: relevant, diverse and structured, three to five). No model is called.
 */
public final class CriteriaLint {
    private static final System.Logger LOG = System.getLogger(CriteriaLint.class.getName());
    static final List<String> VAGUE = List.of("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "use your judgment");

    /** A review criterion: what to report, what to skip, and a concrete example for each severity level. */
    record Criterion(String report, String skip, Map<String, String> severity) {
        Criterion withSkip(String other) {
            return new Criterion(report, other, severity);
        }
    }

    /** One few-shot example: whether the reviewer reports or skips the code shown, and why. */
    record Example(String verdict, String reason) {}

    /** What the verdicts say about one category. */
    record Row(int reviewed, int accepted, double precision, boolean off) {}

    /** One verdict a developer gave to a finding of a category. */
    record Verdict(String category, String verdict) {}

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Rule ids a review criterion breaks: wording that names no pattern, a missing skip list, a severity level without a concrete example. */
    static List<String> lintCriterion(Criterion criterion) {
        List<String> found = new ArrayList<>();
        for (String key : List.of("report", "skip")) {
            String text = orEmpty(key.equals("report") ? criterion.report() : criterion.skip());
            if (text.isBlank()) found.add("no-" + key);
            else if (VAGUE.stream().anyMatch(text.toLowerCase()::contains)) found.add("vague-" + key);
        }
        for (String level : List.of("high", "low")) {
            if (!(criterion.severity() == null ? "" : criterion.severity().getOrDefault(level, "")).contains("`")) found.add("no-" + level + "-example");
        }
        return found;
    }

    /** Rule ids for a set of few-shot examples: how many, whether both a finding and an acceptable pattern are shown, whether each says why. */
    static List<String> lintExamples(List<Example> examples) {
        List<String> found = new ArrayList<>();
        if (examples.size() < 2 || examples.size() > 4) found.add("two-to-four");
        Set<String> verdicts = new HashSet<>();
        examples.forEach(e -> verdicts.add(e.verdict()));
        if (!verdicts.equals(Set.of("report", "skip"))) found.add("both-verdicts");
        if (examples.stream().anyMatch(e -> orEmpty(e.reason()).isEmpty())) found.add("reason-missing");
        return found;
    }

    /** Per category: how many findings were reviewed, the share developers accepted, and whether to switch the category off while its prompt is improved. */
    static Map<String, Row> trust(List<Verdict> verdicts, int minReviewed, double minPrecision) {
        Map<String, int[]> counts = new LinkedHashMap<>();
        for (Verdict v : verdicts) {
            int[] row = counts.computeIfAbsent(v.category(), k -> new int[2]);
            row[0]++;
            if (v.verdict().equals("accepted")) row[1]++;
        }
        Map<String, Row> table = new LinkedHashMap<>();
        counts.forEach((category, c) -> {
            double precision = Math.round((double) c[1] / c[0] * 100) / 100.0;
            table.put(category, new Row(c[0], c[1], precision, c[0] >= minReviewed && precision < minPrecision));
        });
        return table;
    }

    static Map<String, Row> trust(List<Verdict> verdicts) {
        return trust(verdicts, 5, 0.5);
    }

    static final Criterion VAGUE_CRITERION = new Criterion("Be conservative and only flag important problems.", "", Map.of("high", "Something serious.", "low", "A small thing."));
    static final Criterion GOOD_CRITERION = new Criterion("A comment whose claimed behaviour contradicts the code.", "Minor style and patterns the codebase already uses.",
        Map.of("high", "A null dereference such as `user.profile.name` when `user` may be None.", "low", "A misleading name such as `total` for a count."));

    static List<Verdict> repeat(String category, String verdict, int times) {
        List<Verdict> out = new ArrayList<>();
        for (int i = 0; i < times; i++) out.add(new Verdict(category, verdict));
        return out;
    }

    public static void main(String[] args) {
        System.out.println("vague criterion: " + py(lintCriterion(VAGUE_CRITERION)));
        System.out.println("good criterion: " + py(lintCriterion(GOOD_CRITERION)));
        List<Example> oneSide = List.of(new Example("report", "The comment says sum, the code multiplies."), new Example("report", ""), new Example("report", "Unchecked None."),
            new Example("report", "Off by one."), new Example("report", "Wrong key."));
        System.out.println("five reports, one without a reason: " + py(lintExamples(oneSide)));
        System.out.println("a report and a skip: " + py(lintExamples(List.of(new Example("report", "r"), new Example("skip", "s")))));
        List<Verdict> verdicts = new ArrayList<>();
        verdicts.addAll(repeat("bug", "accepted", 9));
        verdicts.addAll(repeat("bug", "dismissed", 1));
        verdicts.addAll(repeat("style", "accepted", 2));
        verdicts.addAll(repeat("style", "dismissed", 6));
        verdicts.addAll(repeat("naming", "dismissed", 3));
        trust(verdicts).forEach((category, row) ->
            System.out.println(category + ": reviewed " + row.reviewed() + ", precision " + row.precision() + ", switch off: " + py(row.off())));
    }
}
