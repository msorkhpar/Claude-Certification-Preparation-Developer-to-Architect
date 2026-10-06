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

        // Sessions of a support agent: a clean one, one that skipped the customer check, and one escalated without need.
        List<Audit.Step> clean = List.of(new Audit.Step("get_customer", true, null), new Audit.Step("lookup_order", true, null), new Audit.Step("process_refund", true, null));
        List<Audit.Session> sessions = List.of(
            new Audit.Session("s", clean, "resolved", false, 2000, 10000),
            new Audit.Session("s", List.of(new Audit.Step("lookup_order", true, null), new Audit.Step("get_customer", true, null), new Audit.Step("process_refund", true, null)), "resolved", false, 0, 10000),
            new Audit.Session("s", List.of(new Audit.Step("get_customer", true, null)), "escalated", false, 0, 10000));
        Audit.Report report = Audit.audit(sessions);
        System.out.println("sessions: " + report.sessions() + " | resolved: " + report.resolved() + " | fcr: " + report.fcr() + " | meets target: " + report.meetsTarget());
        System.out.println("over-escalated: " + report.overEscalated() + " | under-escalated: " + report.underEscalated());
        System.out.println("skipped prerequisite: " + report.skippedPrerequisite() + " | wrong tool: " + report.wrongTool() + " | over-limit refunds: " + report.overLimitRefunds());
        System.out.println("diagnosis: " + report.diagnosis());
    }
}
