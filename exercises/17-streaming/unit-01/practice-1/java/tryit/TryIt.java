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

        // The parsed `data:` events of one streamed reply, like the ones the tests build: start, one text block in three deltas, finish.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) Json.parse("""
            [
              {"type":"message_start","message":{"id":"msg_x","type":"message","role":"assistant","model":"claude-sonnet-5-5","content":[],"stop_reason":null,"stop_sequence":null,"usage":{"input_tokens":25,"output_tokens":1}}},
              {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}},
              {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hel"}},
              {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"lo, "}},
              {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"world."}},
              {"type":"content_block_stop","index":0},
              {"type":"message_delta","delta":{"stop_reason":"end_turn","stop_sequence":null},"usage":{"output_tokens":15}},
              {"type":"message_stop"}
            ]
            """);

        try {
            Map<String, Object> message = Assemble.assemble(events);
            System.out.println("content: " + message.get("content"));
            System.out.println("stop reason: " + message.get("stop_reason"));
            System.out.println("usage: " + message.get("usage"));
        } catch (StreamError err) {
            System.out.println("stream error: " + err.getMessage());
        }
    }
}
