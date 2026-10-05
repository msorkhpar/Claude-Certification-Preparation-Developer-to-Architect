import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
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

        // A scripted tool, like the one the tests use: it times out twice (a transient failure), then works.
        List<Object> script = new ArrayList<>(List.of(new Errors.ToolError("transient", "The billing service timed out after 5 s."),
            new Errors.ToolError("transient", "The billing service timed out after 5 s."), "refund R-1 created"));
        List<Integer> waits = new ArrayList<>();
        Function<Map<String, Object>, Object> tool = call -> {
            Object step = script.remove(0);
            if (step instanceof RuntimeException e) throw e;
            return step;
        };

        // The sleep is injected, so nothing really waits: it records the delays asked for.
        Map<String, Object> result = Errors.runTool(tool, Map.of("order", "A-7", "amount", 40), Map.of("max_retries", 2, "base_delay_ms", 100), waits::add);

        System.out.println("ok: " + (result == null ? null : result.get("ok") + " | content: " + result.get("content") + " | attempts: " + result.get("attempts")));
        System.out.println("waits (ms): " + waits);
        System.out.println("tool_result block: " + (result == null ? null : Errors.toToolResult("toolu_1", result)));
        System.out.println("next action: " + (result == null ? null : Errors.nextAction(result)));
    }
}
