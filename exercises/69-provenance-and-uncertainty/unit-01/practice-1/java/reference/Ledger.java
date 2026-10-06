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
        for (String name : REQUIRED) if (field(finding, name).isBlank()) missing.add(name);
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
                if (!sources.get(at).contains(pair)) sources.get(at).add(pair);
            }
            List<Val> values = new ArrayList<>();
            for (int i = 0; i < order.size(); i++) values.add(new Val(order.get(i), sources.get(i)));
            String status;
            if (values.size() == 1) {
                status = "agreed";
            } else if (group.stream().anyMatch(a -> group.stream().anyMatch(b -> !a.value().equals(b.value()) && a.date().equals(b.date())))) {
                status = "conflict";
            } else {
                status = "changed";
                values.sort(Comparator.comparing(Ledger::earliest));
            }
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
            else if (e.values().get(0).sources().stream().map(Src::source).distinct().count() >= 2) well.add(e.claim());
            else single.add(e.claim());
        }
        List<Gap> gaps = new ArrayList<>();
        for (String c : planned) if (!have.contains(c)) gaps.add(new Gap(c, unavailable.getOrDefault(c, "no source found")));
        return new Coverage(well, single, changed, contested, gaps);
    }

    static String render(Entry entry, String kind) {
        if (!KINDS.contains(kind)) throw new IllegalArgumentException("unknown content type " + kind);
        List<String[]> rows = new ArrayList<>();
        for (Val v : entry.values()) for (Src s : v.sources()) rows.add(new String[] {v.value(), s.source(), s.date()});
        List<String> lines = new ArrayList<>();
        if (kind.equals("financial")) {
            lines.add("| Source | Date | Value |");
            lines.add("|---|---|---|");
            for (String[] r : rows) lines.add("| " + r[1] + " | " + r[2] + " | " + r[0] + " |");
            return String.join("\n", lines);
        }
        if (kind.equals("technical")) {
            lines.add(entry.claim() + ":");
            for (String[] r : rows) lines.add("- " + r[0] + " (" + r[1] + ", " + r[2] + ")");
            return String.join("\n", lines);
        }
        List<String> parts = new ArrayList<>();
        for (String[] r : rows) parts.add(r[0] + " (" + r[1] + ", " + r[2] + ")");
        String text = entry.claim() + ": " + String.join("; ", parts) + ".";
        if (entry.status().equals("conflict")) text += " The sources disagree.";
        else if (entry.status().equals("changed")) text += " The figures are from different dates.";
        return text;
    }
}
