import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Claims that keep their sources: required provenance fields, a merge that records agreement, change over time and conflict, a coverage note with gaps, and rendering by content type. See ../../statement.md. */
final class Ledger {
    private static final System.Logger LOG = System.getLogger(Ledger.class.getName());
    private Ledger() {}

    static final List<String> REQUIRED = List.of("claim", "value", "source", "date");
    static final List<String> KINDS = List.of("financial", "news", "technical");

    record Finding(String claim, String value, String source, String date) {}

    record Src(String source, String date) {}

    record Val(String value, List<Src> sources) {}

    record Entry(String claim, String status, List<Val> values) {}

    record Gap(String claim, String reason) {}

    record Coverage(List<String> wellSupported, List<String> singleSource, List<String> changed, List<String> contested, List<Gap> gaps) {}

    private static String field(Finding f, String name) {
        String v = switch (name) {
            case "claim" -> f.claim();
            case "value" -> f.value();
            case "source" -> f.source();
            default -> f.date();
        };
        return v == null ? "" : v;
    }

    static List<String> checkFinding(Finding finding) {
        List<String> missing = new ArrayList<>();
        // TODO 1 of 8 (finish this to pass e1): the provenance check. Receives one finding. Return the names, in
        //   REQUIRED order, of the fields that are absent, empty or only white space. Example: source "" and date " " ->
        //   [source, date].
        for (String name : REQUIRED) if (field(finding, name).isEmpty()) missing.add(name);
        return missing;
    }

    private static String earliest(Val v) {
        return v.sources().stream().map(Src::date).min(Comparator.naturalOrder()).orElseThrow();
    }

    static List<Entry> merge(List<Finding> findings) {
        LOG.log(System.Logger.Level.DEBUG, "merge input: {0}", findings);
        for (int i = 0; i < findings.size(); i++) {
            List<String> missing = checkFinding(findings.get(i));
            if (!missing.isEmpty()) throw new IllegalArgumentException("finding " + i + " is missing " + String.join(", ", missing));
        }
        Set<String> claims = new LinkedHashSet<>();
        for (Finding f : findings) claims.add(f.claim());
        List<Entry> out = new ArrayList<>();
        for (String claim : claims) {
            List<Finding> group = findings.stream().filter(f -> f.claim().equals(claim)).toList();
            List<String> order = new ArrayList<>();
            List<List<Src>> sources = new ArrayList<>();
            for (Finding f : group) {
                int at = order.indexOf(f.value());
                if (at < 0) {
                    order.add(f.value());
                    sources.add(new ArrayList<>());
                    at = order.size() - 1;
                }
                Src pair = new Src(f.source(), f.date());
                // TODO 2 of 8 (finish this to pass m1): the sources of a value. When a finding repeats a source and date
                //   pair that the value already holds, do not add it again; otherwise add the pair last. Example: Annual
                //   report 2024-02-01 reported twice -> kept once.
                sources.get(at).add(pair);
            }
            List<Val> values = new ArrayList<>();
            for (int i = 0; i < order.size(); i++) values.add(new Val(order.get(i), sources.get(i)));
            String status;
            // TODO 3 of 8 (finish this to pass e2, e3): the status of a claim. Receives the group of findings and its
            //   distinct values. One value -> agreed. Several values where two different values share a date -> conflict
            //   (keep all). Several values with no shared date -> changed, with the values ordered by their earliest date.
            //   Example: 12% and 9% both on 2024-05-01 -> conflict.
            status = "agreed";
            out.add(new Entry(claim, status, values));
        }
        return out;
    }

    static Coverage coverageNote(List<String> planned, List<Entry> merged, Map<String, String> unavailable) {
        List<String> well = new ArrayList<>();
        List<String> single = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        List<String> contested = new ArrayList<>();
        Set<String> have = new HashSet<>();
        for (Entry e : merged) {
            have.add(e.claim());
            if (e.status().equals("conflict")) contested.add(e.claim());
            else if (e.status().equals("changed")) changed.add(e.claim());
            // TODO 4 of 8 (finish this to pass e4): the split of the agreed claims. For an agreed claim, add it to
            //   well_supported when its value comes from at least two different sources, otherwise to single_source.
            //   Example: two sources -> well_supported; one -> single_source.
            else single.add(e.claim());
        }
        List<Gap> gaps = new ArrayList<>();
        // TODO 5 of 8 (finish this to pass e5): the gaps. Receives the planned claims, the merged entries and the
        //   reasons for unavailable claims. Return one {claim, reason} for each planned claim that has no merged entry,
        //   with the reason from `unavailable` or "no source found". Example: planned [a, b], merged a -> gap b.
        return new Coverage(well, single, changed, contested, gaps);
    }

    static String render(Entry entry, String kind) {
        if (!KINDS.contains(kind)) throw new IllegalArgumentException("unknown content type " + kind);
        List<String[]> rows = new ArrayList<>();
        for (Val v : entry.values()) for (Src s : v.sources()) rows.add(new String[] {v.value(), s.source(), s.date()});
        List<String> lines = new ArrayList<>();
        // TODO 6 of 8 (finish this to pass e6): the financial rendering. Return a Markdown table: the header "| Source |
        //   Date | Value |", the line "|---|---|---|" and one row "| source | date | value |" for each source of each
        //   value. Example: one source -> three lines.
        // TODO 7 of 8 (finish this to pass e8): the technical rendering. Return the claim followed by a colon, then one
        //   line "- value (source, date)" for each source of each value. Example: one source -> two lines.
        List<String> parts = new ArrayList<>();
        for (String[] r : rows) parts.add(r[0] + " (" + r[1] + ", " + r[2] + ")");
        String text = entry.claim() + ": " + String.join("; ", parts) + ".";
        // TODO 8 of 8 (finish this to pass e7): the news ending. After the prose, add " The sources disagree." for a
        //   conflict and " The figures are from different dates." for a changed claim. Example: conflict -> the prose ends
        //   with that sentence.
        return text;
    }
}
