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

        // How often to submit a batch so a 30-hour SLA still leaves room for the 24-hour window and 2 hours of handling.
        System.out.println("interval for a 30 h SLA: " + BatchReview.submissionInterval(30));
        System.out.println("API for a blocking check: " + BatchReview.chooseApi(true, false));
        System.out.println("API for a nightly report: " + BatchReview.chooseApi(false, false));

        // What to do with the results of a batch: (custom id, result kind) pairs and the size of each request.
        List<BatchReview.Result> results = List.of(new BatchReview.Result("a1", "succeeded"), new BatchReview.Result("big", "expired"),
            new BatchReview.Result("bad", "invalid_request"), new BatchReview.Result("late", "errored"));
        System.out.println("resubmission plan: " + BatchReview.resubmissionPlan(results, Map.of("big", 2000, "bad", 10, "late", 10), 1000));
        System.out.println("review passes: " + BatchReview.reviewPlan(List.of("a.py", "b.py", "c.py")));
    }
}
