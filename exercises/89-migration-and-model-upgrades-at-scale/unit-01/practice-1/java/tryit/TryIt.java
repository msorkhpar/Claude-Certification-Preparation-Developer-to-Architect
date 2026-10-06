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

        // The retirement calendar: days left per model, nearest first, with the level of urgency.
        List<Rollout.Model> models = List.of(new Rollout.Model("claude-old-a", "2026-10-18", true), new Rollout.Model("claude-old-b", "2026-11-30", false),
            new Rollout.Model("claude-old-c", "2027-01-01", false));
        for (String line : Rollout.retirementStatus(models, "2026-10-04")) System.out.println("calendar: " + line);

        // A request written for an older model, and what the migration changes in it.
        Rollout.Request old = new Rollout.Request("claude-sonnet-4-5-20250929", 0.7, 0.9, 40.0, "disabled", "any", false, true);
        Rollout.Migration migration = Rollout.migrateRequest(old);
        System.out.println("migrated model: " + (migration == null || migration.request() == null ? null : migration.request().model()));
        if (migration != null && migration.changes() != null) for (String change : migration.changes()) System.out.println("change: " + change);

        // A staged rollout: one decision per stage from the traffic and the errors seen so far.
        System.out.println("stage 1: " + Rollout.rolloutStep(1, 2000, 6, 1000, 5));
        System.out.println("stage 25: " + Rollout.rolloutStep(25, 50000, 400, 1000, 5));
    }
}
