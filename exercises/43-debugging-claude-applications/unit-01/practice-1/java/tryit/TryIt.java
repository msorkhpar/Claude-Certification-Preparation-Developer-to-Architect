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

        // A trace is the list of what happened: the request, then what came back.
        Map<String, Object> request = Map.of("kind", "request", "model", "claude-sonnet-5-5", "max_tokens", 1024,
            "tools", List.of("get_weather"), "last_user_blocks", List.of("text"));

        for (Object[] failure : new Object[][] {{401, "authentication_error"}, {504, "timeout_error"}, {529, "overloaded_error"}}) {
            Map<String, Object> error = Map.of("kind", "error", "status", failure[0], "error_type", failure[1], "message", "m");
            Map<String, Object> d = Diagnose.diagnose(List.of(request, error));
            System.out.println("HTTP " + failure[0] + ": type=" + (d == null ? null : d.get("type")) + " origin=" + (d == null ? null : d.get("origin"))
                + " recovery=" + (d == null ? null : d.get("recovery")));
        }
    }
}
