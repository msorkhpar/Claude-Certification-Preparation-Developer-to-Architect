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

        // A sound workflow design passes; a multi-agent design that writes without approval and has no feedback loop does not.
        Map<String, Object> stages = new LinkedHashMap<>();
        stages.put("input", List.of("parse"));
        stages.put("processing", List.of("classify", "route"));
        stages.put("output", List.of("validate", "send"));
        stages.put("feedback", List.of("review a sample"));
        Map<String, Object> sound = design("intake", "workflow", 1, false, stages);
        Map<String, Object> noFeedback = new LinkedHashMap<>(stages);
        noFeedback.put("feedback", List.of());
        Map<String, Object> risky = design("research", "multi-agent", 4, true, noFeedback);
        for (Map<String, Object> d : List.of(sound, risky)) {
            List<Map<String, Object>> findings = ArchitectureReview.review(d);
            System.out.println(d.get("name") + " -> " + ArchitectureReview.verdict(findings) + " " + findings);
        }
        System.out.println("cheapest design that is not rejected: " + ArchitectureReview.cheapestAdequate(List.of(sound, risky)));
    }

    static Map<String, Object> design(String name, String pattern, int agents, boolean writes, Map<String, Object> stages) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("name", name);
        d.put("pattern", pattern);
        d.put("agents", agents);
        d.put("cost", 3);
        d.put("path_known", true);
        d.put("parallel_independent", false);
        d.put("shared_context", false);
        d.put("needs_audit", true);
        d.put("writes_without_approval", writes);
        d.put("stages", stages);
        return d;
    }
}
