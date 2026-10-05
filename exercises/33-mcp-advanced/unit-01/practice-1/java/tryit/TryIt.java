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

        String secret = "s3cret";
        Map<String, Object> meta = Map.of("io.modelcontextprotocol/protocolVersion", "2026-07-28",
            "io.modelcontextprotocol/clientCapabilities", Map.of("elicitation", Map.of(), "sampling", Map.of()));
        Map<String, Object> base = Map.of("name", "deploy", "arguments", Map.of("service", "api", "env", "production"), "_meta", meta);

        // Round trip 1: the server needs a person's confirmation, so it ends the call with input_required and a signed state.
        Map<String, Object> first = Mrtr.callTool(base, secret, "alice", 1000);
        System.out.println("first call: " + first.get("resultType") + " | asks for: " + (first.get("inputRequests") instanceof Map<?, ?> m ? m.keySet() : "[]"));
        System.out.println("state is a string: " + (first.get("requestState") instanceof String));

        // Round trip 2: the client retries the same call with the answer and echoes the state back.
        Map<String, Object> retry = new java.util.HashMap<>(base);
        retry.put("inputResponses", Map.of("confirm", Map.of("action", "accept", "content", Map.of("confirm", true))));
        retry.put("requestState", first.get("requestState"));
        Map<String, Object> second = Mrtr.callTool(retry, secret, "alice", 1010);
        System.out.println("second call: " + second.get("resultType") + " | asks for: " + (second.get("inputRequests") instanceof Map<?, ?> m2 ? m2.keySet() : "[]"));
    }
}
