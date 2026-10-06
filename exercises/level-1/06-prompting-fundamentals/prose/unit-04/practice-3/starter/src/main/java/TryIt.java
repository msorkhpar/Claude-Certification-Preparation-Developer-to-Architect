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

        // A small spec like the first main test case: a role, one document, one constraint and a task with a placeholder.
        Spec spec = new Spec(
            "Classify the message about {{topic}}.",
            "You are a careful support analyst for {{company}}.",
            null,
            List.of(new Doc("policy.txt", "Refunds within 30 days.")),
            null,
            List.of("Answer in one word."),
            null);
        String prompt = PromptBuilder.build(spec, Map.of("company", "Acme", "topic", "delivery"));

        System.out.println("prompt length: " + prompt.length());
        System.out.println("starts with: " + prompt.substring(0, Math.min(20, prompt.length())).replace("\n", "\\n"));
        System.out.println("has task block: " + prompt.contains("<task>\nClassify the message about delivery.\n</task>"));
        System.out.println(prompt);
    }
}
