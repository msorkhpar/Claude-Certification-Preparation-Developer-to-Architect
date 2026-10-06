import java.util.ArrayList;
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

        Response ok = new Response(200, Map.of(), Map.of("type", "message"));
        // A scripted send(), like the one the tests use: overloaded twice, then a good reply.
        List<Response> replies = new ArrayList<>(List.of(failure(529, "overloaded_error"), failure(503, "api_error"), ok));
        int[] calls = {0};
        Send send = () -> {
            calls[0]++;
            return replies.remove(0);
        };

        // The sleep is injected, so nothing really waits: it just records the delays asked for.
        List<Double> waits = new ArrayList<>();
        try {
            Response result = Retry.callWithRetry(send, waits::add, Policy.defaults());
            System.out.println("final status: " + (result == null ? null : result.status()));
        } catch (CallFailed err) {
            System.out.println("gave up: " + err.getMessage());
        }
        System.out.println("calls made: " + calls[0]);
        System.out.println("waits requested: " + waits);
    }

    private static Response failure(int code, String kind) {
        return new Response(code, Map.of(), Map.of("type", "error", "error", Map.of("type", kind, "message", "x")));
    }
}
