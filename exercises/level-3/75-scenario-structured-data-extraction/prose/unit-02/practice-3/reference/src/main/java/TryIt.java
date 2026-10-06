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

        RunAudit.Policy policy = new RunAudit.Policy(90, 3, 5);
        // A run of seven documents: three typed ones right, two scans, two handwritten ones that went wrong.
        List<RunAudit.Run> runs = new ArrayList<>();
        for (int i = 0; i < 3; i++) runs.add(new RunAudit.Run("d", "typed", "valid", true, false, false, true));
        runs.add(new RunAudit.Run("d", "scanned", "valid", true, false, false, true));
        runs.add(new RunAudit.Run("d", "scanned", "needs_review", false, false, false, true));
        runs.add(new RunAudit.Run("d", "handwritten", "failed", false, true, false, true));
        runs.add(new RunAudit.Run("d", "handwritten", "needs_review", false, false, true, true));
        RunAudit.Report report = RunAudit.audit(runs, policy);
        System.out.println(report);
    }
}
