import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The coordinator's last step in a research run: merge what the subagents returned into claims with their sources, conflicts, errors and a coverage note. */
public final class Synthesis {
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

    private record Seen(Finding finding, boolean partial) {}

    private static <T> void addNew(List<T> items, T item) {
        if (!items.contains(item)) items.add(item);
    }

    public static Report synthesize(List<String> required, List<Result> results) {
        List<String> okScopes = results.stream().filter(r -> r.status().equals("ok") && !r.findings().isEmpty()).map(Result::scope).toList();
        List<String> covered = required.stream().filter(okScopes::contains).toList();
        List<String> gaps = required.stream().filter(s -> !okScopes.contains(s)).toList();
        List<Seen> seen = new ArrayList<>();
        for (Result r : results) {
            if (r.status().equals("ok")) r.findings().forEach(f -> seen.add(new Seen(f, false)));
            else r.error().partial().forEach(f -> seen.add(new Seen(f, true)));
        }
        Map<String, List<Seen>> byClaim = new TreeMap<>();
        for (Seen s : seen) byClaim.computeIfAbsent(s.finding().claim(), k -> new ArrayList<>()).add(s);
        List<Claim> claims = new ArrayList<>();
        List<Conflict> conflicts = new ArrayList<>();
        byClaim.forEach((claim, group) -> {
            List<String> values = new ArrayList<>();
            for (Seen s : group) addNew(values, s.finding().value());
            if (values.size() > 1) {
                List<Observed> observed = new ArrayList<>();
                for (Seen s : group) addNew(observed, new Observed(s.finding().value(), s.finding().source(), s.finding().date()));
                conflicts.add(new Conflict(claim, observed));
            } else {
                List<Source> sources = new ArrayList<>();
                for (Seen s : group) addNew(sources, new Source(s.finding().source(), s.finding().date()));
                claims.add(new Claim(claim, values.get(0), sources, group.stream().allMatch(Seen::partial)));
            }
        });
        List<Unresolved> errors = results.stream().filter(r -> r.status().equals("error") && !covered.contains(r.scope()))
            .map(r -> new Unresolved(r.scope(), r.error().type(), r.error().query(), r.error().alternatives())).toList();
        List<String> partial = gaps.stream().filter(s -> results.stream().anyMatch(r -> r.status().equals("error") && r.scope().equals(s) && !r.error().partial().isEmpty())).toList();
        List<String> parts = new ArrayList<>();
        for (String g : gaps) {
            Unresolved failed = errors.stream().filter(e -> e.scope().equals(g)).findFirst().orElse(null);
            if (failed != null) parts.add(g + " (" + failed.type() + " on '" + failed.query() + "')");
            else if (results.stream().noneMatch(r -> r.scope().equals(g))) parts.add(g + " (not researched)");
            else parts.add(g + " (no findings)");
        }
        return new Report(gaps.isEmpty() ? "complete" : "partial", covered, gaps, partial, claims, conflicts, errors, parts.isEmpty() ? "all scopes covered" : "not covered: " + String.join(", ", parts));
    }
}
