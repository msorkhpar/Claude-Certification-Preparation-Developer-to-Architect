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

        // A review specification like the tests use: explicit criteria first, then examples of both verdicts.
        Map<String, Object> criterion = new LinkedHashMap<>();
        criterion.put("id", "bug");
        criterion.put("report", "A comment whose claimed behaviour contradicts what the code does.");
        criterion.put("skip", "Minor style, naming and patterns the codebase already uses.");
        Map<String, Object> severity = new LinkedHashMap<>();
        severity.put("high", "A null dereference on a request path.");
        severity.put("low", "A misleading variable name.");
        criterion.put("severity", severity);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("verdict", "report");
        report.put("category", "bug");
        report.put("code", "total = price * qty  # sum of the line items");
        report.put("reason", "The comment says the line sums items, the code multiplies.");
        Map<String, Object> skip = new LinkedHashMap<>();
        skip.put("verdict", "skip");
        skip.put("code", "for i in range(n):  # loop");
        skip.put("reason", "Terse and accurate.");
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("criteria", List.of(criterion));
        spec.put("examples", List.of(report, skip));

        String prompt = ReviewSpec.buildReviewPrompt(spec, "+ x = 1");
        System.out.println("prompt lines: " + (prompt == null ? null : prompt.split("\n").length));
        System.out.println("first line: " + (prompt == null ? null : prompt.split("\n")[0]));
        System.out.println("diff is last: " + (prompt != null && prompt.stripTrailing().endsWith("</diff>")));

        // Which categories to switch off, from what reviewers accepted or dismissed.
        List<Map<String, Object>> findings = new ArrayList<>();
        for (int i = 0; i < 4; i++) findings.add(Map.of("category", "style", "verdict", "dismissed", "detected_pattern", "line-length"));
        findings.add(Map.of("category", "style", "verdict", "accepted", "detected_pattern", "line-length"));
        System.out.println("category report: " + ReviewSpec.categoryReport(findings));
    }
}
