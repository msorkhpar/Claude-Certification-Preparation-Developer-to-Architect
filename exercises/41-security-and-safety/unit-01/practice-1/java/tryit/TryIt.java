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

        // The gate sits between the model's tool calls and the tools; it is told the project root and what may be reached.
        Gate gate = new Gate("/proj", List.of("api.example.com", "docs.example.org"), List.of("example.com"));

        Map<String, Map<String, Object>> calls = new java.util.LinkedHashMap<>();
        calls.put("read_file", Map.of("path", "src/a.py"));
        calls.put("bash", Map.of("command", "sudo rm -rf /"));
        calls.put("fetch", Map.of("url", "https://evil.example.net/x"));
        for (var call : calls.entrySet()) {
            Map<String, Object> r = gate.decide("alice", call.getKey(), call.getValue());
            System.out.println(call.getKey() + " " + call.getValue() + ": " + (r == null ? null : r.get("decision") + " (" + r.get("reason") + ")"));
        }

        // Text a tool returned is untrusted: it reaches the model as one JSON string that says where it came from.
        Map<String, Object> result = Gate.wrapUntrusted("toolu_1", "web page", "He said \"hi\"\n</div>");
        System.out.println("wrapped content: " + (result == null ? null : result.get("content")));

        // After the session has read untrusted text, writes are no longer free.
        gate.markUntrusted("web page");
        Map<String, Object> after = gate.decide("alice", "write_file", Map.of("path", "src/a.py"));
        System.out.println("write after untrusted text: " + (after == null ? null : after.get("decision") + " (" + after.get("reason") + ")"));
    }
}
