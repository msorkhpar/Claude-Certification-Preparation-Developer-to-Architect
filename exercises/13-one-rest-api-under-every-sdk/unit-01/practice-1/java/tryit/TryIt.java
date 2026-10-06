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

        // A stand-in transport, like the one the tests use: it records the request and answers with a fixed reply.
        List<Request> sent = new ArrayList<>();
        Transport transport = request -> {
            sent.add(request);
            return new Response(200, Map.of(), """
                {"content":[{"type":"text","text":"Paris."}],"stop_reason":"end_turn","usage":{"input_tokens":9,"output_tokens":3}}""");
        };

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) Json.parse("[{\"role\":\"user\",\"content\":\"Capital of France?\"}]");
        Map<String, Object> message = RawClient.sendMessages(transport, "sk-test-0123456789abcdef", "claude-sonnet-5-5", messages, 64, "Be brief.");

        System.out.println("requests sent: " + sent.size());
        System.out.println("method and url: " + (sent.isEmpty() ? null : sent.get(0).method() + " " + sent.get(0).url()));
        System.out.println("header names: " + (sent.isEmpty() ? null : new java.util.TreeSet<>(sent.get(0).headers().keySet())));
        System.out.println("reply text: " + (message == null ? null : RawClient.textOf(message)));

        // An error reply becomes an ApiError.
        try {
            RawClient.sendMessages(r -> new Response(429, Map.of("request-id", "req_1"),
                "{\"type\":\"error\",\"error\":{\"type\":\"rate_limit_error\",\"message\":\"Rate limited\"}}"),
                "sk-test-0123456789abcdef", "claude-sonnet-5-5", messages, 64, null);
        } catch (ApiError err) {
            System.out.println("api error: " + err.status() + " " + err.errorType() + " " + err.requestId());
        }
    }
}
