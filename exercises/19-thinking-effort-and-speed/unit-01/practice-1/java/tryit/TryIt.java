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

        String sonnet = "claude-sonnet-5-5";
        String haiku = "claude-haiku-4-5-20251001";

        // What the application wants, turned into the request parameters one model accepts.
        System.out.println("sonnet adaptive: " + Params.buildParams(sonnet, 4096, Map.of("thinking", Map.of("type", "adaptive"), "effort", "medium")));
        System.out.println("haiku budget: " + Params.buildParams(haiku, 4096, Map.of("thinking", Map.of("type", "enabled", "budget_tokens", 2048))));

        // A combination the API would answer with a 400 is refused, naming the parameter.
        try {
            System.out.println("haiku with effort: " + Params.buildParams(haiku, 4096, Map.of("effort", "high")));
        } catch (RejectedRequest err) {
            System.out.println("refused: " + err.param());
        }
    }
}
