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

        // Two images and a question: the planner puts the images first, each labelled, and the question last.
        List<Map<String, Object>> items = List.of(
            img("chart", 1000, 1000, "image/png", "base64", "AAAA"),
            img("photo", 200, 200, "image/jpeg", "url", "https://example.invalid/p.jpg"));
        try {
            Map<String, Object> plan = Vision.planRequest("claude-opus-5-5", items, "What changed?");
            if (plan != null && plan.get("content") instanceof List<?> content) {
                for (Object block : content) {
                    Map<?, ?> b = (Map<?, ?>) block;
                    System.out.println("block: " + b.get("type") + " " + (b.get("text") == null ? "" : b.get("text")));
                }
            }
            System.out.println("image tokens: " + (plan == null ? null : plan.get("image_tokens")));
            System.out.println("resized: " + (plan == null ? null : plan.get("resized")));
        } catch (RequestError err) {
            System.out.println("refused: " + err.getMessage());
        }
    }

    private static Map<String, Object> img(String name, int w, int h, String mediaType, String source, String value) {
        return Map.of("kind", "image", "name", name, "media_type", mediaType, "width", w, "height", h, "size", 1000, "source", source, "value", value);
    }
}
