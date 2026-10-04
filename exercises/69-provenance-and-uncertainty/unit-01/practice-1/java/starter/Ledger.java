import java.util.List;
import java.util.Map;

/** Claims that keep their sources: required provenance fields, a merge that records agreement, change over time and conflict, a coverage note with gaps, and rendering by content type. See ../../statement.md. */
final class Ledger {
    private Ledger() {}

    static final List<String> REQUIRED = List.of("claim", "value", "source", "date");
    static final List<String> KINDS = List.of("financial", "news", "technical");

    record Finding(String claim, String value, String source, String date) {}

    record Src(String source, String date) {}

    record Val(String value, List<Src> sources) {}

    record Entry(String claim, String status, List<Val> values) {}

    record Gap(String claim, String reason) {}

    record Coverage(List<String> wellSupported, List<String> singleSource, List<String> changed, List<String> contested, List<Gap> gaps) {}

    static List<String> checkFinding(Finding finding) {
        // TODO: the names of the required fields that are missing or blank, in the order of REQUIRED.
        return null;
    }

    static List<Entry> merge(List<Finding> findings) {
        // TODO: one entry per claim, with its values, their sources and a status; refuse an incomplete finding with an IllegalArgumentException.
        return null;
    }

    static Coverage coverageNote(List<String> planned, List<Entry> merged, Map<String, String> unavailable) {
        // TODO: which claims are well supported, single-source, changed, contested, and which planned claims are gaps.
        return null;
    }

    static String render(Entry entry, String kind) {
        // TODO: a table for financial data, prose for news, a bulleted list for technical findings.
        return null;
    }
}
