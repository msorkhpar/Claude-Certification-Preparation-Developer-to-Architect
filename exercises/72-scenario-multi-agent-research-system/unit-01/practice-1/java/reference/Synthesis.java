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

    static Coverage coveredScopes(List<String> required, List<Result> results) {
        List<String> okScopes = results.stream().filter(r -> r.status().equals("ok") && !r.findings().isEmpty()).map(Result::scope).toList();
        return new Coverage(required.stream().filter(okScopes::contains).toList(), required.stream().filter(s -> !okScopes.contains(s)).toList());
    }

    static List<Source> sourcesOf(List<Seen> group) {
        List<Source> sources = new ArrayList<>();
        for (Seen s : group) addNew(sources, new Source(s.finding().source(), s.finding().date()));
        return sources;
    }

    static List<Observed> observedValues(List<Seen> group) {
        List<Observed> observed = new ArrayList<>();
        for (Seen s : group) addNew(observed, new Observed(s.finding().value(), s.finding().source(), s.finding().date()));
        return observed;
    }

    static boolean allPartial(List<Seen> group) {
        return group.stream().allMatch(Seen::partial);
    }

    static List<Unresolved> unresolvedErrors(List<Result> results, List<String> covered) {
        return results.stream().filter(r -> r.status().equals("error") && !covered.contains(r.scope()))
            .map(r -> new Unresolved(r.scope(), r.error().type(), r.error().query(), r.error().alternatives())).toList();
    }

    static List<String> partialScopes(List<Result> results, List<String> gaps) {
        return gaps.stream().filter(s -> results.stream().anyMatch(r -> r.status().equals("error") && r.scope().equals(s) && !r.error().partial().isEmpty())).toList();
    }

    static String coverageNote(List<String> gaps, List<Unresolved> errors, List<Result> results) {
        List<String> parts = new ArrayList<>();
        for (String g : gaps) {
            Unresolved failed = errors.stream().filter(e -> e.scope().equals(g)).findFirst().orElse(null);
            if (failed != null) parts.add(g + " (" + failed.type() + " on '" + failed.query() + "')");
            else if (results.stream().noneMatch(r -> r.scope().equals(g))) parts.add(g + " (not researched)");
            else parts.add(g + " (no findings)");
        }
        return parts.isEmpty() ? "all scopes covered" : "not covered: " + String.join(", ", parts);
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
