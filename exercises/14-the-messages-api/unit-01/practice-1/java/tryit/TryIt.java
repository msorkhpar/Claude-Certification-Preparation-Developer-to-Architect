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

        // A stand-in for the API, like the one the tests use: it answers every request the same way.
        Send fakeSend = body -> Map.of(
            "content", List.of(Map.of("type", "text", "text", "Paris.")),
            "stop_reason", "end_turn",
            "usage", Map.of("input_tokens", 10L, "output_tokens", 5L));

        Conversation chat = new Conversation(fakeSend, "claude-sonnet-5-5", 64, "Be brief.", null);
        Reply reply = chat.say("Capital of France?");
        chat.say("Since when?");

        System.out.println("reply text: " + reply.text());
        System.out.println("stop reason: " + reply.stopReason() + " | truncated: " + reply.truncated());
        System.out.println("history size: " + chat.history().size());
        System.out.println("totals: " + chat.totals());
    }
}
