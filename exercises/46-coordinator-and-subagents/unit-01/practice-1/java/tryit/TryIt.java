import java.util.function.Function;
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

        // The four model roles are plain functions, like the ones the tests script.
        Function<String, Map<String, Object>> planner = question -> Map.of("delegate", true, "answer", "", "subtasks", List.of(
            Map.of("scope", "chips", "brief", "chips: find 2024 chip supply news"),
            Map.of("scope", "cars", "brief", "cars: find 2024 car output news"),
            Map.of("scope", "rates", "brief", "rates: find 2024 interest rates")));
        // A subagent knows only its brief: here it just reports on the topic that starts it.
        Function<String, String> subagent = brief -> brief.split(":")[0] + " report";
        java.util.function.BiFunction<String, List<Map<String, Object>>, List<String>> reviewer = (question, findings) -> List.of(); // no gaps
        java.util.function.BiFunction<String, List<Map<String, Object>>, String> synthesizer = (question, findings) ->
            String.join(" | ", findings.stream().map(f -> String.valueOf(f.get("text"))).toList());

        Map<String, Object> result = Coordinator.coordinate(planner, subagent, reviewer, synthesizer, "How did supply change?");

        System.out.println("status: " + (result == null ? null : result.get("status")) + " | subagent calls: " + (result == null ? null : result.get("subagent_calls"))
            + " | rounds: " + (result == null ? null : result.get("rounds")));
        System.out.println("findings: " + (result == null ? null : result.get("findings")));
        System.out.println("answer: " + (result == null ? null : result.get("answer")));
    }
}
