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

        Map<String, Object> body = Map.of("model", "ignored-by-the-builder", "max_tokens", 256,
            "messages", List.of(Map.of("role", "user", "content", "Hello, Claude")));
        String opus = "claude-opus-5-5";

        // The same message for three front doors: the URL, the model id and the version header change.
        try {
            Map<String, Map<String, Object>> doors = new java.util.LinkedHashMap<>();
            doors.put("anthropic", Map.of());
            doors.put("bedrock", Map.of("region", "us-east-1"));
            doors.put("vertex", Map.of("project", "my-project"));
            for (var door : doors.entrySet()) {
                Map<String, Object> request = Platforms.buildRequest(door.getKey(), opus, new java.util.HashMap<>(body), door.getValue());
                System.out.println(door.getKey() + " -> " + (request == null ? null : request.get("url")));
                Object reqBody = request == null ? null : request.get("body");
                Object headers = request == null ? null : request.get("headers");
                System.out.println("   model in body: " + (reqBody instanceof Map<?, ?> b ? b.get("model") : null)
                    + " | headers: " + (headers instanceof Map<?, ?> h ? new java.util.TreeSet<>(h.keySet()) : null));
            }
        } catch (PlatformError err) {
            System.out.println("platform error: " + err.getMessage());
        }

        // What a team would lose by moving to Bedrock.
        System.out.println("missing on bedrock: " + Platforms.unsupportedFeatures("bedrock", List.of("batches", "fast_mode", "files_api")));
    }
}
