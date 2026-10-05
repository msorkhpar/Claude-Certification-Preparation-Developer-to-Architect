import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main to try your code; Submit runs the tests. */
public class TryIt {
    public static void main(String[] args) {
        // Turn the logger up, so the LOG.log(DEBUG, ...) lines of your code show under the printed lines.
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s %5$s%n");
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);

        // Findings from two subagents: the same claim from two sources, and one that disagrees.
        List<Ledger.Finding> findings = List.of(
            new Ledger.Finding("revenue 2023", "4.1B", "Annual report", "2024-02-01"),
            new Ledger.Finding("revenue 2023", "4.1B", "Press release", "2024-02-03"),
            new Ledger.Finding("headcount", "910", "Press release", "2024-03-01"),
            new Ledger.Finding("headcount", "950", "Blog", "2024-03-01"));
        System.out.println("missing fields: " + Ledger.checkFinding(new Ledger.Finding("headcount", "910", null, null)));

        List<Ledger.Entry> merged = Ledger.merge(findings);
        for (Ledger.Entry entry : merged) System.out.println("entry: " + entry.claim() + " " + entry.status() + " " + entry.values());

        // What the report says about coverage, and how an entry is shown.
        System.out.println("coverage: " + Ledger.coverageNote(List.of("revenue 2023", "headcount", "patents"), merged, Map.of("patents", "the registry timed out")));
        System.out.println("rendered: " + (merged.isEmpty() ? null : Ledger.render(merged.get(0), "news")));
    }
}
