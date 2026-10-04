import java.util.List;

/**
 * The coordinator's last step in a research run: merge what the subagents returned into claims with their sources, conflicts, errors and a coverage note.
 * Read statement.md for the shapes of a result and of the report, then replace the body of synthesize().
 */
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

    public static Report synthesize(List<String> required, List<Result> results) {
        return new Report("", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "");
    }
}
