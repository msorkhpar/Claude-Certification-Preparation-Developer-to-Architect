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

        // The scratchpad: one line per finding, a fact is recorded once per area.
        List<Recovery.Finding> findings = Recovery.addFinding(List.of(), "auth", "tokens are signed in TokenSigner", "auth/TokenSigner.java:12");
        findings = Recovery.addFinding(findings, "billing", "invoices use cents", "billing/Money.java:5");
        findings = Recovery.addFinding(findings, "auth", "tokens are signed in TokenSigner", "auth/Other.java:99");
        System.out.println(Recovery.renderScratchpad(findings));

        // The manifest of the subagents, and what to do with each after a crash.
        List<Recovery.AgentEntry> agents = List.of(
            new Recovery.AgentEntry("search", "state/search.md", "running"),
            new Recovery.AgentEntry("auth", "state/auth.md", "done"),
            new Recovery.AgentEntry("billing", "state/billing.md", "failed"));
        Recovery.Manifest manifest = Recovery.buildManifest(agents);
        System.out.println("manifest: " + manifest);
        System.out.println("resume plan: " + Recovery.resumePlan(manifest, Set.of("state/auth.md", "state/search.md")));
        System.out.println("compact command: " + Recovery.compactCommand(List.of("the open questions", "file paths")));
    }
}
