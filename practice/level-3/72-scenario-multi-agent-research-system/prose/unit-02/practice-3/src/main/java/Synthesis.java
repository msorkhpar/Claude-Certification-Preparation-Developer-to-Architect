import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The coordinator's last step in a research run: merge what the subagents returned into claims with their sources, conflicts, errors and a coverage note. */
public final class Synthesis {
    private static final System.Logger LOG = System.getLogger(Synthesis.class.getName());

    public record Finding(String claim, String value, String source, String date) {}

    /** What a failed subagent returns: its type, the query, the partial findings and what to try instead. */
    public record Failure(String type, String query, List<Finding> partial, List<String> alternatives) {}

    /** A subagent's result: status "ok" with findings, or "error" with a failure. */
    public record Result(String scope, String status, List<Finding> findings, Failure error) {}

    public record Source(String source, String date) {}

    public record Claim(String claim, String value, List<Source> sources, boolean partial) {}

    public record Observed(String value, String source, String date) {}

    public record Conflict(String claim, List<Observed> values) {}

    public record Unresolved(String scope, String type, String query, List<String> alternatives) {}

    public record Report(String status, List<String> covered, List<String> gaps, List<String> partial, List<Claim> claims, List<Conflict> conflicts, List<Unresolved> errors, String note) {}

    /** The covered scopes and the gaps, both in the order of the required scopes. */
    record Coverage(List<String> covered, List<String> gaps) {}

    /** One finding and whether it came from a partial list. */
    record Seen(Finding finding, boolean partial) {}

    private static <T> void addNew(List<T> items, T item) {
        if (!items.contains(item)) items.add(item);
    }

    /** Every finding with its partial flag, in arrival order. */
    static List<Seen> findings(List<Result> results) {
        List<Seen> seen = new ArrayList<>();
        for (Result r : results) {
            if (r.status().equals("ok")) r.findings().forEach(f -> seen.add(new Seen(f, false)));
            else r.error().partial().forEach(f -> seen.add(new Seen(f, true)));
        }
        return seen;
    }

    /**
     * TODO 1 of 7 (unlocks e1, e5 and the coverage of every other case): split the required scopes into covered and gaps.
     * Receives the required scopes and the results. Returns a Coverage of both lists, in the order of `required`: a scope is covered
     * when an "ok" result for it has at least one finding.
     * Example: required ["a", "b"], one ok result for "a" with a finding -> Coverage(["a"], ["b"])
     */
    static Coverage coveredScopes(List<String> required, List<Result> results) {
        return new Coverage(List.of(), List.of());
    }

    /**
     * TODO 2 of 7 (unlocks e6): the sources of one claim.
     * Receives the Seen findings of one claim. Returns a list of Source in arrival order, no duplicates.
     * Example: findings from s9, s1, s9 -> [Source("s9", ...), Source("s1", ...)]
     */
    static List<Source> sourcesOf(List<Seen> group) {
        return List.of();
    }

    /**
     * TODO 3 of 7 (unlocks e3): what the sources said about a conflicting claim.
     * Receives the Seen findings of one claim. Returns a list of Observed (value, source, date) in arrival order, no duplicates.
     * Example: s1 says "12", s2 says "14" twice -> [Observed("12", "s1", ...), Observed("14", "s2", ...)]
     */
    static List<Observed> observedValues(List<Seen> group) {
        return List.of();
    }

    /**
     * TODO 4 of 7 (unlocks e4): is every finding of the claim from a partial list?
     * Receives the Seen findings of one claim. Returns true only when all of them are partial.
     * Example: [Seen(f, true), Seen(f, false)] -> false
     */
    static boolean allPartial(List<Seen> group) {
        return false;
    }

    /**
     * TODO 5 of 7 (unlocks e2 and e4): the errors that nothing made up for.
     * Receives the results and the covered scopes. Returns an Unresolved (scope, type, query, alternatives) for each "error" result whose scope is not covered.
     * Example: an error for "b" while only "a" is covered -> [Unresolved("b", "timeout", "qb", [...])]
     */
    static List<Unresolved> unresolvedErrors(List<Result> results, List<String> covered) {
        return List.of();
    }

    /**
     * TODO 6 of 7 (unlocks e4): the gaps for which a failed search still returned findings.
     * Receives the results and the gaps. Returns the gaps that have an "error" result with a non-empty partial list.
     * Example: gap "b" with an error that carries one partial finding -> ["b"]
     */
    static List<String> partialScopes(List<Result> results, List<String> gaps) {
        return List.of();
    }

    /**
     * TODO 7 of 7 (unlocks e1, e2 and e5): say what the report cannot cover.
     * Receives the gaps, the unresolved errors and the results. Returns "all scopes covered" for no gaps; otherwise "not covered: " and
     * each gap as "<scope> (<type> on '<query>')" for an unresolved error, "<scope> (not researched)" when no result names the scope,
     * or "<scope> (no findings)", joined by ", ".
     * Example: gaps ["b", "c"], an error for "b" (timeout on "qb"), no result for "c" -> "not covered: b (timeout on 'qb'), c (not researched)"
     */
    static String coverageNote(List<String> gaps, List<Unresolved> errors, List<Result> results) {
        return "";
    }

    public static Report synthesize(List<String> required, List<Result> results) {
        LOG.log(System.Logger.Level.DEBUG, "synthesize input: {0}", results);
        Coverage coverage = coveredScopes(required, results);
        Map<String, List<Seen>> byClaim = new TreeMap<>();
        for (Seen s : findings(results)) byClaim.computeIfAbsent(s.finding().claim(), k -> new ArrayList<>()).add(s);
        List<Claim> claims = new ArrayList<>();
        List<Conflict> conflicts = new ArrayList<>();
        byClaim.forEach((claim, group) -> {
            List<String> values = new ArrayList<>();
            for (Seen s : group) addNew(values, s.finding().value());
            if (values.size() > 1) conflicts.add(new Conflict(claim, observedValues(group)));
            else claims.add(new Claim(claim, values.get(0), sourcesOf(group), allPartial(group)));
        });
        List<Unresolved> errors = unresolvedErrors(results, coverage.covered());
        return new Report(coverage.gaps().isEmpty() ? "complete" : "partial", coverage.covered(), coverage.gaps(), partialScopes(results, coverage.gaps()), claims, conflicts, errors,
            coverageNote(coverage.gaps(), errors, results));
    }
}
