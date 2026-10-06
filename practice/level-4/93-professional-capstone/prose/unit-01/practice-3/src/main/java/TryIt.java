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

        // A design that sits at every threshold of the review, as the tests' clean design does.
        Set<String> cleanFlags = new LinkedHashSet<>(List.of("feedback_loop", "model_measured", "replace_on_change", "deferral", "protected_segment", "rollback",
            "human_step", "owner", "accuracy_stated", "managed_settings", "irreversible_action", "team"));
        Map<String, Integer> numbers = Map.of("team_value_chats", 15, "tool_tokens", 10000, "eval_cases", 20, "rollout_stages", 3, "retain_days", 365,
            "floor_days", 90, "ceiling_days", 365, "team_size", 10, "latency_ms", 2000, "availability_tenths", 995);

        List<String> findings = LaunchReview.launchReview(cleanFlags, numbers);
        System.out.println("clean design: " + findings + " -> " + LaunchReview.verdict(findings));

        // The same design with an agent that does not need to be one, and PII reaching the model.
        Set<String> riskyFlags = new LinkedHashSet<>(cleanFlags);
        riskyFlags.addAll(List.of("agent", "path_known", "pii_reaches_model"));
        List<String> risky = LaunchReview.launchReview(riskyFlags, numbers);
        System.out.println("risky design: " + risky + " -> " + LaunchReview.verdict(risky));
        System.out.println("findings per domain: " + LaunchReview.scorecard(risky));
        System.out.println("accuracy needed when an error costs 250 and a review 5: " + LaunchReview.neededAccuracy(250, 5));
    }
}
