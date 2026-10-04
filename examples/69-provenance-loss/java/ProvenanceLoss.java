import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What a summary loses, and what a ledger keeps: sources, dates and disagreement.
 *
 * <p>The exam guide (task 5.6) says that source attribution is lost when findings are compressed without their claim-source mappings, that conflicting statistics from credible sources are annotated with their sources and not
 * settled by choosing one, and that dates are required so that a difference over time is not read as a contradiction. Below, seven findings from five invented sources are compressed twice (nothing here calls a model): once into
 * a plain summary that keeps one value per claim, and once into a ledger line per claim that keeps every value with its source and date. The names and figures are invented for the illustration.
 */
public final class ProvenanceLoss {
    record Row(String claim, String value, String source, String date) {}

    static final List<Row> FINDINGS = List.of(
        new Row("market growth 2024", "12%", "Firm A report", "2024-05-01"),
        new Row("market growth 2024", "9%", "Firm B survey", "2024-05-01"),
        new Row("growth forecast", "7%", "Firm C yearbook", "2022-04-01"),
        new Row("growth forecast", "9%", "Firm B survey", "2024-05-01"),
        new Row("inflation 2023", "4%", "Firm A report", "2024-05-01"),
        new Row("inflation 2023", "4%", "Trade paper", "2024-06-10"),
        new Row("headcount", "910", "Press release", "2024-03-01"));

    private static List<String> claimsInOrder(List<Row> findings) {
        Set<String> claims = new LinkedHashSet<>();
        for (Row r : findings) claims.add(r.claim());
        return new ArrayList<>(claims);
    }

    /** agreed: one value; conflict: different values on the same date; changed: different values on different dates. */
    static String status(List<Row> rows) {
        if (rows.stream().map(Row::value).distinct().count() == 1) return "agreed";
        if (rows.stream().anyMatch(a -> rows.stream().anyMatch(b -> !a.value().equals(b.value()) && a.date().equals(b.date())))) return "conflict";
        return "changed";
    }

    /** One line per claim with the first value seen: short, and the sources are gone. */
    static String plainSummary(List<Row> findings) {
        List<String> lines = new ArrayList<>();
        for (String claim : claimsInOrder(findings)) lines.add(claim + ": " + findings.stream().filter(f -> f.claim().equals(claim)).findFirst().orElseThrow().value());
        return String.join("\n", lines);
    }

    /** One line per claim: its status, then every value with its source and date, the oldest date first for a change. */
    static String ledgerLines(List<Row> findings) {
        List<String> lines = new ArrayList<>();
        for (String claim : claimsInOrder(findings)) {
            List<Row> rows = new ArrayList<>(findings.stream().filter(f -> f.claim().equals(claim)).toList());
            String status = status(rows);
            if (status.equals("changed")) rows.sort(Comparator.comparing(Row::date));
            List<String> parts = new ArrayList<>();
            for (Row r : rows) parts.add(r.value() + " (" + r.source() + ", " + r.date() + ")");
            lines.add(claim + " [" + status + "]: " + String.join("; ", parts));
        }
        return String.join("\n", lines);
    }

    static int sourcesNamed(String text, List<Row> findings) {
        return (int) findings.stream().filter(f -> text.contains(f.source())).map(Row::source).distinct().count();
    }

    public static void main(String[] args) {
        int total = (int) FINDINGS.stream().map(Row::source).distinct().count();
        System.out.println("findings: " + FINDINGS.size() + " from " + total + " sources");
        String summary = plainSummary(FINDINGS);
        System.out.println("plain summary:");
        System.out.println(summary);
        System.out.println("sources named by the plain summary: " + sourcesNamed(summary, FINDINGS) + " of " + total);
        String ledger = ledgerLines(FINDINGS);
        System.out.println("ledger:");
        System.out.println(ledger);
        System.out.println("sources named by the ledger: " + sourcesNamed(ledger, FINDINGS) + " of " + total);
    }
}
