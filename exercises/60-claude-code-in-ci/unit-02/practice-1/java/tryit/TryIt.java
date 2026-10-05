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

        // Jobs a team wants to repeat: each is a small map of what is known about it.
        Map<String, Map<String, Object>> jobs = new LinkedHashMap<>();
        jobs.put("check the build while I watch", Map.of());
        jobs.put("poll a status page every 5 minutes", Map.of("interval_seconds", 300));
        jobs.put("nightly report, laptop may be closed", Map.of("interval_seconds", 86400, "machine_off", true));
        jobs.put("react to a pull request unattended", Map.of("trigger", "event", "repo_event", true));
        jobs.put("run in a pipeline", Map.of("ci", true, "interval_seconds", 600));
        for (Map.Entry<String, Map<String, Object>> job : jobs.entrySet()) System.out.println(job.getKey() + ": " + RhythmPlan.choose(job.getValue()));
    }
}
