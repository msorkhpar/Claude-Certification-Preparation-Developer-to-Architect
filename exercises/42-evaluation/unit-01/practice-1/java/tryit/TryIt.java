import java.util.List;
import java.util.Map;
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

        // Four test cases, each with its own automated check, like the first main test case.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cases = (List<Map<String, Object>>) Json.parse("""
            [{"id":"c1","input":"I love it","tags":["core"],"check":{"type":"exact","expected":"positive"}},
             {"id":"c2","input":"awful","tags":["core"],"check":{"type":"exact","expected":"negative"}},
             {"id":"c3","input":"order 7","tags":["extract"],"check":{"type":"regex","pattern":"ORD-\\\\d{4}"}},
             {"id":"c4","input":"meh","tags":["core","edge"],"check":{"type":"exact","expected":"neutral"}}]""");
        Map<String, String> answers = Map.of("I love it", "positive", "awful", "negative", "order 7", "The order is ORD-0007.", "meh", "positive");

        // The application under test is a plain function: here it just looks the answer up.
        Map<String, Object> report = Harness.runEval(cases, answers::get, null, 1);

        System.out.println("passed: " + report.get("passed") + " of " + report.get("total"));
        System.out.println("pass rate: " + report.get("pass_rate"));
        if (report.get("results") instanceof List<?> results) {
            for (Object r : results) {
                Map<?, ?> result = (Map<?, ?>) r;
                System.out.println("  " + result.get("id") + " " + (Boolean.TRUE.equals(result.get("passed")) ? "passed" : "failed") + " - " + result.get("reason"));
            }
        }
    }
}
