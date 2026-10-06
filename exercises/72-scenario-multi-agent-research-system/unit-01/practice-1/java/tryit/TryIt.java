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

        // Two subagents agree on claim X; a third scope timed out and is not covered.
        Synthesis.Result a = new Synthesis.Result("a", "ok", List.of(new Synthesis.Finding("X", "1", "s1", "2025-01-01"), new Synthesis.Finding("Y", "2", "s2", "2025-01-01")), null);
        Synthesis.Result b = new Synthesis.Result("b", "ok", List.of(new Synthesis.Finding("X", "1", "s3", "2025-01-01")), null);
        Synthesis.Result c = new Synthesis.Result("c", "error", List.of(), new Synthesis.Failure("timeout", "q-c", List.of(), List.of("q-c-narrow")));
        Synthesis.Report report = Synthesis.synthesize(List.of("a", "b", "c"), List.of(a, b, c));
        System.out.println("status: " + report.status());
        System.out.println("covered: " + report.covered() + " | gaps: " + report.gaps());
        System.out.println("claims: " + report.claims());
        System.out.println("note: " + report.note());
    }
}
