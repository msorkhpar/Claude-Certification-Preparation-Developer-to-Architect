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

        // Three situations from the scenario bank, described by a few features.
        Map<String, Map<String, Object>> situations = new java.util.LinkedHashMap<>();
        situations.put("a rule that must never be broken", Map.of("guarantee", true, "knowledge", "convention"));
        situations.put("a database that needs a connection", Map.of("external_system", true));
        situations.put("work that must run while the laptop is closed", Map.of("timing", "interval", "presence", "away"));
        for (var entry : situations.entrySet()) {
            System.out.println(entry.getKey() + ": " + ExtensionChoice.choose(entry.getValue()));
        }
    }
}
