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

        String plan = "[\"research the topic\", \"draft the outline\", \"check the facts\"]";

        // A stand-in for the model, like the one the tests script: the start of the prompt says which step is asking.
        java.util.function.Function<String, String> model = prompt -> {
            if (prompt.startsWith("Plan")) return plan;
            if (prompt.startsWith("Subtask")) return "done: " + prompt.split("\n")[0].substring("Subtask: ".length());
            return "FINAL";
        };

        // The orchestrator asks for a plan, runs one worker per subtask, then combines the results.
        Map<String, Object> result = Workflows.orchestrate(model, "Write a guide");

        System.out.println("status: " + result.get("status") + " | fallback: " + result.get("fallback") + " | calls: " + result.get("calls"));
        System.out.println("plan: " + result.get("plan"));
        System.out.println("results: " + result.get("results"));
        System.out.println("answer: " + result.get("answer"));
    }
}
